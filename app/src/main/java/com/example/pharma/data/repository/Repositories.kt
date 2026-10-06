package com.example.pharma.data.repository

import com.example.pharma.data.db.*
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun insert(project: ProjectEntity): Long = projectDao.insertProject(project)

    suspend fun update(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun delete(project: ProjectEntity) = projectDao.deleteProject(project)

    suspend fun deleteById(id: Long) = projectDao.deleteProjectById(id)
}

class MachineRepository(private val machineDao: MachineLibraryDao) {
    val allMachines: Flow<List<MachineLibraryEntity>> = machineDao.getAllMachines()

    fun getMachinesByType(type: String): Flow<List<MachineLibraryEntity>> = machineDao.getMachinesByType(type)

    suspend fun insert(machine: MachineLibraryEntity): Long = machineDao.insertMachine(machine)

    suspend fun update(machine: MachineLibraryEntity) = machineDao.updateMachine(machine)

    suspend fun delete(machine: MachineLibraryEntity) = machineDao.deleteMachine(machine)
}

class CalculationRepository(
    private val calculationDao: EquipmentCalculationDao,
    private val historyDao: CalculationHistoryDao
) {
    val allCalculations: Flow<List<EquipmentCalculationEntity>> = calculationDao.getAllCalculations()
    val calculationHistory: Flow<List<CalculationHistoryEntity>> = historyDao.getHistory()

    fun getCalculationsForProject(projectId: Long): Flow<List<EquipmentCalculationEntity>> =
        calculationDao.getCalculationsForProject(projectId)

    suspend fun saveCalculation(calc: EquipmentCalculationEntity): Long =
        calculationDao.insertCalculation(calc)

    suspend fun addHistory(item: CalculationHistoryEntity): Long =
        historyDao.insertHistory(item)

    suspend fun deleteCalculationById(id: Long) =
        calculationDao.deleteCalculationById(id)

    suspend fun clearHistory() =
        historyDao.clearHistory()
}
