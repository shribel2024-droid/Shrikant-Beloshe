package com.example.pharma.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Dao
interface EquipmentCalculationDao {
    @Query("SELECT * FROM equipment_calculations WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun getCalculationsForProject(projectId: Long): Flow<List<EquipmentCalculationEntity>>

    @Query("SELECT * FROM equipment_calculations ORDER BY createdAt DESC")
    fun getAllCalculations(): Flow<List<EquipmentCalculationEntity>>

    @Query("SELECT * FROM equipment_calculations WHERE id = :id")
    suspend fun getCalculationById(id: Long): EquipmentCalculationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calc: EquipmentCalculationEntity): Long

    @Delete
    suspend fun deleteCalculation(calc: EquipmentCalculationEntity)

    @Query("DELETE FROM equipment_calculations WHERE id = :id")
    suspend fun deleteCalculationById(id: Long)
}

@Dao
interface MachineLibraryDao {
    @Query("SELECT * FROM machine_library ORDER BY equipmentType ASC, manufacturer ASC")
    fun getAllMachines(): Flow<List<MachineLibraryEntity>>

    @Query("SELECT * FROM machine_library WHERE equipmentType = :type ORDER BY capacityL ASC, model ASC")
    fun getMachinesByType(type: String): Flow<List<MachineLibraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachine(machine: MachineLibraryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(machines: List<MachineLibraryEntity>)

    @Update
    suspend fun updateMachine(machine: MachineLibraryEntity)

    @Delete
    suspend fun deleteMachine(machine: MachineLibraryEntity)

    @Query("SELECT COUNT(*) FROM machine_library")
    suspend fun getMachineCount(): Int
}

@Dao
interface CalculationHistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC LIMIT 50")
    fun getHistory(): Flow<List<CalculationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CalculationHistoryEntity): Long

    @Query("DELETE FROM calculation_history")
    suspend fun clearHistory()
}
