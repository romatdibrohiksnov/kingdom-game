package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buildings")
data class BuildingEntity(
    @PrimaryKey val gridIndex: Int,
    val buildingType: String?, // "Town Hall", "Gold Mine", "Lumber Yard", "Quarry", "Farm", "Celestial Obelisk" (Promo-only), "Spire of pxpux" (Promo-only)
    val level: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockCostCoins: Double = 0.0,
    val unlockCostWood: Double = 0.0,
    val unlockCostStone: Double = 0.0
)
