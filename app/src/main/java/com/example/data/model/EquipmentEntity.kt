package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey val id: String, // "crown", "sword", "shield", "ring"
    val name: String,
    val level: Int = 1,
    val bonusValue: Double = 0.05, // e.g. 5% boost per level, or cost reduction
    val upgradeCostCoins: Double = 100.0,
    val upgradeCostWood: Double = 50.0,
    val upgradeCostStone: Double = 50.0
)
