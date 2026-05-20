package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BuildingEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.GameStateEntity
import com.example.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository

    // Raw internal flows
    private val _gameState = MutableStateFlow<GameStateEntity?>(null)
    val gameState: StateFlow<GameStateEntity?> = _gameState.asStateFlow()

    private val _buildings = MutableStateFlow<List<BuildingEntity>>(emptyList())
    val buildings: StateFlow<List<BuildingEntity>> = _buildings.asStateFlow()

    private val _equipment = MutableStateFlow<List<EquipmentEntity>>(emptyList())
    val equipment: StateFlow<List<EquipmentEntity>> = _equipment.asStateFlow()

    // Dialog & UI Events State
    private val _idleEarningsDialog = MutableStateFlow<IdleEarnings?>(null)
    val idleEarningsDialog: StateFlow<IdleEarnings?> = _idleEarningsDialog.asStateFlow()

    private val _promoToast = MutableStateFlow<String?>(null)
    val promoToast: StateFlow<String?> = _promoToast.asStateFlow()

    private val _insufficientResources = MutableStateFlow<String?>(null)
    val insufficientResources: StateFlow<String?> = _insufficientResources.asStateFlow()

    // Real-time production rates (calculated values)
    val coinsPerSec = MutableStateFlow(0.0)
    val diamondsPerSec = MutableStateFlow(0.0)
    val woodPerSec = MutableStateFlow(0.0)
    val stonePerSec = MutableStateFlow(0.0)
    val foodPerSec = MutableStateFlow(0.0)

    private var tickJob: Job? = null
    private var saveTickCounter = 0

    data class IdleEarnings(
        val seconds: Long,
        val coins: Double,
        val diamonds: Double,
        val wood: Double,
        val stone: Double,
        val food: Double
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        viewModelScope.launch {
            // First-time setup checks
            repository.checkAndInitialize()

            // Observe DB updates continuously and keep VM state synchronized
            launch {
                repository.gameStateFlow.collect { state ->
                    _gameState.value = state
                    calculateProductionRates()
                }
            }

            launch {
                repository.buildingsFlow.collect { bList ->
                    _buildings.value = bList
                    calculateProductionRates()
                }
            }

            launch {
                repository.equipmentFlow.collect { eqList ->
                    _equipment.value = eqList
                    calculateProductionRates()
                }
            }

            // Small delay to let data load, then calculate and handle offline/idle growth
            delay(300)
            handleOfflineGrowth()

            // Start game simulation tick loop
            startGameTick()
        }
    }

    private fun startGameTick() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                executeGameTick()
            }
        }
    }

    private fun executeGameTick() {
        val currentState = _gameState.value ?: return

        // Compute resource increases
        val incCoins = coinsPerSec.value
        val incDiamonds = diamondsPerSec.value
        val incWood = woodPerSec.value
        val incStone = stonePerSec.value
        val incFood = foodPerSec.value

        val updatedState = currentState.copy(
            coins = currentState.coins + incCoins,
            diamonds = currentState.diamonds + incDiamonds,
            wood = currentState.wood + incWood,
            stone = currentState.stone + incStone,
            food = currentState.food + incFood,
            lastActiveTime = System.currentTimeMillis()
        )

        _gameState.value = updatedState

        // Save state to room every 5 counts (every 5 seconds) to reduce write overhead, or when manual action triggers instant save
        saveTickCounter++
        if (saveTickCounter >= 5) {
            saveTickCounter = 0
            persistGameStateInstant()
        }
    }

    private fun persistGameStateInstant() {
        val stateToSave = _gameState.value ?: return
        viewModelScope.launch {
            repository.updateGameState(stateToSave)
        }
    }

    private fun handleOfflineGrowth() {
        val state = _gameState.value ?: return
        val current = System.currentTimeMillis()
        val elapsedMs = current - state.lastActiveTime
        val elapsedSec = elapsedMs / 1000L

        // Grant offline earnings if gone for more than 15 seconds
        if (elapsedSec >= 15L) {
            // Cap offline earnings at 12 hours (43200 seconds) to balance game economics
            val rateSec = min(elapsedSec, 43200L)

            // Compute values
            calculateProductionRates()
            val cEarned = coinsPerSec.value * rateSec
            val dEarned = diamondsPerSec.value * rateSec
            val wEarned = woodPerSec.value * rateSec
            val sEarned = stonePerSec.value * rateSec
            val fEarned = foodPerSec.value * rateSec

            if (cEarned > 0.1 || dEarned > 0.1 || wEarned > 0.1 || sEarned > 0.1 || fEarned > 0.1) {
                val updatedState = state.copy(
                    coins = state.coins + cEarned,
                    diamonds = state.diamonds + dEarned,
                    wood = state.wood + wEarned,
                    stone = state.stone + sEarned,
                    food = state.food + fEarned,
                    lastActiveTime = current
                )
                _gameState.value = updatedState
                persistGameStateInstant()

                _idleEarningsDialog.value = IdleEarnings(
                    seconds = rateSec,
                    coins = cEarned,
                    diamonds = dEarned,
                    wood = wEarned,
                    stone = sEarned,
                    food = fEarned
                )
            }
        }
    }

    fun dismissIdleEarnings() {
        _idleEarningsDialog.value = null
    }

    fun clearPromoToast() {
        _promoToast.value = null
    }

    fun clearInsufficientResources() {
        _insufficientResources.value = null
    }

    // Cost Calculator taking compounding discounts into account (e.g., Aegis Shield reduction)
    private fun getDiscountMultiplier(): Double {
        val shield = _equipment.value.find { it.id == "shield" } ?: return 1.0
        // Level 1 = 0% discount, Level 2 = 3% discount, ..., capped at 50% discount
        val rawDiscount = (shield.level - 1) * 0.03
        return max(0.5, 1.0 - rawDiscount)
    }

    // REDEEM PROMO CODE
    fun redeemPromoCode(code: String) {
        val normalizedCode = code.trim().uppercase()
        val state = _gameState.value ?: return
        val usedList = state.promoCodesUsed.split(",").filter { it.isNotBlank() }.toMutableSet()

        if (usedList.contains(normalizedCode)) {
            _promoToast.value = "Code '$normalizedCode' already redeemed!"
            return
        }

        when (normalizedCode) {
            "PXPUX_KING" -> {
                // Elite resource injection & Unique building unlocks for pxpux!
                usedList.add(normalizedCode)
                val currentUnlocks = state.unlockedUniqueBuildings.split(",").filter { it.isNotBlank() }.toMutableSet()
                currentUnlocks.add("Spire of pxpux")
                currentUnlocks.add("Infinity Zenith")

                val updated = state.copy(
                    coins = state.coins + 1_000_000.0,
                    diamonds = state.diamonds + 50_000.0,
                    wood = state.wood + 200_000.0,
                    stone = state.stone + 200_000.0,
                    food = state.food + 200_000.0,
                    unlockedUniqueBuildings = currentUnlocks.joinToString(","),
                    promoCodesUsed = usedList.joinToString(",")
                )
                _gameState.value = updated
                persistGameStateInstant()
                _promoToast.value = "👑 Welcome Sire pxpux! Received massive celestial resources & unlocked legendary architectural schematics!"
            }
            "FIRST_BUILDER" -> {
                usedList.add(normalizedCode)
                val updated = state.copy(
                    coins = state.coins + 25_000.0,
                    wood = state.wood + 10_000.0,
                    stone = state.stone + 8_000.0,
                    food = state.food + 10_000.0,
                    promoCodesUsed = usedList.joinToString(",")
                )
                _gameState.value = updated
                persistGameStateInstant()
                _promoToast.value = "⚒️ Redeemed FIRST_BUILDER: +25,000 Coins, +10k Wood/Food, +8k Stone!"
            }
            "DIAMOND_HEIST" -> {
                usedList.add(normalizedCode)
                val updated = state.copy(
                    diamonds = state.diamonds + 10_000.0,
                    promoCodesUsed = usedList.joinToString(",")
                )
                _gameState.value = updated
                persistGameStateInstant()
                _promoToast.value = "💎 Redeemed DIAMOND_HEIST: +10,000 Diamonds injected!"
            }
            "CHAMPION" -> {
                // Upgrade all equipments to lvl 5!
                usedList.add(normalizedCode)
                viewModelScope.launch {
                    val currentEquip = repository.getAllEquipment()
                    for (eq in currentEquip) {
                        repository.updateEquipment(eq.copy(level = max(5, eq.level)))
                    }
                    val updated = state.copy(promoCodesUsed = usedList.joinToString(","))
                    repository.updateGameState(updated)
                    _gameState.value = updated
                    _promoToast.value = "⚔️ Redeemed CHAMPION: All Royal Armory equipments boosted to Level 5!"
                }
            }
            else -> {
                _promoToast.value = "❌ Invalid Promo Code. Keep searching, traveler!"
            }
        }
    }

    // TRIBUTE TAX COLLECTION IN REALM CHAMBER
    fun collectTribute() {
        val state = _gameState.value ?: return
        if (state.food >= 150.0) {
            val updated = state.copy(
                food = state.food - 150.0,
                coins = state.coins + 1500.0
            )
            _gameState.value = updated
            persistGameStateInstant()
            _promoToast.value = "🌾 Royal Decree: Citizens paid 1,500 Gold tribute for your food stores!"
        } else {
            _insufficientResources.value = "Need Food: 150 for Tribute Collection"
        }
    }

    // EXPAND TERRITORY (Unlock Map Tile)
    fun unlockTerritory(gridIndex: Int) {
        val state = _gameState.value ?: return
        val bList = _buildings.value
        val targetBuilding = bList.find { it.gridIndex == gridIndex } ?: return

        if (targetBuilding.isUnlocked) return

        val disc = getDiscountMultiplier()
        val neededCoins = targetBuilding.unlockCostCoins * disc
        val neededWood = targetBuilding.unlockCostWood * disc
        val neededStone = targetBuilding.unlockCostStone * disc

        if (state.coins >= neededCoins && state.wood >= neededWood && state.stone >= neededStone) {
            val updatedState = state.copy(
                coins = state.coins - neededCoins,
                wood = state.wood - neededWood,
                stone = state.stone - neededStone
            )

            viewModelScope.launch {
                val updatedBuilding = targetBuilding.copy(isUnlocked = true)
                repository.updateBuilding(updatedBuilding)
                repository.updateGameState(updatedState)
                _gameState.value = updatedState
                _promoToast.value = "🏞️ Cleared wildlands! Territory expanded."
            }
        } else {
            _insufficientResources.value = "Need Gold: ${neededCoins.roundToLong()}, Wood: ${neededWood.roundToLong()}, Stone: ${neededStone.roundToLong()}"
        }
    }

    // CONSTRUCT BUILDING on Unlocked Empty Tile
    fun constructBuilding(gridIndex: Int, buildingType: String) {
        val state = _gameState.value ?: return
        val bList = _buildings.value
        val targetBuilding = bList.find { it.gridIndex == gridIndex } ?: return

        if (!targetBuilding.isUnlocked || targetBuilding.buildingType != null) return

        // Compute material costs
        val d = getDiscountMultiplier()
        val (goldCost, woodCost, stoneCost) = when (buildingType) {
            "Gold Mine" -> Triple(150.0 * d, 80.0 * d, 40.0 * d)
            "Lumber Yard" -> Triple(100.0 * d, 40.0 * d, 60.0 * d)
            "Quarry" -> Triple(120.0 * d, 70.0 * d, 30.0 * d)
            "Farm" -> Triple(80.0 * d, 50.0 * d, 30.0 * d)
            "Spire of pxpux" -> Triple(5000.0 * d, 3000.0 * d, 3000.0 * d) // High cost celestial spire
            "Infinity Zenith" -> Triple(10000.0 * d, 5000.0 * d, 5000.0 * d) // Ultimate diamond synthesizer
            else -> Triple(150.0 * d, 100.0 * d, 100.0 * d)
        }

        if (state.coins >= goldCost && state.wood >= woodCost && state.stone >= stoneCost) {
            val updatedState = state.copy(
                coins = state.coins - goldCost,
                wood = state.wood - woodCost,
                stone = state.stone - stoneCost
            )

            viewModelScope.launch {
                val updatedBuilding = targetBuilding.copy(
                    buildingType = buildingType,
                    level = 1
                )
                repository.updateBuilding(updatedBuilding)
                repository.updateGameState(updatedState)
                _gameState.value = updatedState
                _promoToast.value = "🏗️ Constructing production: $buildingType constructed!"
                calculateProductionRates()
            }
        } else {
            _insufficientResources.value = "Not enough building materials. Need Gold: ${goldCost.roundToLong()}, Wood: ${woodCost.roundToLong()}, Stone: ${stoneCost.roundToLong()}"
        }
    }

    // UPGRADE EXISTING BUILDING
    fun upgradeBuilding(gridIndex: Int) {
        val state = _gameState.value ?: return
        val bList = _buildings.value
        val targetBuilding = bList.find { it.gridIndex == gridIndex } ?: return

        if (!targetBuilding.isUnlocked || targetBuilding.buildingType == null) return

        val nextLevel = targetBuilding.level + 1
        val disc = getDiscountMultiplier()

        // Cost scales compounds per level (e.g., baseCost * 1.5 ^ level)
        val factor = 1.6.pow(targetBuilding.level - 1)
        val (baseGold, baseWood, baseStone) = when (targetBuilding.buildingType) {
            "Town Hall" -> Triple(300.0, 200.0, 200.0)
            "Gold Mine" -> Triple(120.0, 60.0, 30.0)
            "Lumber Yard" -> Triple(80.0, 30.0, 50.0)
            "Quarry" -> Triple(100.0, 55.0, 25.0)
            "Farm" -> Triple(60.0, 40.0, 25.0)
            "Spire of pxpux" -> Triple(4000.0, 2000.0, 2000.0)
            "Infinity Zenith" -> Triple(8000.0, 4000.0, 4000.0)
            else -> Triple(100.0, 50.0, 50.0)
        }

        val goldCost = (baseGold * factor * disc)
        val woodCost = (baseWood * factor * disc)
        val stoneCost = (baseStone * factor * disc)

        if (state.coins >= goldCost && state.wood >= woodCost && state.stone >= stoneCost) {
            val updatedState = state.copy(
                coins = state.coins - goldCost,
                wood = state.wood - woodCost,
                stone = state.stone - stoneCost
            )

            viewModelScope.launch {
                val updatedBuilding = targetBuilding.copy(level = nextLevel)
                repository.updateBuilding(updatedBuilding)
                repository.updateGameState(updatedState)
                _gameState.value = updatedState
                _promoToast.value = "⭐ Improved ${targetBuilding.buildingType} to Level $nextLevel!"
                calculateProductionRates()
            }
        } else {
            _insufficientResources.value = "Need Gold: ${goldCost.roundToLong()}, Wood: ${woodCost.roundToLong()}, Stone: ${stoneCost.roundToLong()}"
        }
    }

    // DEMOLISH / SELL BUILDING (Gives back 40% of building's initial construction cost value)
    fun sellBuilding(gridIndex: Int) {
        val state = _gameState.value ?: return
        val bList = _buildings.value
        val targetBuilding = bList.find { it.gridIndex == gridIndex } ?: return

        val bType = targetBuilding.buildingType ?: return
        if (bType == "Town Hall") return // Primary command structure is immutable

        val refundGold = when (bType) {
            "Gold Mine" -> 60.0
            "Lumber Yard" -> 40.0
            "Quarry" -> 50.0
            "Farm" -> 30.0
            "Spire of pxpux" -> 2000.0
            "Infinity Zenith" -> 4000.0
            else -> 60.0
        }

        viewModelScope.launch {
            val updatedBuilding = targetBuilding.copy(buildingType = null, level = 0)
            val updatedState = state.copy(
                coins = state.coins + refundGold,
                wood = state.wood + (refundGold * 0.4),
                stone = state.stone + (refundGold * 0.2)
            )
            repository.updateBuilding(updatedBuilding)
            repository.updateGameState(updatedState)
            _gameState.value = updatedState
            _promoToast.value = "🧹 Demolished and reclaimed construction materials!"
            calculateProductionRates()
        }
    }

    // UPGRADE EQUIPMENT IN ROYAL ARMORY
    fun upgradeEquipment(id: String) {
        val state = _gameState.value ?: return
        val eqList = _equipment.value
        val targetEquip = eqList.find { it.id == id } ?: return

        val multiplier = 1.7.pow(targetEquip.level - 1)
        val goldCost = targetEquip.upgradeCostCoins * multiplier
        val woodCost = targetEquip.upgradeCostWood * multiplier
        val stoneCost = targetEquip.upgradeCostStone * multiplier

        if (state.coins >= goldCost && state.wood >= woodCost && state.stone >= stoneCost) {
            val updatedState = state.copy(
                coins = state.coins - goldCost,
                wood = state.wood - woodCost,
                stone = state.stone - stoneCost
            )

            viewModelScope.launch {
                val updatedEquip = targetEquip.copy(
                    level = targetEquip.level + 1,
                    // Linear level multiplier increments
                    bonusValue = when (id) {
                        "crown" -> 0.05 + targetEquip.level * 0.05 // +5% gold per level
                        "sword" -> 0.05 + targetEquip.level * 0.05 // +5% crystal/sec
                        "shield" -> 0.03 + targetEquip.level * 0.03 // +3% discount/lvl
                        "ring" -> 0.05 + targetEquip.level * 0.05 // Overall production 5% bonus/lvl
                        else -> 0.05
                    }
                )
                repository.updateEquipment(updatedEquip)
                repository.updateGameState(updatedState)
                _gameState.value = updatedState
                _promoToast.value = "🛡️ Forge alert! Upgraded ${targetEquip.name}!"
                calculateProductionRates()
            }
        } else {
            _insufficientResources.value = "Need Gold: ${goldCost.roundToLong()}, Wood: ${woodCost.roundToLong()}, Stone: ${stoneCost.roundToLong()}"
        }
    }

    // GET REMAINING UPGRADE COST FOR EQUIPMENT
    fun getEquipmentUpgradeCost(equip: EquipmentEntity): Triple<Long, Long, Long> {
        val mul = 1.7.pow(equip.level - 1)
        return Triple(
            (equip.upgradeCostCoins * mul).roundToLong(),
            (equip.upgradeCostWood * mul).roundToLong(),
            (equip.upgradeCostStone * mul).roundToLong()
        )
    }

    // GET REMAINING UPGRADE COST FOR BUILDING
    fun getBuildingUpgradeCost(building: BuildingEntity): Triple<Long, Long, Long>? {
        if (!building.isUnlocked || building.buildingType == null) return null
        val disc = getDiscountMultiplier()
        val factor = 1.6.pow(building.level - 1)
        val (baseGold, baseWood, baseStone) = when (building.buildingType) {
            "Town Hall" -> Triple(300.0, 200.0, 200.0)
            "Gold Mine" -> Triple(120.0, 60.0, 30.0)
            "Lumber Yard" -> Triple(80.0, 30.0, 50.0)
            "Quarry" -> Triple(100.0, 55.0, 25.0)
            "Farm" -> Triple(60.0, 40.0, 25.0)
            "Spire of pxpux" -> Triple(4000.0, 2000.0, 2000.0)
            "Infinity Zenith" -> Triple(8000.0, 4000.0, 4000.0)
            else -> Triple(100.0, 50.0, 50.0)
        }
        return Triple(
            (baseGold * factor * disc).roundToLong(),
            (baseWood * factor * disc).roundToLong(),
            (baseStone * factor * disc).roundToLong()
        )
    }

    // PRODUCTION RATE ENGINE CALCULATIONS
    private fun calculateProductionRates() {
        val buildingsList = _buildings.value
        val eqList = _equipment.value

        // Fetch upgrade multipliers from armory
        val crown = eqList.find { it.id == "crown" }
        val ring = eqList.find { it.id == "ring" }
        val sword = eqList.find { it.id == "sword" }

        val crownMultiplier = 1.0 + ((crown?.level?.minus(1) ?: 0) * 0.05) // Gold specific boost
        val ringMultiplier = 1.0 + ((ring?.level?.minus(1) ?: 0) * 0.05)   // All-resource multiplier
        val swordMultiplier = 1.0 + ((sword?.level?.minus(1) ?: 0) * 0.05) // Crystal/Special production multiplier

        var rawCoins = 0.0
        var rawDiamonds = 0.0
        var rawWood = 0.0
        var rawStone = 0.0
        var rawFood = 0.0

        for (b in buildingsList) {
            if (!b.isUnlocked || b.buildingType == null) continue

            val lvl = b.level
            when (b.buildingType) {
                "Town Hall" -> {
                    // Small base support rate for all resources
                    rawCoins += lvl * 0.5
                    rawWood += lvl * 0.2
                    rawStone += lvl * 0.2
                    rawFood += lvl * 0.2
                    rawDiamonds += lvl * 0.01
                }
                "Gold Mine" -> {
                    rawCoins += lvl * 1.5
                }
                "Lumber Yard" -> {
                    rawWood += lvl * 1.2
                }
                "Quarry" -> {
                    rawStone += lvl * 0.8
                }
                "Farm" -> {
                    rawFood += lvl * 1.0
                }
                "Spire of pxpux" -> {
                    // Generates premium gold & massive secondary resources
                    rawCoins += lvl * 10.0
                    rawDiamonds += lvl * 0.20
                    rawWood += lvl * 5.0
                    rawStone += lvl * 5.0
                    rawFood += lvl * 5.0
                }
                "Infinity Zenith" -> {
                    // Legendary celestial structures synthesizing raw crystals (diamonds)
                    rawDiamonds += lvl * 1.5
                    rawCoins += lvl * 25.0
                }
            }
        }

        // Apply equipment boosts to final production metrics
        coinsPerSec.value = rawCoins * crownMultiplier * ringMultiplier
        diamondsPerSec.value = rawDiamonds * swordMultiplier * ringMultiplier
        woodPerSec.value = rawWood * ringMultiplier
        stonePerSec.value = rawStone * ringMultiplier
        foodPerSec.value = rawFood * ringMultiplier
    }
}
