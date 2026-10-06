package com.example.pharma.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProjectEntity::class,
        EquipmentCalculationEntity::class,
        MachineLibraryEntity::class,
        CalculationHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun calculationDao(): EquipmentCalculationDao
    abstract fun machineDao(): MachineLibraryDao
    abstract fun historyDao(): CalculationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pharma_scaleup_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val machineDao = db.machineDao()
            val projectDao = db.projectDao()

            val defaultMachines = listOf(
                // RMG
                MachineLibraryEntity(
                    manufacturer = "Diosna",
                    model = "P 1-6 (Lab RMG)",
                    equipmentType = "RMG",
                    capacityL = 4.0,
                    workingVolumeL = 2.5,
                    diameterMm = 170.0,
                    impellerDiameterMm = 170.0,
                    maxRpm = 1200.0,
                    minRpm = 100.0,
                    notes = "Laboratory benchtop high-shear granulator with interchangeable bowls"
                ),
                MachineLibraryEntity(
                    manufacturer = "Diosna",
                    model = "P 25 (Pilot RMG)",
                    equipmentType = "RMG",
                    capacityL = 25.0,
                    workingVolumeL = 16.0,
                    diameterMm = 280.0,
                    impellerDiameterMm = 280.0,
                    maxRpm = 750.0,
                    minRpm = 75.0,
                    notes = "Pilot development scale high-shear granulator"
                ),
                MachineLibraryEntity(
                    manufacturer = "Diosna",
                    model = "P 65 (Pilot RMG)",
                    equipmentType = "RMG",
                    capacityL = 65.0,
                    workingVolumeL = 42.0,
                    diameterMm = 390.0,
                    impellerDiameterMm = 390.0,
                    maxRpm = 500.0,
                    minRpm = 50.0,
                    notes = "Workbook pilot reference model (170mm -> 390mm scaling)"
                ),
                MachineLibraryEntity(
                    manufacturer = "Diosna",
                    model = "P 300 (Commercial RMG)",
                    equipmentType = "RMG",
                    capacityL = 300.0,
                    workingVolumeL = 200.0,
                    diameterMm = 680.0,
                    impellerDiameterMm = 680.0,
                    maxRpm = 280.0,
                    minRpm = 30.0,
                    notes = "Production commercial scale high shear mixer"
                ),
                MachineLibraryEntity(
                    manufacturer = "Glatt",
                    model = "VG 65",
                    equipmentType = "RMG",
                    capacityL = 65.0,
                    workingVolumeL = 40.0,
                    diameterMm = 400.0,
                    impellerDiameterMm = 400.0,
                    maxRpm = 450.0,
                    minRpm = 40.0,
                    notes = "Vertical granulator with bottom drive"
                ),

                // Blender
                MachineLibraryEntity(
                    manufacturer = "L.B. Bohle",
                    model = "LM 40 (Lab Blender)",
                    equipmentType = "BLENDER",
                    capacityL = 40.0,
                    workingVolumeL = 24.0,
                    diameterMm = 400.0,
                    maxRpm = 35.0,
                    minRpm = 5.0,
                    notes = "Tumble bin blender for pilot R&D"
                ),
                MachineLibraryEntity(
                    manufacturer = "L.B. Bohle",
                    model = "PM 400 (Pilot Bin Blender)",
                    equipmentType = "BLENDER",
                    capacityL = 400.0,
                    workingVolumeL = 240.0,
                    diameterMm = 1000.0,
                    maxRpm = 25.0,
                    minRpm = 4.0,
                    notes = "Workbook reference model D1 = 1.0 m"
                ),
                MachineLibraryEntity(
                    manufacturer = "L.B. Bohle",
                    model = "PM 1200 (Commercial Bin Blender)",
                    equipmentType = "BLENDER",
                    capacityL = 1200.0,
                    workingVolumeL = 720.0,
                    diameterMm = 2500.0,
                    maxRpm = 18.0,
                    minRpm = 3.0,
                    notes = "Workbook reference model D2 = 2.5 m"
                ),

                // FBP Top Spray
                MachineLibraryEntity(
                    manufacturer = "Glatt",
                    model = "GPCG 1 (Lab FBP)",
                    equipmentType = "FBP_TOP_SPRAY",
                    capacityL = 6.0,
                    diameterMm = 150.0,
                    notes = "Laboratory fluid bed processor with 1 top-spray nozzle"
                ),
                MachineLibraryEntity(
                    manufacturer = "Glatt",
                    model = "GPCG 15/30 (Pilot FBP)",
                    equipmentType = "FBP_TOP_SPRAY",
                    capacityL = 60.0,
                    diameterMm = 450.0,
                    notes = "Pilot scale fluid bed dryer / top spray granulator"
                ),
                MachineLibraryEntity(
                    manufacturer = "Glatt",
                    model = "PRO 120 (Commercial FBP)",
                    equipmentType = "FBP_TOP_SPRAY",
                    capacityL = 300.0,
                    diameterMm = 900.0,
                    notes = "Commercial production fluid bed dryer with multi-head nozzle array"
                ),

                // FBP Wurster
                MachineLibraryEntity(
                    manufacturer = "Glatt",
                    model = "GPCG 1 Wurster (Lab)",
                    equipmentType = "FBP_WURSTER",
                    capacityL = 4.0,
                    diameterMm = 70.0,
                    notes = "1 Wurster column, 70 mm diameter"
                ),
                MachineLibraryEntity(
                    manufacturer = "Glatt",
                    model = "GPCG 30 Wurster (Pilot)",
                    equipmentType = "FBP_WURSTER",
                    capacityL = 30.0,
                    diameterMm = 150.0,
                    notes = "1 Wurster column, 150 mm diameter"
                ),
                MachineLibraryEntity(
                    manufacturer = "Glatt",
                    model = "PRO 120 Wurster (Commercial)",
                    equipmentType = "FBP_WURSTER",
                    capacityL = 200.0,
                    diameterMm = 225.0,
                    notes = "3 Wurster columns, 225 mm diameter each"
                ),

                // Pan Coater
                MachineLibraryEntity(
                    manufacturer = "O'Hara",
                    model = "Labcoat M (Lab)",
                    equipmentType = "PAN_COATER",
                    panDiameterMm = 300.0,
                    capacityL = 3.0,
                    maxRpm = 30.0,
                    notes = "Interchangeable perforated coating pan"
                ),
                MachineLibraryEntity(
                    manufacturer = "Glatt",
                    model = "GC Smart 750 (Pilot)",
                    equipmentType = "PAN_COATER",
                    panDiameterMm = 750.0,
                    capacityL = 45.0,
                    maxRpm = 18.0,
                    notes = "Pilot tablet coater with 2 spray nozzles"
                ),
                MachineLibraryEntity(
                    manufacturer = "Thomas Processing",
                    model = "Flex 1500 (Commercial)",
                    equipmentType = "PAN_COATER",
                    panDiameterMm = 1500.0,
                    capacityL = 350.0,
                    maxRpm = 10.0,
                    notes = "Production coater with 4-gun manifold"
                ),

                // Roller Compactor
                MachineLibraryEntity(
                    manufacturer = "Alexanderwerk",
                    model = "WP 120 Pharma (Pilot)",
                    equipmentType = "ROLLER_COMPACTOR",
                    rollDiameterMm = 120.0,
                    rollWidthMm = 25.0,
                    notes = "Compact pilot dry granulator"
                ),
                MachineLibraryEntity(
                    manufacturer = "Alexanderwerk",
                    model = "WP 200 Pharma (Commercial)",
                    equipmentType = "ROLLER_COMPACTOR",
                    rollDiameterMm = 200.0,
                    rollWidthMm = 75.0,
                    notes = "Commercial dry granulation system"
                ),

                // Tablet Press
                MachineLibraryEntity(
                    manufacturer = "Fette Compacting",
                    model = "102i (R&D Press)",
                    equipmentType = "TABLET_PRESS",
                    stations = 8,
                    maxRpm = 120.0,
                    notes = "Flexible rotary tablet press for formulation R&D"
                ),
                MachineLibraryEntity(
                    manufacturer = "Korsch",
                    model = "XL 400 (Commercial Press)",
                    equipmentType = "TABLET_PRESS",
                    stations = 29,
                    maxRpm = 100.0,
                    notes = "Medium-to-large batch production rotary tablet press"
                ),
                MachineLibraryEntity(
                    manufacturer = "Fette Compacting",
                    model = "3090i (High Speed Commercial)",
                    equipmentType = "TABLET_PRESS",
                    stations = 61,
                    maxRpm = 100.0,
                    notes = "Double-sided high speed commercial tablet press"
                ),

                // Capsule Filler
                MachineLibraryEntity(
                    manufacturer = "Syntegon / Bosch",
                    model = "GKF 702 (Pilot Capsule)",
                    equipmentType = "CAPSULE_POWDER",
                    stations = 8,
                    notes = "Intermittent motion capsule filler (700 caps/min max)"
                ),
                MachineLibraryEntity(
                    manufacturer = "Syntegon / Bosch",
                    model = "GKF 1500 (Commercial Capsule)",
                    equipmentType = "CAPSULE_POWDER",
                    stations = 12,
                    notes = "Continuous high-speed capsule filling machine (1500 caps/min)"
                )
            )

            machineDao.insertAll(defaultMachines)

            // Demo Project
            val demoProject = ProjectEntity(
                projectName = "Paracetamol 500mg Tech Transfer",
                productName = "Paracetamol Rapid-Release Tablets 500 mg",
                productCode = "PARA-500-TT",
                apiName = "Paracetamol USP / Acetaminophen",
                dosageForm = "Film-Coated Tablet",
                manufacturingProcess = "Wet Granulation — High Shear",
                batchSizeKg = 150.0,
                sourceScaleName = "Lab Scale (Diosna P 1-6)",
                targetScaleName = "Commercial (Diosna P 300)",
                manufacturingSite = "Tech Transfer Site A, Facility 2",
                preparedBy = "Senior Formulation Specialist",
                date = "2026-10-05",
                comments = "DEMO DATA — Replace with actual process/equipment data. Illustrative scale-up validation study.",
                selectedEquipmentJson = "[\"RMG\", \"MULTIMILL\", \"FBP_TOP_SPRAY\", \"BLENDER\", \"TABLET_PRESS\", \"PAN_COATER\"]"
            )
            projectDao.insertProject(demoProject)
        }
    }
}
