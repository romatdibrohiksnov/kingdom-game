package com.example.data.db

import androidx.room.*
import com.example.data.model.BuildingEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.GameStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // Game State
    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    fun getGameStateFlow(): Flow<GameStateEntity?>

    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    suspend fun getGameState(): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameState(gameState: GameStateEntity)

    // Buildings
    @Query("SELECT * FROM buildings ORDER BY gridIndex ASC")
    fun getAllBuildingsFlow(): Flow<List<BuildingEntity>>

    @Query("SELECT * FROM buildings ORDER BY gridIndex ASC")
    suspend fun getAllBuildings(): List<BuildingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildings(buildings: List<BuildingEntity>)

    @Update
    suspend fun updateBuilding(building: BuildingEntity)

    // Equipment
    @Query("SELECT * FROM equipment ORDER BY id ASC")
    fun getAllEquipmentFlow(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment ORDER BY id ASC")
    suspend fun getAllEquipment(): List<EquipmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: List<EquipmentEntity>)

    @Update
    suspend fun updateEquipment(equipment: EquipmentEntity)
}
