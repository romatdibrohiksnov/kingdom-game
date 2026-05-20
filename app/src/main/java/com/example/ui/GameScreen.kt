package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BuildingEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.GameStateEntity
import com.example.ui.theme.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val buildings by viewModel.buildings.collectAsStateWithLifecycle()
    val equipment by viewModel.equipment.collectAsStateWithLifecycle()

    val idleEarnings by viewModel.idleEarningsDialog.collectAsStateWithLifecycle()
    val promoToast by viewModel.promoToast.collectAsStateWithLifecycle()
    val insufficientRes by viewModel.insufficientResources.collectAsStateWithLifecycle()

    val coinsPerSec by viewModel.coinsPerSec.collectAsStateWithLifecycle()
    val diamondsPerSec by viewModel.diamondsPerSec.collectAsStateWithLifecycle()
    val woodPerSec by viewModel.woodPerSec.collectAsStateWithLifecycle()
    val stonePerSec by viewModel.stonePerSec.collectAsStateWithLifecycle()
    val foodPerSec by viewModel.foodPerSec.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("kingdom") } // "kingdom", "armory", "treasury"

    // Dialog state
    var selectedBuildingForAction by remember { mutableStateOf<BuildingEntity?>(null) }
    var showBuildSelectionDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val townHallLvl = buildings.firstOrNull { it.buildingType == "Town Hall" }?.level ?: 1

    // Screen Layout
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "👑 KINGDOM REALM",
                            color = SleekBrandPurple,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        gameState?.let {
                            val activeCodes = it.promoCodesUsed.split(",").filter { c -> c.isNotBlank() }
                            Text(
                                text = if (activeCodes.contains("PXPUX_KING")) "Blessed by Celestial pxpux" else "Sovereign Lord",
                                color = SleekGrayText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Ticks active indicator
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekLightPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(LifeGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE SIM",
                                color = SleekDarkPurpleText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Capsules: Gold, Crystals, Fort Level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Gold Capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SleekLightPurple)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🪙", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatAmount(gameState?.coins ?: 0.0),
                            color = SleekDarkPurpleText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Crystal Capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SleekLightBlue)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("💎", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatAmount(gameState?.diamonds ?: 0.0),
                            color = SleekDarkBlueText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Fort Level Capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SleekLightRose)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🏰", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LVL $townHallLvl",
                            color = SleekDarkRoseText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary dividers Row: Wood, Stone, Bread
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            drawLine(
                                color = SleekBorder,
                                start = androidx.compose.ui.geometry.Offset(0f, size.height),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                strokeWidth = strokeWidth
                            )
                        }
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🌲", fontSize = 12.sp)
                        Text(
                            text = "Wood: ${formatAmount(gameState?.wood ?: 0.0)}",
                            color = SleekGrayText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🪨", fontSize = 12.sp)
                        Text(
                            text = "Stone: ${formatAmount(gameState?.stone ?: 0.0)}",
                            color = SleekGrayText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🍞", fontSize = 12.sp)
                        Text(
                            text = "Bread: ${formatAmount(gameState?.food ?: 0.0)}",
                            color = SleekGrayText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = {
            // High-fidelity Custom Styled Bottom tab
            NavigationBar(
                containerColor = Color(0xFFF7F2FA),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .height(82.dp)
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = SleekBorder,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = strokeWidth
                        )
                    }
            ) {
                NavigationBarItem(
                    selected = activeTab == "kingdom",
                    onClick = { activeTab = "kingdom" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Kingdom") },
                    label = { Text("My Realm", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SleekTextDark,
                        selectedTextColor = SleekTextDark,
                        indicatorColor = SleekLightPurple,
                        unselectedIconColor = SleekGrayText,
                        unselectedTextColor = SleekGrayText
                    ),
                    modifier = Modifier.testTag("tab_kingdom")
                )
                NavigationBarItem(
                    selected = activeTab == "armory",
                    onClick = { activeTab = "armory" },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Armory") },
                    label = { Text("Royal Forge", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SleekTextDark,
                        selectedTextColor = SleekTextDark,
                        indicatorColor = SleekLightPurple,
                        unselectedIconColor = SleekGrayText,
                        unselectedTextColor = SleekGrayText
                    ),
                    modifier = Modifier.testTag("tab_armory")
                )
                NavigationBarItem(
                    selected = activeTab == "treasury",
                    onClick = { activeTab = "treasury" },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Treasury") },
                    label = { Text("Scroll Chamber", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SleekTextDark,
                        selectedTextColor = SleekTextDark,
                        indicatorColor = SleekLightPurple,
                        unselectedIconColor = SleekGrayText,
                        unselectedTextColor = SleekGrayText
                    ),
                    modifier = Modifier.testTag("tab_treasury")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SleekBg)
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "kingdom" -> KingdomTabContent(
                    buildings = buildings,
                    onTileClick = { building ->
                        selectedBuildingForAction = building
                        if (building.isUnlocked) {
                            if (building.buildingType == null) {
                                showBuildSelectionDialog = true
                            } else {
                                // Show upgrade/sell dialog
                            }
                        } else {
                            // Automatically triggers prompt confirmation
                        }
                    }
                )
                "armory" -> ArmoryTabContent(
                    equipmentList = equipment,
                    viewModel = viewModel
                )
                "treasury" -> TreasuryTabContent(
                    gameState = gameState,
                    viewModel = viewModel
                )
            }

            // --- DETAILED TILE INTERACTION INTERFACES ---
            selectedBuildingForAction?.let { building ->
                if (!building.isUnlocked) {
                    // Unlock territory dialog
                    AlertDialog(
                        onDismissRequest = { selectedBuildingForAction = null },
                        title = { Text("🗺️ Claim Wilderness", color = RoyalGold, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text(
                                    text = "This sector is wrapped in ancient wilderness. Clear the woods to claim it for your kingdom!",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Expansion Costs:", fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CostTag(name = "Gold", amount = building.unlockCostCoins, balance = gameState?.coins ?: 0.0)
                                    CostTag(name = "Lumber", amount = building.unlockCostWood, balance = gameState?.wood ?: 0.0)
                                    CostTag(name = "Stone", amount = building.unlockCostStone, balance = gameState?.stone ?: 0.0)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.unlockTerritory(building.gridIndex)
                                    selectedBuildingForAction = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                            ) {
                                Text("Settle Tile", color = DarkCharcoal, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { selectedBuildingForAction = null }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        },
                        containerColor = SlateGrey
                    )
                } else if (building.buildingType != null && !showBuildSelectionDialog) {
                    // Upgrade / Demolish dialog
                    val isTownHall = building.buildingType == "Town Hall"
                    val costs = viewModel.getBuildingUpgradeCost(building)

                    AlertDialog(
                        onDismissRequest = { selectedBuildingForAction = null },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${getBuildingEmoji(building.buildingType)} ${building.buildingType}",
                                    color = RoyalGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lvl. ${building.level}",
                                    color = CelestialBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        text = {
                            Column {
                                Text(
                                    text = getBuildingDescription(building.buildingType),
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Current State: Production Lvl ${building.level}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                if (costs != null) {
                                    Text("Upgrade Upgrade costs:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CostTag(name = "Gold", amount = costs.first.toDouble(), balance = gameState?.coins ?: 0.0)
                                        CostTag(name = "Lumber", amount = costs.second.toDouble(), balance = gameState?.wood ?: 0.0)
                                        CostTag(name = "Stone", amount = costs.third.toDouble(), balance = gameState?.stone ?: 0.0)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.upgradeBuilding(building.gridIndex)
                                    selectedBuildingForAction = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                            ) {
                                Text("Upgrade Scheme", color = DarkCharcoal, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            Row {
                                if (!isTownHall) {
                                    TextButton(
                                        onClick = {
                                            viewModel.sellBuilding(building.gridIndex)
                                            selectedBuildingForAction = null
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text("Demolish", fontWeight = FontWeight.Bold)
                                    }
                                }
                                TextButton(onClick = { selectedBuildingForAction = null }) {
                                    Text("Cancel", color = Color.Gray)
                                }
                            }
                        },
                        containerColor = SlateGrey
                    )
                }
            }

            // Construction choice Dialog
            if (showBuildSelectionDialog && selectedBuildingForAction != null) {
                val buildingIndex = selectedBuildingForAction!!.gridIndex
                val isPxpuxUnlocked = gameState?.unlockedUniqueBuildings?.contains("Spire of pxpux") == true

                AlertDialog(
                    onDismissRequest = {
                        showBuildSelectionDialog = false
                        selectedBuildingForAction = null
                    },
                    title = { Text("🏗️ Municipal Planner", color = RoyalGold, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Select structural scheme blueprints to deploy:", color = Color.Gray, fontSize = 14.sp)

                            BuildBlueprintRow(
                                title = "Gold Mine",
                                desc = "Extracts royal bullion gold coins dynamically.",
                                icon = "⛏️",
                                details = "Lvl 1 base: +1.5 Gold/s",
                                coins = 150.0, wood = 80.0, stone = 40.0,
                                gameState = gameState,
                                onSelect = {
                                    viewModel.constructBuilding(buildingIndex, "Gold Mine")
                                    showBuildSelectionDialog = false
                                    selectedBuildingForAction = null
                                }
                            )

                            BuildBlueprintRow(
                                title = "Lumber Yard",
                                desc = "Fells forestry wood automatically.",
                                icon = "🪵",
                                details = "Lvl 1 base: +1.2 Wood/s",
                                coins = 100.0, wood = 40.0, stone = 60.0,
                                gameState = gameState,
                                onSelect = {
                                    viewModel.constructBuilding(buildingIndex, "Lumber Yard")
                                    showBuildSelectionDialog = false
                                    selectedBuildingForAction = null
                                }
                            )

                            BuildBlueprintRow(
                                title = "Quarry",
                                desc = "Heaves granite stones for castle reinforcements.",
                                icon = "🪨",
                                details = "Lvl 1 base: +0.8 Stone/s",
                                coins = 120.0, wood = 70.0, stone = 30.0,
                                gameState = gameState,
                                onSelect = {
                                    viewModel.constructBuilding(buildingIndex, "Quarry")
                                    showBuildSelectionDialog = false
                                    selectedBuildingForAction = null
                                }
                            )

                            BuildBlueprintRow(
                                title = "Farm",
                                desc = "Cultivates fertile crops for citizens.",
                                icon = "🌾",
                                details = "Lvl 1 base: +1.0 Food/s",
                                coins = 80.0, wood = 50.0, stone = 30.0,
                                gameState = gameState,
                                onSelect = {
                                    viewModel.constructBuilding(buildingIndex, "Farm")
                                    showBuildSelectionDialog = false
                                    selectedBuildingForAction = null
                                }
                            )

                            // Promo Special celestial structures
                            if (isPxpuxUnlocked) {
                                Divider(color = LighterGrey, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                Text("🌌 Celestial Blueprints (PROMO UNLOCKED):", color = CelestialBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                BuildBlueprintRow(
                                    title = "Spire of pxpux",
                                    desc = "A glowing tower feeding crystal magic to the realm. Supercharges production.",
                                    icon = "🌌",
                                    details = "Lvl 1 base: +10 Gold, +0.2 Gems, +5 Wood/Stone/Food",
                                    coins = 5000.0, wood = 3000.0, stone = 3000.0,
                                    borderColor = RoyalGold,
                                    gameState = gameState,
                                    onSelect = {
                                        viewModel.constructBuilding(buildingIndex, "Spire of pxpux")
                                        showBuildSelectionDialog = false
                                        selectedBuildingForAction = null
                                    }
                                )

                                BuildBlueprintRow(
                                    title = "Infinity Zenith",
                                    desc = "Direct star-shaper synthesizing raw space diamonds.",
                                    icon = "💎",
                                    details = "Lvl 1 base: +1.5 Gems/s, +25 Gold/s",
                                    coins = 10000.0, wood = 5000.0, stone = 5000.0,
                                    borderColor = CelestialBlue,
                                    gameState = gameState,
                                    onSelect = {
                                        viewModel.constructBuilding(buildingIndex, "Infinity Zenith")
                                        showBuildSelectionDialog = false
                                        selectedBuildingForAction = null
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = {
                            showBuildSelectionDialog = false
                            selectedBuildingForAction = null
                        }) {
                            Text("Dismiss", color = Color.Gray)
                        }
                    },
                    containerColor = SlateGrey
                )
            }

            // --- IDLE EARNINGS POPUP ---
            idleEarnings?.let { ie ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissIdleEarnings() },
                    title = { Text("🏰 Realm Prosperity While Absent", color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    text = {
                        Column {
                            val mins = ie.seconds / 60
                            val displayTime = if (mins > 0) "$mins mins" else "${ie.seconds} secs"
                            Text(
                                text = "Your loyal direct subjects have labored faithfully for $displayTime while you were ruling away:",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                RevenueYieldRow(name = "Gold Yield", amount = ie.coins, color = RoyalGold, flag = "🪙")
                                RevenueYieldRow(name = "Crystal Yield", amount = ie.diamonds, color = CelestialBlue, flag = "💎")
                                RevenueYieldRow(name = "Lumber Gathered", amount = ie.wood, color = WoodBrown, flag = "🪵")
                                RevenueYieldRow(name = "Granite Excavated", amount = ie.stone, color = StoneGrey, flag = "🪨")
                                RevenueYieldRow(name = "Harvest Store", amount = ie.food, color = SoftGold, flag = "🍞")
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissIdleEarnings() },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                        ) {
                            Text("Enforce Tribute", color = DarkCharcoal, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = SlateGrey
                )
            }

            // --- REDEMPTION PROMO NOTIFIER ---
            promoToast?.let { text ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearPromoToast() },
                    title = { Text("📜 Relic Redeemed", color = RoyalGold, fontWeight = FontWeight.Bold) },
                    text = { Text(text, color = OnBackgroundText) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearPromoToast() }, colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)) {
                            Text("Accept Gift", color = DarkCharcoal, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = SlateGrey
                )
            }

            // --- INSUFFICIENT RESOURCES ERROR ---
            insufficientRes?.let { errorText ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearInsufficientResources() },
                    title = { Text("⚠️ Royal Coffers Exhausted", color = Color.Red, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = "My Liege! We do not possess sufficient state materials to execute this order.\n\nRequired:\n$errorText",
                            color = OnBackgroundText
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.clearInsufficientResources() },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateGrey)
                        ) {
                            Text("Understood", color = RoyalGold, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFF221115)
                )
            }
        }
    }
}

// ================= COMPOSABLE COMPONENT SUB-SYSTEMS =================

@Composable
fun ResourceItem(
    name: String,
    amount: Double,
    rate: Double,
    icon: String,
    color: Color
) {
    val rateVal = rate
    Card(
        colors = CardDefaults.cardColors(containerColor = LighterGrey),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.widthIn(min = 100.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = name, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatAmount(amount),
                color = OnBackgroundText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "+${formatAmount(rateVal)}/s",
                color = if (rateVal > 0.0) LifeGreen else Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CostTag(name: String, amount: Double, balance: Double) {
    val canAfford = balance >= amount
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford) LighterGrey else Color(0xFF33151A)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val emoji = when (name) {
                "Gold" -> "🪙"
                "Lumber" -> "🪵"
                "Stone" -> "🪨"
                else -> "📦"
            }
            Text("$emoji $name", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(
                text = formatAmount(amount),
                color = if (canAfford) Color.White else Color.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun RevenueYieldRow(name: String, amount: Double, color: Color, flag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(flag, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, color = Color.LightGray, fontSize = 14.sp)
        }
        Text(
            text = "+${formatAmount(amount)}",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun BuildBlueprintRow(
    title: String,
    desc: String,
    icon: String,
    details: String,
    coins: Double,
    wood: Double,
    stone: Double,
    borderColor: Color = Color.Transparent,
    gameState: GameStateEntity?,
    onSelect: () -> Unit
) {
    val canAfford = (gameState?.coins ?: 0.0) >= coins &&
            (gameState?.wood ?: 0.0) >= wood &&
            (gameState?.stone ?: 0.0) >= stone

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (borderColor != Color.Transparent) borderColor else LighterGrey, RoundedCornerShape(12.dp))
            .clickable(enabled = canAfford) { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford) LighterGrey else Color(0xFF20222B)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (borderColor != Color.Transparent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1430)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "CELESTIAL",
                                color = CelestialBlue,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(desc, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                Text(details, color = LifeGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🪙 ${coins.roundToLong()}", color = if ((gameState?.coins ?: 0.0) >= coins) Color.LightGray else Color.Red, fontSize = 10.sp)
                    Text("🪵 ${wood.roundToLong()}", color = if ((gameState?.wood ?: 0.0) >= wood) Color.LightGray else Color.Red, fontSize = 10.sp)
                    Text("🪨 ${stone.roundToLong()}", color = if ((gameState?.stone ?: 0.0) >= stone) Color.LightGray else Color.Red, fontSize = 10.sp)
                }
            }
            if (!canAfford) {
                Text("🔒 LACK STORES", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("DEPLOY >", color = RoyalGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- KINGDOM TAB (MAP GRID) ---
@Composable
fun KingdomTabContent(
    buildings: List<BuildingEntity>,
    onTileClick: (BuildingEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SOVEREIGN BLUEPRINT DIRECTIVES",
            color = SleekBrandPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 5x5 Map Layout Grid representing our real-time simulation land tiles
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(SleekLightSurface)
                .border(2.dp, SleekBorder, RoundedCornerShape(28.dp))
                .drawBehind {
                    val dotRadius = 1.2.dp.toPx()
                    val dotSpacing = 16.dp.toPx()
                    val dotColor = SleekBrandPurple.copy(alpha = 0.08f)
                    var x = dotSpacing / 2
                    while (x < size.width) {
                        var y = dotSpacing / 2
                        while (y < size.height) {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }
                .padding(12.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(buildings, key = { it.gridIndex }) { building ->
                    KingdomTile(
                        building = building,
                        onClick = { onTileClick(building) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Strategy Tips Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSolidWhite),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ️", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Realm Expansion Directives",
                        color = SleekTextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Clear forests to claim bordering tiles. Upgrade your Town Hall to boost central growth tax speeds.",
                        color = SleekGrayText,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun KingdomTile(
    building: BuildingEntity,
    onClick: () -> Unit
) {
    val isTH = building.buildingType == "Town Hall"
    val isCelestial = building.buildingType?.contains("Spire") == true || building.buildingType?.contains("Zenith") == true

    val cardBg = if (!building.isUnlocked) {
        Color(0xFFE2DCE5) // Unlocked/Claimable state
    } else if (building.buildingType == null) {
        SleekLightSurface
    } else if (isTH) {
        SleekBrandPurple
    } else if (isCelestial) {
        SleekDragonBg
    } else {
        SleekSolidWhite
    }

    val tileShape = if (building.isUnlocked && building.buildingType == null) {
        RoundedCornerShape(24.dp) // Circular empty plots
    } else {
        RoundedCornerShape(12.dp) // Nice dynamic square rounded shape
    }

    val borderStroke = if (!building.isUnlocked) {
        null
    } else if (building.buildingType == null) {
        BorderStroke(1.5.dp, SleekBorder)
    } else if (isTH) {
        BorderStroke(1.5.dp, SleekLightRose)
    } else if (isCelestial) {
        BorderStroke(1.5.dp, SleekDragonText)
    } else {
        BorderStroke(1.dp, SleekBorder)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(tileShape)
            .background(cardBg)
            .then(if (borderStroke != null) Modifier.border(borderStroke, tileShape) else Modifier)
            .clickable { onClick() }
            .testTag("kingdom_tile_${building.gridIndex}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!building.isUnlocked) {
                Text(text = "🔒", fontSize = 14.sp)
            } else if (building.buildingType == null) {
                Text(text = "+", color = SleekBrandPurple, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(text = getBuildingEmoji(building.buildingType), fontSize = 18.sp)
                Text(
                    text = "Lvl ${building.level}",
                    color = if (isTH) SleekLightRose else if (isCelestial) SleekDragonText else SleekGrayText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- ARMORY TAB CONTENT ---
@Composable
fun ArmoryTabContent(
    equipmentList: List<EquipmentEntity>,
    viewModel: GameViewModel
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ROYAL FORGE & ARTIFACT REFORGING",
            color = SleekBrandPurple,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Text(
            text = "Temper mythical armory artifacts with your state resources. Upgraded artifacts yield compounding production multipliers across the entire kingdom territory.",
            color = SleekGrayText,
            fontSize = 11.sp
        )

        for (equip in equipmentList) {
            val costs = viewModel.getEquipmentUpgradeCost(equip)
            val canUpgrade = (gameState?.coins ?: 0.0) >= costs.first &&
                    (gameState?.wood ?: 0.0) >= costs.second &&
                    (gameState?.stone ?: 0.0) >= costs.third

            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSolidWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left brand symbol icon box
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(SleekBg)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.2.dp, SleekBrandPurple, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getEquipmentEmoji(equip.id), fontSize = 30.sp)
                    }

                    // Center Details
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = equip.name,
                            color = SleekTextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        // Progress slider mapping level
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            val activeProgress = minOf((equip.level / 12f), 1f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SleekBorder)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(activeProgress)
                                        .height(6.dp)
                                        .background(SleekBrandPurple)
                                )
                            }
                            Text(
                                text = "Lv. ${equip.level}",
                                color = SleekGrayText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = getEquipmentDetails(equip),
                            color = LifeGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Secondary Wood / Stone costs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "🪵 ${costs.second}",
                                color = if ((gameState?.wood ?: 0.0) >= costs.second) SleekGrayText else Color.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "🪨 ${costs.third}",
                                color = if ((gameState?.stone ?: 0.0) >= costs.third) SleekGrayText else Color.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Upgrade action capsule
                    Button(
                        onClick = { viewModel.upgradeEquipment(equip.id) },
                        enabled = canUpgrade,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekBrandPurple,
                            disabledContainerColor = SleekBorder
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .testTag("forge_button_${equip.id}")
                            .widthIn(min = 85.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "FORGE",
                                color = if (canUpgrade) SleekSolidWhite else SleekGrayText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "🪙 ${formatAmount(costs.first.toDouble())}",
                                color = if (canUpgrade) SleekSolidWhite.copy(alpha = 0.85f) else SleekGrayText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- TREASURY & ROYAL CODE TAB ---
@Composable
fun TreasuryTabContent(
    gameState: GameStateEntity?,
    viewModel: GameViewModel
) {
    var codeText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SACRED SCHEMATICS CHAMBER",
            color = SleekBrandPurple,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Ancient Scroll promo code container
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSolidWhite),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📜 Ancient Redwood Scroll",
                    color = SleekBrandPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Decrypt hidden cosmic promo codes to manifest massive resource boxes and unlock legendary celestial construction blueprints.",
                    color = SleekGrayText,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    label = { Text("Input Ancient Code", color = SleekGrayText, fontSize = 11.sp) },
                    placeholder = { Text("E.g. FIRST_BUILDER", color = SleekBorder, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("promo_code_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekBrandPurple,
                        unfocusedBorderColor = SleekBorder,
                        focusedTextColor = SleekTextDark,
                        unfocusedTextColor = SleekTextDark,
                        focusedLabelColor = SleekBrandPurple,
                        unfocusedLabelColor = SleekGrayText
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.redeemPromoCode(codeText)
                        codeText = ""
                    },
                    modifier = Modifier.fillMaxWidth().testTag("redeem_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBrandPurple),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Redeem Secret Scroll", color = SleekSolidWhite, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active State stats summary
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSolidWhite),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📊 State Empire Overview",
                    color = SleekTextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Divider(color = SleekBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Special Scrolls Redeemed:", color = SleekGrayText, fontSize = 12.sp)
                    val count = gameState?.promoCodesUsed?.split(",")?.filter { it.isNotBlank() }?.size ?: 0
                    Text("$count Scrolls Active", color = SleekBrandPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Celestial Blueprints Unlocked:", color = SleekGrayText, fontSize = 12.sp)
                    val hasPxpux = gameState?.unlockedUniqueBuildings?.contains("Spire of pxpux") ?: false
                    Text(if (hasPxpux) "All Blueprints" else "Normal Only", color = if (hasPxpux) SleekBrandPurple else SleekGrayText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Forge Level Cap:", color = SleekGrayText, fontSize = 12.sp)
                    Text("Mythic Core", color = SleekBrandPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tactical feature: Tribute collect (Designed capsule highlight)
                Button(
                    onClick = {
                        viewModel.collectTribute()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekLightPurple),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().testTag("collect_tax_button")
                ) {
                    Text("🌾 Sacrifice 150 Bread for 🪙 1,500 Gold", color = SleekDarkPurpleText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ================= UTILITIES & HELPERS =================

fun formatAmount(value: Double): String {
    return if (value >= 1_000_000.0) {
        String.format("%.2fM", value / 1_000_000.0)
    } else if (value >= 1_000.0) {
        String.format("%.1fk", value / 1_000.0)
    } else {
        value.roundToLong().toString()
    }
}

fun getBuildingEmoji(type: String?): String {
    return when (type) {
        "Town Hall" -> "🏛️"
        "Gold Mine" -> "⛏️"
        "Lumber Yard" -> "🪵"
        "Quarry" -> "🪨"
        "Farm" -> "🌾"
        "Spire of pxpux" -> "🌌"
        "Infinity Zenith" -> "👑"
        else -> "🌳"
    }
}

fun getBuildingDescription(type: String?): String {
    return when (type) {
        "Town Hall" -> "The central administrative core of your realm. Employs active clerks to coordinate collection of basic taxes."
        "Gold Mine" -> "Deep earth mines extracting precious gold. Boosted heavily by the legendary Crown of Dominion."
        "Lumber Yard" -> "Logging yard producing lumber. Essential for reinforcing massive structures."
        "Quarry" -> "Excavation pits providing premium building stones."
        "Farm" -> "Arable farmlands cultivating heavy crops to satisfy city residents and tradesmen."
        "Spire of pxpux" -> "A divine interstellar beacon from celestial realms, pulsing dynamic cosmic production into all bordering coordinates!"
        "Infinity Zenith" -> "The absolute peak of interstellar alchemy. Synthesizes premium cosmos diamonds continuously."
        else -> ""
    }
}

fun getEquipmentEmoji(id: String): String {
    return when (id) {
        "crown" -> "👑"
        "sword" -> "⚔️"
        "shield" -> "🛡️"
        "ring" -> "💍"
        else -> "🛡️"
    }
}

fun getEquipmentDetails(equip: EquipmentEntity): String {
    val lvl = equip.level
    return when (equip.id) {
        "crown" -> "Gold Production Boost: +${(lvl - 1) * 5}%"
        "sword" -> "Diamond Synthesis Rate: +${(lvl - 1) * 5}%"
        "shield" -> "Construction Discount: ${(lvl - 1) * 3}% (Max 50%)"
        "ring" -> "All Production Rates Multiply: +${(lvl - 1) * 5}%"
        else -> ""
    }
}
