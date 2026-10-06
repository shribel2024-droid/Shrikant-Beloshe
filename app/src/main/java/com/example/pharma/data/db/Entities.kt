package com.example.pharma.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectName: String,
    val productName: String,
    val productCode: String,
    val apiName: String,
    val dosageForm: String,
    val manufacturingProcess: String,
    val batchSizeKg: Double,
    val sourceScaleName: String,
    val targetScaleName: String,
    val manufacturingSite: String,
    val preparedBy: String,
    val date: String,
    val comments: String,
    val selectedEquipmentJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "equipment_calculations")
data class EquipmentCalculationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val equipmentType: String,
    val calculationTitle: String,
    val methodology: String,
    val inputSummaryJson: String,
    val resultsJson: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "machine_library")
data class MachineLibraryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val manufacturer: String,
    val model: String,
    val equipmentType: String,
    val capacityL: Double? = null,
    val workingVolumeL: Double? = null,
    val diameterMm: Double? = null,
    val impellerDiameterMm: Double? = null,
    val maxRpm: Double? = null,
    val minRpm: Double? = null,
    val rollDiameterMm: Double? = null,
    val rollWidthMm: Double? = null,
    val panDiameterMm: Double? = null,
    val rotorDiameterMm: Double? = null,
    val screenSizeMm: Double? = null,
    val stations: Int? = null,
    val notes: String = "",
    val isCustom: Boolean = false
)

@Entity(tableName = "calculation_history")
data class CalculationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val equipmentType: String,
    val title: String,
    val scaleSummary: String,
    val keyResult: String,
    val timestamp: Long = System.currentTimeMillis()
)
