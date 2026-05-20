package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Double = 500.0,
    val diamonds: Double = 50.0,
    val wood: Double = 200.0,
    val stone: Double = 100.0,
    val food: Double = 150.0,
    val lastActiveTime: Long = System.currentTimeMillis(),
    val unlockedUniqueBuildings: String = "", // Comma-separated list
    val promoCodesUsed: String = "" // Comma-separated list of redeemed promo codes
)
