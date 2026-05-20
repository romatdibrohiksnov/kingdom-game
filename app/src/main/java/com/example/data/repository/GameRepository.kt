package com.example.data.repository

import com.example.data.db.GameDao
import com.example.data.model.BuildingEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.GameStateEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

class GameRepository(private val gameDao: GameDao) {

    val gameStateFlow: Flow<GameStateEntity?> = gameDao.getGameStateFlow()
    val buildingsFlow: Flow<List<BuildingEntity>> = gameDao.getAllBuildingsFlow()
    val equipmentFlow: Flow<List<EquipmentEntity>> = gameDao.getAllEquipmentFlow()

    suspend fun checkAndInitialize() {
        val existingState = gameDao.getGameState()
        if (existingState == null) {
            // Populate Game State
            val defaultState = GameStateEntity(
                id = 1,
                coins = 1000.0,
                diamonds = 100.0,
                wood = 500.0,
                stone = 300.0,
                food = 400.0,
                lastActiveTime = System.currentTimeMillis()
            )
            gameDao.insertGameState(defaultState)

            // Populate Equipments
            val defaultEquipment = listOf(
                EquipmentEntity(
                    id = "crown",
                    name = "Crown of Dominion",
                    level = 1,
                    bonusValue = 0.05, // 5% boost to gold production/level
                    upgradeCostCoins = 150.0,
                    upgradeCostWood = 50.0,
                    upgradeCostStone = 50.0
                ),
                EquipmentEntity(
                    id = "sword",
                    name = "Sword of Majesty",
                    level = 1,
                    bonusValue = 0.05, // 5% boost to crystal production/level (Celestial / Promo houses)
                    upgradeCostCoins = 200.0,
                    upgradeCostWood = 60.0,
                    upgradeCostStone = 60.0
                ),
                EquipmentEntity(
                    id = "shield",
                    name = "Aegis Shield",
                    level = 1,
                    bonusValue = 0.03, // 3% cost discount/level
                    upgradeCostCoins = 180.0,
                    upgradeCostWood = 70.0,
                    upgradeCostStone = 70.0
                ),
                EquipmentEntity(
                    id = "ring",
                    name = "Ring of Prosperity",
                    level = 1,
                    bonusValue = 0.05, // 5% passive bonus to all resource generation/level
                    upgradeCostCoins = 250.0,
                    upgradeCostWood = 80.0,
                    upgradeCostStone = 80.0
                )
            )
            gameDao.insertEquipment(defaultEquipment)

            // Populate 5x5 Map Grid (25 tiles, 0 to 24)
            val buildings = mutableListOf<BuildingEntity>()
            for (i in 0 until 25) {
                val col = i % 5
                val row = i / 5
                val dist = abs(col - 2) + abs(row - 2)
                
                // If distance to center is <= 1, unlock the tile immediately as starting territory
                val isUnlocked = dist <= 1
                
                val buildingType = when {
                    i == 12 -> "Town Hall"
                    i == 11 -> "Gold Mine"
                    i == 13 -> "Lumber Yard"
                    i == 7 -> "Quarry"
                    i == 17 -> "Farm"
                    else -> null // Empty land
                }
                
                val level = if (buildingType != null) 1 else 0

                // Unlock cost scales exponentially with distance from center
                val scale = if (dist > 1) dist - 1 else 1
                val unlockCostCoins = 100.0 * scale * scale
                val unlockCostWood = 50.0 * scale * scale
                val unlockCostStone = 30.0 * scale * scale

                buildings.add(
                    BuildingEntity(
                        gridIndex = i,
                        buildingType = buildingType,
                        level = level,
                        isUnlocked = isUnlocked,
                        unlockCostCoins = unlockCostCoins,
                        unlockCostWood = unlockCostWood,
                        unlockCostStone = unlockCostStone
                    )
                )
            }
            gameDao.insertBuildings(buildings)
        }
    }

    suspend fun updateGameState(state: GameStateEntity) {
        gameDao.insertGameState(state)
    }

    suspend fun updateBuilding(building: BuildingEntity) {
        gameDao.updateBuilding(building)
    }

    suspend fun updateEquipment(equipment: EquipmentEntity) {
        gameDao.updateEquipment(equipment)
    }

    suspend fun getGameState(): GameStateEntity? {
        return gameDao.getGameState()
    }

    suspend fun getAllBuildings(): List<BuildingEntity> {
        return gameDao.getAllBuildings()
    }

    suspend fun getAllEquipment(): List<EquipmentEntity> {
        return gameDao.getAllEquipment()
    }
}
