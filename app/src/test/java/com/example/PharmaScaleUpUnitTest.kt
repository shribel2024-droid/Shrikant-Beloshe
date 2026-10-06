package com.example

import com.example.pharma.domain.engine.CalculationEngine
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PharmaScaleUpUnitTest {

    @Test
    fun testRmgConstantTipSpeed_workbookExample() {
        // D1 = 170 mm, D2 = 390 mm, N1 = 500 RPM
        // N2 = 500 * 170 / 390 = 217.948... ≈ 217.9 RPM
        val input = CalculationEngine.RmgInput(
            sourceDiameterMm = 170.0,
            sourceRpm = 500.0,
            sourceBowlTotalL = 4.0,
            sourceWorkingL = 2.5,
            sourceBatchKg = 1.5,
            bulkDensityGml = 0.55,
            targets = listOf(
                CalculationEngine.RmgTargetInput("Pilot P65", 390.0, 65.0, 42.0)
            ),
            method = CalculationEngine.RmgMethod.CONSTANT_TIP_SPEED
        )
        val result = CalculationEngine.calculateRmg(input)
        val speedParam = result.parameters.find { it.name == "Impeller Speed" }
        assertNotNull(speedParam)
        val targetRpm = speedParam!!.target1Value!!
        assertTrue("Expected ~217.9 RPM, got $targetRpm", abs(targetRpm - 217.9) < 0.2)
    }

    @Test
    fun testRmgConstantFroude() {
        // N2 = N1 * sqrt(D1 / D2) = 500 * sqrt(170 / 390) = 500 * 0.65997 = 329.98 RPM
        val input = CalculationEngine.RmgInput(
            sourceDiameterMm = 170.0,
            sourceRpm = 500.0,
            sourceBowlTotalL = 4.0,
            sourceWorkingL = 2.5,
            sourceBatchKg = 1.5,
            bulkDensityGml = 0.55,
            targets = listOf(
                CalculationEngine.RmgTargetInput("Pilot P65", 390.0, 65.0, 42.0)
            ),
            method = CalculationEngine.RmgMethod.CONSTANT_FROUDE
        )
        val result = CalculationEngine.calculateRmg(input)
        val speedParam = result.parameters.find { it.name == "Impeller Speed" }
        assertNotNull(speedParam)
        val targetRpm = speedParam!!.target1Value!!
        assertTrue("Expected ~330.0 RPM under Froude, got $targetRpm", abs(targetRpm - 330.0) < 0.5)
    }

    @Test
    fun testBlenderFroudeAndBlendTime_workbookExample() {
        // D1 = 1.0 m, D2 = 2.5 m, N1 = 25 RPM, t1 = 15 min
        // Target RPM under Froude = 25 * sqrt(1.0 / 2.5) = 25 * 0.632455 = 15.811 RPM
        // Blend time t2 = (N1 * t1) / N2 = (25 * 15) / 15.811 = 375 / 15.811 = 23.717 min
        val input = CalculationEngine.BlenderInput(
            sourceVolumeL = 400.0,
            sourceWorkingL = 240.0,
            sourceDiameterM = 1.0,
            sourceRpm = 25.0,
            sourceBlendTimeMin = 15.0,
            bulkDensityGml = 0.55,
            sourceBatchKg = 120.0,
            targets = listOf(
                CalculationEngine.BlenderTargetInput("Commercial PM 1200", 1200.0, 2.5)
            ),
            method = CalculationEngine.BlenderMethod.CONSTANT_FROUDE
        )
        val result = CalculationEngine.calculateBlender(input)
        val speedParam = result.parameters.find { it.name == "Blender Speed" }
        val timeParam = result.parameters.find { it.name == "Blend Time" }
        val revsParam = result.parameters.find { it.name == "Total Revolutions" }

        assertNotNull(speedParam)
        assertNotNull(timeParam)
        assertNotNull(revsParam)

        val targetRpm = speedParam!!.target1Value!!
        val targetTime = timeParam!!.target1Value!!
        val totalRevs = revsParam!!.target1Value!!

        assertTrue("Expected ~15.81 RPM, got $targetRpm", abs(targetRpm - 15.81) < 0.1)
        assertTrue("Expected ~23.7 min, got $targetTime", abs(targetTime - 23.7) < 0.2)
        assertEquals(375.0, totalRevs, 0.01)
    }

    @Test
    fun testFbpTopSprayScreenAreaScaling() {
        // D1 = 150 mm, D2 = 450 mm -> D2/D1 = 3.0 -> SF = 3^2 = 9.0
        // Airflow2 = 120 * 9 = 1080 CFM
        val input = CalculationEngine.FbpTopSprayInput(
            sourceDiameterMm = 150.0,
            sourceBatchKg = 2.0,
            sourceAirflowCfm = 120.0,
            sourceSprayRateGpm = 20.0,
            sourceAtomizingBar = 1.5,
            targets = listOf(
                CalculationEngine.FbpTopSprayTargetInput("Pilot GPCG 30", 450.0)
            )
        )
        val result = CalculationEngine.calculateFbpTopSpray(input)
        val sfParam = result.parameters.find { it.name == "Screen Scale Factor (SF)" }
        val airflowParam = result.parameters.find { it.name == "Process Airflow" }
        val sprayParam = result.parameters.find { it.name == "Spray Rate" }

        assertEquals(9.0, sfParam!!.target1Value!!, 0.01)
        assertEquals(1080.0, airflowParam!!.target1Value!!, 0.1)
        assertEquals(180.0, sprayParam!!.target1Value!!, 0.1)
    }

    @Test
    fun testExtruderCubeAndHeatLimitedRules() {
        // D1 = 18 mm, D2 = 36 mm -> ratio = 2.0
        // Cube: 2^3 = 8.0x -> Q2 = 5 * 8 = 40 kg/h
        // Heat-limited: 2^2.5 = 5.6568x -> Q2 = 5 * 5.6568 = 28.28 kg/h
        val inputCube = CalculationEngine.ExtruderInput(
            sourceScrewDiaMm = 18.0,
            sourceThroughputKgHr = 5.0,
            sourceScrewRpm = 200.0,
            sourceProductTempC = 35.0,
            method = CalculationEngine.ExtruderMethod.CUBE_RULE,
            targets = listOf(CalculationEngine.ExtruderTargetInput("Pilot 36mm", 36.0))
        )
        val resCube = CalculationEngine.calculateExtruder(inputCube)
        val qCube = resCube.parameters.find { it.name == "Throughput Capacity" }!!.target1Value!!
        assertEquals(40.0, qCube, 0.1)

        val inputHeat = CalculationEngine.ExtruderInput(
            sourceScrewDiaMm = 18.0,
            sourceThroughputKgHr = 5.0,
            sourceScrewRpm = 200.0,
            sourceProductTempC = 35.0,
            method = CalculationEngine.ExtruderMethod.HEAT_LIMITED,
            targets = listOf(CalculationEngine.ExtruderTargetInput("Pilot 36mm", 36.0))
        )
        val resHeat = CalculationEngine.calculateExtruder(inputHeat)
        val qHeat = resHeat.parameters.find { it.name == "Throughput Capacity" }!!.target1Value!!
        assertTrue("Expected ~28.28 kg/h, got $qHeat", abs(qHeat - 28.28) < 0.2)
    }

    @Test
    fun testRollerCompactorSpecificCompactionForce() {
        // Force = 20 kN, Width = 25 mm = 2.5 cm -> SCF = 20 / 2.5 = 8.0 kN/cm
        // Target Width = 75 mm = 7.5 cm -> Target Force = 8.0 * 7.5 = 60.0 kN
        val input = CalculationEngine.RollerCompactorInput(
            sourceRollDiaMm = 120.0,
            sourceRollWidthMm = 25.0,
            sourceRollForceKn = 20.0,
            sourceRollGapMm = 2.5,
            sourceRollSpeedRpm = 6.0,
            sourceScrewSpeedRpm = 30.0,
            sourceThroughputKgHr = 15.0,
            targets = listOf(CalculationEngine.RollerCompactorTargetInput("WP 200", 200.0, 75.0))
        )
        val result = CalculationEngine.calculateRollerCompactor(input)
        val scfParam = result.parameters.find { it.name == "Specific Compaction Force (SCF)" }
        val targetForceParam = result.parameters.find { it.name == "Total Roll Force" }

        assertEquals(8.0, scfParam!!.target1Value!!, 0.01)
        assertEquals(60.0, targetForceParam!!.target1Value!!, 0.01)
    }

    @Test
    fun testTabletCompressionOutput() {
        // Stations = 29, Turret RPM = 60, tabs_per_rev = 1
        // Theo output/hr = 29 * 60 * 60 = 104,400 tablets/hr
        val input = CalculationEngine.TabletPressInput(
            sourceStations = 8,
            sourceTurretRpm = 40.0,
            targetTabletWeightMg = 500.0,
            batchSizeTablets = 500000.0,
            machineEfficiencyPct = 100.0,
            turretUtilizationPct = 100.0,
            rejectPct = 0.0,
            targets = listOf(CalculationEngine.TabletPressTargetInput("Korsch XL 400", 29, 60.0))
        )
        val result = CalculationEngine.calculateTabletPress(input)
        val theoParam = result.parameters.find { it.name == "Theoretical Output" }
        assertEquals(104400.0, theoParam!!.target1Value!!, 1.0)
    }

    @Test
    fun testCapsuleFillerOutput() {
        // Stations = 12, Speed = 1200 cycles/min -> Theo = 1200 * 12 * 60 = 864,000 caps/hr
        val input = CalculationEngine.CapsuleFillerInput(
            isPelletFill = false,
            targetFillWeightMg = 300.0,
            sourceStations = 8,
            sourceSpeedCpm = 400.0,
            sourceBatchCapsules = 1000000.0,
            efficiencyPct = 100.0,
            targets = listOf(CalculationEngine.CapsuleFillerTargetInput("GKF 1500", 12, 1200.0))
        )
        val result = CalculationEngine.calculateCapsuleFiller(input)
        val theoParam = result.parameters.find { it.name == "Theoretical Output" }
        assertEquals(864000.0, theoParam!!.target1Value!!, 1.0)
    }
}
