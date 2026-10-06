package com.example.pharma.domain.engine

import com.example.pharma.domain.model.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

object CalculationEngine {

    private fun formatNum(value: Double, decimals: Int = 2): String {
        return if (value.isNaN() || value.isInfinite()) "N/A"
        else String.format(Locale.US, "%.${decimals}f", value)
    }

    // ==========================================
    // 1. RAPID MIXER GRANULATOR (RMG)
    // ==========================================
    data class RmgInput(
        val sourceDiameterMm: Double,
        val sourceRpm: Double,
        val sourceBowlTotalL: Double,
        val sourceWorkingL: Double,
        val sourceBatchKg: Double,
        val bulkDensityGml: Double,
        val sourceMaxRpm: Double = 600.0,
        val targets: List<RmgTargetInput>,
        val method: RmgMethod = RmgMethod.CONSTANT_TIP_SPEED
    )

    data class RmgTargetInput(
        val name: String,
        val diameterMm: Double,
        val bowlTotalL: Double,
        val workingL: Double,
        val targetBatchKg: Double? = null,
        val maxRpm: Double = 500.0
    )

    enum class RmgMethod(val displayName: String) {
        CONSTANT_TIP_SPEED("Constant Tip Speed (N2 = N1 * D1/D2)"),
        CONSTANT_FROUDE("Constant Froude Number (N2 = N1 * sqrt(D1/D2))")
    }

    fun calculateRmg(input: RmgInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        assumptions.add("Granule density and rheological behavior remain consistent across scales.")
        assumptions.add("Impeller blade geometry and vessel aspect ratio (H/D) are geometrically similar.")
        assumptions.add("Chopper speed is typically scaled proportionally to maintain relative shear intensity.")

        engineeringNotes.add("Constant Tip Speed tends to over-densify granules at commercial scale; Constant Froude is widely preferred for fragile formulations.")
        engineeringNotes.add("Liquid addition time should be kept relatively constant (~2-5 min) to maintain droplet distribution.")

        // Source calculations
        val d1M = input.sourceDiameterMm / 1000.0
        val tipSpeed1 = (PI * d1M * input.sourceRpm) / 60.0
        val froude1 = ((input.sourceRpm / 60.0).pow(2) * d1M) / 9.81
        val sourceFillRatio = if (input.sourceWorkingL > 0 && input.bulkDensityGml > 0) {
            val volumeUsed = input.sourceBatchKg / input.bulkDensityGml
            (volumeUsed / input.sourceBowlTotalL) * 100.0
        } else {
            0.0
        }

        if (sourceFillRatio < 30.0 || sourceFillRatio > 70.0) {
            warnings.add(
                CalculationWarning(
                    level = ValidationStatus.CAUTION,
                    message = "Source bowl fill ratio (${formatNum(sourceFillRatio, 1)}%) is outside optimal 30-70% range.",
                    parameterName = "Fill Ratio",
                    recommendation = "Maintain 35-65% filling to avoid unmixed dead zones or excessive motor load."
                )
            )
        }

        val targetResults = input.targets.map { target ->
            val d2M = target.diameterMm / 1000.0
            val dRatio = if (input.sourceDiameterMm > 0) target.diameterMm / input.sourceDiameterMm else 1.0

            val targetRpm = when (input.method) {
                RmgMethod.CONSTANT_TIP_SPEED -> {
                    if (target.diameterMm > 0) input.sourceRpm * (input.sourceDiameterMm / target.diameterMm) else 0.0
                }
                RmgMethod.CONSTANT_FROUDE -> {
                    if (target.diameterMm > 0) input.sourceRpm * sqrt(input.sourceDiameterMm / target.diameterMm) else 0.0
                }
            }

            val tipSpeed2 = (PI * d2M * targetRpm) / 60.0
            val froude2 = ((targetRpm / 60.0).pow(2) * d2M) / 9.81
            val scaledBatchKg = target.targetBatchKg ?: (input.sourceBatchKg * (target.bowlTotalL / input.sourceBowlTotalL))
            val targetFillRatio = if (target.bowlTotalL > 0 && input.bulkDensityGml > 0) {
                val volUsed = scaledBatchKg / input.bulkDensityGml
                (volUsed / target.bowlTotalL) * 100.0
            } else 0.0

            if (targetRpm > target.maxRpm) {
                warnings.add(
                    CalculationWarning(
                        level = ValidationStatus.INVALID,
                        message = "${target.name}: Target RPM (${formatNum(targetRpm, 1)}) exceeds machine max limit (${target.maxRpm} RPM).",
                        parameterName = "Impeller Speed",
                        recommendation = "Select Constant Froude number or larger impeller diameter, or utilize lower gear."
                    )
                )
            }

            if (targetFillRatio < 30.0 || targetFillRatio > 70.0) {
                warnings.add(
                    CalculationWarning(
                        level = ValidationStatus.CAUTION,
                        message = "${target.name}: Fill ratio (${formatNum(targetFillRatio, 1)}%) is outside recommended 30-70% range.",
                        parameterName = "Fill Ratio",
                        recommendation = "Adjust batch size to stay within 40-65% working capacity."
                    )
                )
            }

            Triple(targetRpm, tipSpeed2, targetFillRatio to (froude2 to scaledBatchKg))
        }

        val t1 = targetResults.getOrNull(0)
        val t2 = targetResults.getOrNull(1)
        val t3 = targetResults.getOrNull(2)

        val methodologyType = if (input.method == RmgMethod.CONSTANT_FROUDE) MethodologyType.REFERENCE else MethodologyType.STANDARD_PRACTICE
        val equationStr = if (input.method == RmgMethod.CONSTANT_FROUDE) "N2 = N1 * sqrt(D1 / D2)" else "N2 = N1 * (D1 / D2)"
        val substitutedStr = if (input.targets.isNotEmpty()) {
            val d1 = formatNum(input.sourceDiameterMm, 1)
            val d2 = formatNum(input.targets[0].diameterMm, 1)
            val n1 = formatNum(input.sourceRpm, 1)
            if (input.method == RmgMethod.CONSTANT_FROUDE) {
                "N2 = $n1 * sqrt($d1 / $d2) = ${formatNum(t1?.first ?: 0.0, 1)} RPM"
            } else {
                "N2 = $n1 * ($d1 / $d2) = ${formatNum(t1?.first ?: 0.0, 1)} RPM"
            }
        } else "N/A"

        val formula = FormulaDetail(
            formulaId = "RMG-01",
            title = "RMG Impeller Speed Scale-Up",
            equationText = equationStr,
            variablesDescription = mapOf(
                "N1" to "Source impeller rotational speed (RPM)",
                "N2" to "Target impeller rotational speed (RPM)",
                "D1" to "Source impeller diameter (mm)",
                "D2" to "Target impeller diameter (mm)",
                "Fr" to "Froude number = N^2 * D / g (inertia vs gravity)"
            ),
            substitutedText = substitutedStr,
            calculatedResultText = "${formatNum(t1?.first ?: 0.0, 1)} RPM",
            methodologyType = methodologyType,
            engineeringNotes = "Constant Froude ensures comparable vortex formation and flow pattern. Constant tip speed matches maximum shear at the blade edge."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Impeller Speed",
                unit = "RPM",
                sourceValue = input.sourceRpm,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                scaleFactor1 = if (input.sourceRpm > 0 && t1 != null) t1.first / input.sourceRpm else null,
                scaleFactor2 = if (input.sourceRpm > 0 && t2 != null) t2.first / input.sourceRpm else null,
                scaleFactor3 = if (input.sourceRpm > 0 && t3 != null) t3.first / input.sourceRpm else null,
                formulaDetail = formula,
                status = if (warnings.any { it.parameterName == "Impeller Speed" }) ValidationStatus.CAUTION else ValidationStatus.VALID
            ),
            ScaleParameter(
                name = "Impeller Diameter",
                unit = "mm",
                sourceValue = input.sourceDiameterMm,
                target1Value = input.targets.getOrNull(0)?.diameterMm,
                target2Value = input.targets.getOrNull(1)?.diameterMm,
                target3Value = input.targets.getOrNull(2)?.diameterMm,
                scaleFactor1 = input.targets.getOrNull(0)?.let { it.diameterMm / input.sourceDiameterMm }
            ),
            ScaleParameter(
                name = "Tip Speed",
                unit = "m/s",
                sourceValue = tipSpeed1,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second
            ),
            ScaleParameter(
                name = "Froude Number (Fr)",
                unit = "-",
                sourceValue = froude1,
                target1Value = t1?.third?.second?.first,
                target2Value = t2?.third?.second?.first,
                target3Value = t3?.third?.second?.first
            ),
            ScaleParameter(
                name = "Batch Size",
                unit = "kg",
                sourceValue = input.sourceBatchKg,
                target1Value = t1?.third?.second?.second,
                target2Value = t2?.third?.second?.second,
                target3Value = t3?.third?.second?.second,
                scaleFactor1 = t1?.third?.second?.second?.let { it / input.sourceBatchKg }
            ),
            ScaleParameter(
                name = "Bowl Fill Ratio",
                unit = "%",
                sourceValue = sourceFillRatio,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first,
                status = if (warnings.any { it.parameterName == "Fill Ratio" }) ValidationStatus.CAUTION else ValidationStatus.VALID
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.RMG,
            methodologyName = input.method.displayName,
            methodologyType = methodologyType,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 2. BLENDER MODULE
    // ==========================================
    data class BlenderInput(
        val blenderType: String = "V-Blender / Bin Blender",
        val sourceVolumeL: Double,
        val sourceWorkingL: Double,
        val sourceDiameterM: Double,
        val sourceRpm: Double,
        val sourceBlendTimeMin: Double,
        val bulkDensityGml: Double,
        val sourceBatchKg: Double,
        val targets: List<BlenderTargetInput>,
        val method: BlenderMethod = BlenderMethod.CONSTANT_FROUDE
    )

    data class BlenderTargetInput(
        val name: String,
        val volumeL: Double,
        val diameterM: Double,
        val targetBatchKg: Double? = null,
        val maxRpm: Double = 30.0
    )

    enum class BlenderMethod(val displayName: String) {
        CONSTANT_FROUDE("Constant Froude Number (N2 = N1 * sqrt(D1/D2))"),
        CONSTANT_TIP_SPEED("Constant Tip Speed (N2 = N1 * D1/D2)")
    }

    fun calculateBlender(input: BlenderInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        assumptions.add("Equal total revolutions (N1 * t1 = N2 * t2) produces equivalent degree of particle mixing.")
        assumptions.add("Powder bed fill ratio is held consistent between 50% and 60% of total vessel capacity.")

        engineeringNotes.add("Total blend revolutions is the primary scale-up invariant for tumble blenders. Do not overblend lubricant (Magnesium Stearate) to prevent dissolution retardation.")
        engineeringNotes.add("Froude number scaling preserves relative gravitational tumbling regime: Fr = (omega^2 * R) / g.")

        val sourceTotalRevs = input.sourceRpm * input.sourceBlendTimeMin
        val sourceFillRatio = if (input.sourceVolumeL > 0 && input.bulkDensityGml > 0) {
            ((input.sourceBatchKg / input.bulkDensityGml) / input.sourceVolumeL) * 100.0
        } else 55.0

        if (sourceFillRatio < 40.0 || sourceFillRatio > 70.0) {
            warnings.add(
                CalculationWarning(
                    level = ValidationStatus.CAUTION,
                    message = "Source blender fill ratio (${formatNum(sourceFillRatio, 1)}%) deviates from optimal 50-60%.",
                    parameterName = "Fill Ratio",
                    recommendation = "50-60% fill promotes optimum axial and cross-flow cascading."
                )
            )
        }

        val targetResults = input.targets.map { target ->
            val targetRpm = when (input.method) {
                BlenderMethod.CONSTANT_FROUDE -> {
                    if (target.diameterM > 0) input.sourceRpm * sqrt(input.sourceDiameterM / target.diameterM) else 0.0
                }
                BlenderMethod.CONSTANT_TIP_SPEED -> {
                    if (target.diameterM > 0) input.sourceRpm * (input.sourceDiameterM / target.diameterM) else 0.0
                }
            }

            val targetTimeMin = if (targetRpm > 0) sourceTotalRevs / targetRpm else 0.0
            val targetBatchKg = target.targetBatchKg ?: (input.sourceBatchKg * (target.volumeL / input.sourceVolumeL))
            val targetFill = if (target.volumeL > 0 && input.bulkDensityGml > 0) {
                ((targetBatchKg / input.bulkDensityGml) / target.volumeL) * 100.0
            } else 55.0

            if (targetRpm > target.maxRpm) {
                warnings.add(
                    CalculationWarning(
                        level = ValidationStatus.INVALID,
                        message = "${target.name}: Target speed (${formatNum(targetRpm, 1)} RPM) exceeds blender safety rating (${target.maxRpm} RPM).",
                        parameterName = "Blender RPM",
                        recommendation = "Operate at safe maximum RPM and extend blending time to achieve target total revolutions."
                    )
                )
            }

            Triple(targetRpm, targetTimeMin, targetFill to targetBatchKg)
        }

        val t1 = targetResults.getOrNull(0)
        val t2 = targetResults.getOrNull(1)
        val t3 = targetResults.getOrNull(2)

        val formulaRpm = FormulaDetail(
            formulaId = "BLEND-01",
            title = "Blender Speed & Time Scale-Up",
            equationText = "N2 = N1 * sqrt(D1 / D2); t2 = (N1 * t1) / N2",
            variablesDescription = mapOf(
                "N1" to "Source rotational speed (RPM)",
                "N2" to "Target rotational speed (RPM)",
                "D1" to "Source blender characteristic diameter (m)",
                "D2" to "Target blender characteristic diameter (m)",
                "t1" to "Source blending duration (min)",
                "t2" to "Target blending duration (min)"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val d1 = formatNum(input.sourceDiameterM, 2)
                val d2 = formatNum(input.targets[0].diameterM, 2)
                "N2 = ${formatNum(input.sourceRpm, 1)} * sqrt($d1 / $d2) = ${formatNum(t1?.first ?: 0.0, 2)} RPM; t2 = (${formatNum(input.sourceRpm, 1)} * ${formatNum(input.sourceBlendTimeMin, 1)}) / ${formatNum(t1?.first ?: 0.0, 2)} = ${formatNum(t1?.second ?: 0.0, 1)} min"
            } else "N/A",
            calculatedResultText = "${formatNum(t1?.first ?: 0.0, 2)} RPM, ${formatNum(t1?.second ?: 0.0, 1)} min",
            methodologyType = MethodologyType.REFERENCE,
            engineeringNotes = "Maintains constant total revolutions = ${formatNum(sourceTotalRevs, 0)} revs."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Blender Speed",
                unit = "RPM",
                sourceValue = input.sourceRpm,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                scaleFactor1 = if (input.sourceRpm > 0 && t1 != null) t1.first / input.sourceRpm else null,
                formulaDetail = formulaRpm
            ),
            ScaleParameter(
                name = "Blend Time",
                unit = "min",
                sourceValue = input.sourceBlendTimeMin,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second
            ),
            ScaleParameter(
                name = "Total Revolutions",
                unit = "revs",
                sourceValue = sourceTotalRevs,
                target1Value = sourceTotalRevs,
                target2Value = sourceTotalRevs,
                target3Value = sourceTotalRevs
            ),
            ScaleParameter(
                name = "Batch Size",
                unit = "kg",
                sourceValue = input.sourceBatchKg,
                target1Value = t1?.third?.second,
                target2Value = t2?.third?.second,
                target3Value = t3?.third?.second
            ),
            ScaleParameter(
                name = "Fill Ratio",
                unit = "%",
                sourceValue = sourceFillRatio,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.BLENDER,
            methodologyName = input.method.displayName,
            methodologyType = MethodologyType.REFERENCE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 3. FLUID BED PROCESSOR — TOP SPRAY (FBP)
    // ==========================================
    data class FbpTopSprayInput(
        val sourceDiameterMm: Double,
        val sourceBatchKg: Double,
        val sourceAirflowCfm: Double,
        val sourceSprayRateGpm: Double,
        val sourceAtomizingBar: Double,
        val sourceNozzles: Int = 1,
        val targets: List<FbpTopSprayTargetInput>
    )

    data class FbpTopSprayTargetInput(
        val name: String,
        val diameterMm: Double,
        val targetBatchKg: Double? = null,
        val targetNozzles: Int = 1
    )

    fun calculateFbpTopSpray(input: FbpTopSprayInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        assumptions.add("Air superficial velocity (U = Airflow / Distributor Screen Area) remains constant.")
        assumptions.add("Evaporation capacity scales proportionally with distributor plate cross-sectional area.")
        assumptions.add("Spray droplet size distribution (D50) is maintained by preserving liquid-to-atomizing air mass ratio.")

        engineeringNotes.add("Scale Factor (SF) = (D2 / D1)^2 based on bottom distributor screen area.")
        engineeringNotes.add("Inlet air temperature and absolute humidity should be controlled identically at all scales.")

        val tResults = input.targets.map { target ->
            val sf = if (input.sourceDiameterMm > 0) (target.diameterMm / input.sourceDiameterMm).pow(2) else 1.0
            val targetAirflow = input.sourceAirflowCfm * sf
            val targetSprayRate = input.sourceSprayRateGpm * sf
            val targetBatchKg = target.targetBatchKg ?: (input.sourceBatchKg * sf)
            val sprayPerNozzle = if (target.targetNozzles > 0) targetSprayRate / target.targetNozzles else targetSprayRate

            if (targetAirflow > 5000.0) {
                warnings.add(
                    CalculationWarning(
                        level = ValidationStatus.CAUTION,
                        message = "${target.name}: Airflow demand (${formatNum(targetAirflow, 0)} CFM / m3/h) requires high blower capacity verification.",
                        parameterName = "Process Airflow"
                    )
                )
            }

            Triple(sf, targetAirflow, Pair(targetSprayRate, Pair(sprayPerNozzle, targetBatchKg)))
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = "FBP-TS-01",
            title = "Top Spray Airflow & Spray Rate Scale-Up",
            equationText = "SF = (D2 / D1)^2; Airflow2 = Airflow1 * SF; SprayRate2 = SprayRate1 * SF",
            variablesDescription = mapOf(
                "D1" to "Source distributor screen diameter (mm)",
                "D2" to "Target distributor screen diameter (mm)",
                "SF" to "Screen area scale-up factor",
                "Airflow" to "Inlet fluidization air volume flow (CFM or m3/h)",
                "SprayRate" to "Granulation liquid binder delivery rate (g/min)"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val d1 = formatNum(input.sourceDiameterMm, 1)
                val d2 = formatNum(input.targets[0].diameterMm, 1)
                val sfStr = formatNum(t1?.first ?: 0.0, 3)
                "SF = ($d2 / $d1)^2 = $sfStr; Airflow2 = ${formatNum(input.sourceAirflowCfm, 1)} * $sfStr = ${formatNum(t1?.second ?: 0.0, 1)}; SprayRate2 = ${formatNum(input.sourceSprayRateGpm, 1)} * $sfStr = ${formatNum(t1?.third?.first ?: 0.0, 1)} g/min"
            } else "N/A",
            calculatedResultText = "Airflow: ${formatNum(t1?.second ?: 0.0, 1)}, Spray: ${formatNum(t1?.third?.first ?: 0.0, 1)} g/min",
            methodologyType = MethodologyType.REFERENCE,
            engineeringNotes = "Constant superficial fluidization velocity maintains identical fluidization bed height and bed density."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Screen Scale Factor (SF)",
                unit = "ratio",
                sourceValue = 1.0,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Process Airflow",
                unit = "CFM / m³/h",
                sourceValue = input.sourceAirflowCfm,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second,
                scaleFactor1 = t1?.first
            ),
            ScaleParameter(
                name = "Spray Rate",
                unit = "g/min",
                sourceValue = input.sourceSprayRateGpm,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first,
                scaleFactor1 = t1?.first
            ),
            ScaleParameter(
                name = "Spray Rate Per Nozzle",
                unit = "g/min/nozzle",
                sourceValue = if (input.sourceNozzles > 0) input.sourceSprayRateGpm / input.sourceNozzles else input.sourceSprayRateGpm,
                target1Value = t1?.third?.second?.first,
                target2Value = t2?.third?.second?.first,
                target3Value = t3?.third?.second?.first
            ),
            ScaleParameter(
                name = "Atomizing Air Pressure",
                unit = "bar",
                sourceValue = input.sourceAtomizingBar,
                target1Value = input.sourceAtomizingBar,
                target2Value = input.sourceAtomizingBar,
                target3Value = input.sourceAtomizingBar,
                notes = "Hold constant unless droplet size measurement indicates adjustment"
            ),
            ScaleParameter(
                name = "Batch Size",
                unit = "kg",
                sourceValue = input.sourceBatchKg,
                target1Value = t1?.third?.second?.second,
                target2Value = t2?.third?.second?.second,
                target3Value = t3?.third?.second?.second
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.FBP_TOP_SPRAY,
            methodologyName = "Screen-Area Superficial Velocity Scaling",
            methodologyType = MethodologyType.REFERENCE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 4. FBP BOTTOM SPRAY / WURSTER
    // ==========================================
    data class FbpWursterInput(
        val sourceColumns: Int,
        val sourceColumnDiaMm: Double,
        val sourceAirflowCfm: Double,
        val sourceSprayRateGpm: Double,
        val sourcePartitionGapMm: Double,
        val targets: List<FbpWursterTargetInput>
    )

    data class FbpWursterTargetInput(
        val name: String,
        val columns: Int,
        val columnDiaMm: Double,
        val vendorOverridePartitionGapMm: Double? = null
    )

    fun calculateFbpWurster(input: FbpWursterInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        assumptions.add("Air velocity inside Wurster partition tube is maintained constant to avoid pellet attrition or premature falling.")
        assumptions.add("Scale factor is strictly determined by ratio of total Wurster column cross-sectional area.")
        assumptions.add("Partition gap scaling is proportional to column diameter unless vendor specifies fixed clearance.")

        engineeringNotes.add("In multi-tube Wurster systems (e.g. 3-tube, 7-tube commercial units), each tube behaves as an individual coater with dedicated spray nozzle.")
        engineeringNotes.add("Wurster partition gap strongly influences pellet circulation rate: too small causes choking, too large causes spray drying.")

        val sourceTotalArea = input.sourceColumns * (PI / 4.0) * (input.sourceColumnDiaMm).pow(2)

        val tResults = input.targets.map { target ->
            val targetTotalArea = target.columns * (PI / 4.0) * (target.columnDiaMm).pow(2)
            val sf = if (sourceTotalArea > 0) targetTotalArea / sourceTotalArea else 1.0
            val targetAirflow = input.sourceAirflowCfm * sf
            val targetSprayRate = input.sourceSprayRateGpm * sf
            val calculatedGap = input.sourcePartitionGapMm * (target.columnDiaMm / input.sourceColumnDiaMm)
            val finalGap = target.vendorOverridePartitionGapMm ?: calculatedGap

            if (target.vendorOverridePartitionGapMm != null) {
                assumptions.add("${target.name}: Using vendor-confirmed partition gap override (${target.vendorOverridePartitionGapMm} mm).")
            }

            Triple(sf, targetAirflow, targetSprayRate to finalGap)
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = "FBP-WUR-01",
            title = "Wurster Column Area Scale-Up",
            equationText = "SF = Target Total Col Area / Source Total Col Area; Q2 = Q1 * SF; Spray2 = Spray1 * SF",
            variablesDescription = mapOf(
                "Source Area" to "N1 * (pi/4) * Dcol1^2",
                "Target Area" to "N2 * (pi/4) * Dcol2^2",
                "SF" to "Total Wurster tube area ratio"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                "SF = ${formatNum(t1?.first ?: 1.0, 3)}; Target Airflow = ${formatNum(input.sourceAirflowCfm, 1)} * ${formatNum(t1?.first ?: 1.0, 3)} = ${formatNum(t1?.second ?: 0.0, 1)}"
            } else "N/A",
            calculatedResultText = "Scale factor: ${formatNum(t1?.first ?: 1.0, 3)}",
            methodologyType = MethodologyType.REFERENCE,
            engineeringNotes = "Valid for single-tube to multi-tube Wurster scale-up."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Wurster Area Scale Factor",
                unit = "ratio",
                sourceValue = 1.0,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Process Airflow",
                unit = "CFM / m³/h",
                sourceValue = input.sourceAirflowCfm,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second
            ),
            ScaleParameter(
                name = "Total Spray Rate",
                unit = "g/min",
                sourceValue = input.sourceSprayRateGpm,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first
            ),
            ScaleParameter(
                name = "Partition Gap",
                unit = "mm",
                sourceValue = input.sourcePartitionGapMm,
                target1Value = t1?.third?.second,
                target2Value = t2?.third?.second,
                target3Value = t3?.third?.second,
                notes = if (input.targets.getOrNull(0)?.vendorOverridePartitionGapMm != null) "Vendor override applied" else "Estimated proportional gap"
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.FBP_WURSTER,
            methodologyName = "Wurster Column Area Scaling",
            methodologyType = MethodologyType.REFERENCE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 5. EXTRUDER
    // ==========================================
    data class ExtruderInput(
        val sourceScrewDiaMm: Double,
        val sourceThroughputKgHr: Double,
        val sourceScrewRpm: Double,
        val sourceProductTempC: Double,
        val method: ExtruderMethod = ExtruderMethod.HEAT_LIMITED,
        val customExponent: Double = 2.5,
        val targets: List<ExtruderTargetInput>
    )

    data class ExtruderTargetInput(
        val name: String,
        val screwDiaMm: Double,
        val screwRpm: Double? = null
    )

    enum class ExtruderMethod(val displayName: String, val exponent: Double) {
        CUBE_RULE("Volumetric / Cube Rule (n = 3.0)", 3.0),
        HEAT_LIMITED("Heat-Transfer Limited Rule (n = 2.5)", 2.5),
        CUSTOM("Custom Exponent Rule (Q2 = Q1 * (D2/D1)^n)", 2.5)
    }

    fun calculateExtruder(input: ExtruderInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        val exp = if (input.method == ExtruderMethod.CUSTOM) input.customExponent else input.method.exponent

        assumptions.add("Methodology: ${input.method.displayName} with exponent n = $exp.")
        assumptions.add("L/D (Length to Diameter) ratio is maintained identical across extruder barrels.")
        assumptions.add("Screw configuration geometry (kneading block stagger, pitch angle) is geometrically replicated.")

        engineeringNotes.add("Cube rule (n=3) assumes pure drag-flow volumetric scaling without thermal dissipation constraints.")
        engineeringNotes.add("Heat-transfer limited rule (n=2.5) prevents thermal degradation of temperature-sensitive APIs by accounting for reduced surface-area-to-volume ratio.")

        val tResults = input.targets.map { target ->
            val dRatio = if (input.sourceScrewDiaMm > 0) target.screwDiaMm / input.sourceScrewDiaMm else 1.0
            val targetThroughput = input.sourceThroughputKgHr * dRatio.pow(exp)
            val targetRpm = target.screwRpm ?: (input.sourceScrewRpm * (input.sourceScrewDiaMm / target.screwDiaMm))

            Pair(dRatio.pow(exp), targetThroughput to targetRpm)
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = "EXT-01",
            title = "Extruder Throughput Scale-Up",
            equationText = "Q2 = Q1 * (D2 / D1)^$exp",
            variablesDescription = mapOf(
                "Q1" to "Source throughput capacity (kg/h)",
                "Q2" to "Target throughput capacity (kg/h)",
                "D1" to "Source screw diameter (mm)",
                "D2" to "Target screw diameter (mm)",
                "n" to "Scale-up exponent ($exp)"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val d1 = formatNum(input.sourceScrewDiaMm, 1)
                val d2 = formatNum(input.targets[0].screwDiaMm, 1)
                "Q2 = ${formatNum(input.sourceThroughputKgHr, 1)} * ($d2 / $d1)^$exp = ${formatNum(t1?.second?.first ?: 0.0, 2)} kg/h"
            } else "N/A",
            calculatedResultText = "${formatNum(t1?.second?.first ?: 0.0, 2)} kg/h",
            methodologyType = if (input.method == ExtruderMethod.CUSTOM) MethodologyType.CUSTOM else MethodologyType.STANDARD_PRACTICE,
            engineeringNotes = "Verify barrel chiller cooling capacity at commercial scale."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Throughput Capacity",
                unit = "kg/h",
                sourceValue = input.sourceThroughputKgHr,
                target1Value = t1?.second?.first,
                target2Value = t2?.second?.first,
                target3Value = t3?.second?.first,
                scaleFactor1 = t1?.first,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Screw Speed",
                unit = "RPM",
                sourceValue = input.sourceScrewRpm,
                target1Value = t1?.second?.second,
                target2Value = t2?.second?.second,
                target3Value = t3?.second?.second
            ),
            ScaleParameter(
                name = "Screw Diameter Ratio (D2/D1)",
                unit = "-",
                sourceValue = 1.0,
                target1Value = input.targets.getOrNull(0)?.let { it.screwDiaMm / input.sourceScrewDiaMm },
                target2Value = input.targets.getOrNull(1)?.let { it.screwDiaMm / input.sourceScrewDiaMm },
                target3Value = input.targets.getOrNull(2)?.let { it.screwDiaMm / input.sourceScrewDiaMm }
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.EXTRUDER,
            methodologyName = input.method.displayName,
            methodologyType = if (input.method == ExtruderMethod.CUSTOM) MethodologyType.CUSTOM else MethodologyType.STANDARD_PRACTICE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 6. SPHERONIZER
    // ==========================================
    data class SpheronizerInput(
        val sourcePlateDiaMm: Double,
        val sourceRpm: Double,
        val sourceBatchLoadKg: Double,
        val targets: List<SpheronizerTargetInput>
    )

    data class SpheronizerTargetInput(
        val name: String,
        val plateDiaMm: Double
    )

    fun calculateSpheronizer(input: SpheronizerInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        assumptions.add("Constant peripheral plate tip speed preserves centrifugal pellet tumbling velocity.")
        assumptions.add("Batch load scales proportionally with friction plate surface area: Load2 = Load1 * (D2/D1)^2.")

        engineeringNotes.add("Spheronization time (typically 1-5 minutes) is kept nearly constant between laboratory and commercial scale.")
        engineeringNotes.add("Friction plate cross-hatch groove geometry (e.g. 2mm or 3mm pitch) should match pellet target diameter.")

        val tipSpeed1 = (PI * (input.sourcePlateDiaMm / 1000.0) * input.sourceRpm) / 60.0

        val tResults = input.targets.map { target ->
            val dRatio = if (input.sourcePlateDiaMm > 0) target.plateDiaMm / input.sourcePlateDiaMm else 1.0
            val targetRpm = if (target.plateDiaMm > 0) input.sourceRpm * (input.sourcePlateDiaMm / target.plateDiaMm) else 0.0
            val targetLoadKg = input.sourceBatchLoadKg * dRatio.pow(2)
            val tipSpeed2 = (PI * (target.plateDiaMm / 1000.0) * targetRpm) / 60.0

            Triple(targetRpm, targetLoadKg, tipSpeed2 to dRatio.pow(2))
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = "SPH-01",
            title = "Spheronizer Speed & Load Scale-Up",
            equationText = "N2 = N1 * (D1 / D2); Load2 = Load1 * (D2 / D1)^2",
            variablesDescription = mapOf(
                "N1" to "Source plate speed (RPM)",
                "N2" to "Target plate speed (RPM)",
                "D1" to "Source plate diameter (mm)",
                "D2" to "Target plate diameter (mm)",
                "Load" to "Friction plate batch charge capacity (kg)"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val d1 = formatNum(input.sourcePlateDiaMm, 1)
                val d2 = formatNum(input.targets[0].plateDiaMm, 1)
                "N2 = ${formatNum(input.sourceRpm, 1)} * ($d1 / $d2) = ${formatNum(t1?.first ?: 0.0, 1)} RPM; Load2 = ${formatNum(input.sourceBatchLoadKg, 2)} * ($d2 / $d1)^2 = ${formatNum(t1?.second ?: 0.0, 2)} kg"
            } else "N/A",
            calculatedResultText = "${formatNum(t1?.first ?: 0.0, 1)} RPM, ${formatNum(t1?.second ?: 0.0, 2)} kg",
            methodologyType = MethodologyType.REFERENCE,
            engineeringNotes = "Plate area scaling prevents pellet overburdening and agglomeration."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Plate Rotational Speed",
                unit = "RPM",
                sourceValue = input.sourceRpm,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Batch Load Capacity",
                unit = "kg",
                sourceValue = input.sourceBatchLoadKg,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second,
                scaleFactor1 = t1?.third?.second
            ),
            ScaleParameter(
                name = "Peripheral Plate Tip Speed",
                unit = "m/s",
                sourceValue = tipSpeed1,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.SPHERONIZER,
            methodologyName = "Constant Tip Speed & Area-Based Load Scaling",
            methodologyType = MethodologyType.REFERENCE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 7. PAN COATER
    // ==========================================
    data class PanCoaterInput(
        val sourcePanDiaMm: Double,
        val sourcePanRpm: Double,
        val sourceBatchKg: Double,
        val sourceSprayRateGpm: Double,
        val sourceExhaustAirflowCfm: Double,
        val sprayMethod: PanCoaterSprayMethod = PanCoaterSprayMethod.SURFACE_AREA_2_3,
        val targets: List<PanCoaterTargetInput>
    )

    data class PanCoaterTargetInput(
        val name: String,
        val panDiaMm: Double,
        val targetBatchKg: Double? = null
    )

    enum class PanCoaterSprayMethod(val displayName: String) {
        SURFACE_AREA_2_3("Surface Area Basis (Batch Size ^ 2/3)"),
        LINEAR_BATCH_SIZE("Linear Batch Size Basis (Batch2 / Batch1)")
    }

    fun calculatePanCoater(input: PanCoaterInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        assumptions.add("Peripheral pan speed is held constant: N2 = N1 * (D1 / D2) to achieve identical tablet cascading dynamics.")
        assumptions.add("Spray-rate methodology: ${input.sprayMethod.displayName}.")
        assumptions.add("Exhaust airflow is scaled to balance total solvent evaporation rate.")

        engineeringNotes.add("Surface area 2/3 exponent reflects tablet bed exposed presentation area under the spray guns.")
        engineeringNotes.add("Gun-to-bed distance (typically 20-25 cm) and gun-to-gun spacing must be verified on commercial multi-gun manifolds.")

        val peripheralSpeed1 = (PI * (input.sourcePanDiaMm / 1000.0) * input.sourcePanRpm) / 60.0

        val tResults = input.targets.map { target ->
            val targetRpm = if (target.panDiaMm > 0) input.sourcePanRpm * (input.sourcePanDiaMm / target.panDiaMm) else 0.0
            val targetBatch = target.targetBatchKg ?: (input.sourceBatchKg * (target.panDiaMm / input.sourcePanDiaMm).pow(3))
            val batchRatio = if (input.sourceBatchKg > 0) targetBatch / input.sourceBatchKg else 1.0

            val sprayFactor = when (input.sprayMethod) {
                PanCoaterSprayMethod.SURFACE_AREA_2_3 -> batchRatio.pow(2.0 / 3.0)
                PanCoaterSprayMethod.LINEAR_BATCH_SIZE -> batchRatio
            }

            val targetSprayRate = input.sourceSprayRateGpm * sprayFactor
            val targetExhaustAirflow = input.sourceExhaustAirflowCfm * sprayFactor
            val peripheralSpeed2 = (PI * (target.panDiaMm / 1000.0) * targetRpm) / 60.0

            Triple(targetRpm, targetSprayRate, Triple(targetExhaustAirflow, targetBatch, peripheralSpeed2))
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = "PAN-01",
            title = "Pan Coater Speed & Spray Scale-Up",
            equationText = "N2 = N1 * (D1 / D2); Spray2 = Spray1 * (Batch2 / Batch1)^(2/3)",
            variablesDescription = mapOf(
                "N1" to "Source pan speed (RPM)",
                "N2" to "Target pan speed (RPM)",
                "D1" to "Source pan diameter (mm)",
                "D2" to "Target pan diameter (mm)",
                "Batch" to "Tablet core charge mass (kg)"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val d1 = formatNum(input.sourcePanDiaMm, 1)
                val d2 = formatNum(input.targets[0].panDiaMm, 1)
                "N2 = ${formatNum(input.sourcePanRpm, 1)} * ($d1 / $d2) = ${formatNum(t1?.first ?: 0.0, 1)} RPM; Spray = ${formatNum(t1?.second ?: 0.0, 1)} g/min"
            } else "N/A",
            calculatedResultText = "${formatNum(t1?.first ?: 0.0, 1)} RPM, ${formatNum(t1?.second ?: 0.0, 1)} g/min",
            methodologyType = MethodologyType.REFERENCE,
            engineeringNotes = "Maintains constant linear tablet bed cascade velocity."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Pan Speed",
                unit = "RPM",
                sourceValue = input.sourcePanRpm,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Total Spray Rate",
                unit = "g/min",
                sourceValue = input.sourceSprayRateGpm,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second
            ),
            ScaleParameter(
                name = "Exhaust Airflow",
                unit = "CFM / m³/h",
                sourceValue = input.sourceExhaustAirflowCfm,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first
            ),
            ScaleParameter(
                name = "Batch Size",
                unit = "kg",
                sourceValue = input.sourceBatchKg,
                target1Value = t1?.third?.second,
                target2Value = t2?.third?.second,
                target3Value = t3?.third?.second
            ),
            ScaleParameter(
                name = "Linear Pan Rim Velocity",
                unit = "m/s",
                sourceValue = peripheralSpeed1,
                target1Value = t1?.third?.third,
                target2Value = t2?.third?.third,
                target3Value = t3?.third?.third
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.PAN_COATER,
            methodologyName = "Constant Peripheral Speed & Surface Area Spray Scaling",
            methodologyType = MethodologyType.REFERENCE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 8. STIRRER / LIQUID MIXER
    // ==========================================
    data class StirrerInput(
        val sourceDiameterMm: Double,
        val sourceRpm: Double,
        val sourceVolumeL: Double,
        val method: StirrerMethod = StirrerMethod.CONSTANT_TIP_SPEED,
        val customK: Double = 1.0,
        val targets: List<StirrerTargetInput>
    )

    data class StirrerTargetInput(
        val name: String,
        val diameterMm: Double,
        val volumeL: Double
    )

    enum class StirrerMethod(val displayName: String, val k: Double) {
        CONSTANT_TIP_SPEED("Constant Tip Speed (k = 1.0)", 1.0),
        CONSTANT_POWER_VOLUME("Constant Power / Volume (k = 2/3 ≈ 0.667)", 2.0 / 3.0),
        CONSTANT_REYNOLDS("Constant Reynolds Number (k = 2.0)", 2.0),
        CUSTOM("Custom Exponent k", 1.0)
    }

    fun calculateStirrer(input: StirrerInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        val kVal = if (input.method == StirrerMethod.CUSTOM) input.customK else input.method.k

        assumptions.add("Selected scale-up criterion: ${input.method.displayName} with exponent k = ${formatNum(kVal, 3)}.")
        assumptions.add("Geometric similarity: Impeller-to-tank diameter ratio (D/T) and liquid height (H/T) remain constant.")

        engineeringNotes.add("k = 1.0 (Tip Speed) is recommended for shear-sensitive emulsions or polymer dissolution.")
        engineeringNotes.add("k = 2/3 (Equal P/V) is standard for mass transfer and blending of Newtonian solutions.")
        engineeringNotes.add("k = 2.0 (Equal Reynolds) ensures exact fluid dynamic similarity in laminar flow regimes.")

        val tResults = input.targets.map { target ->
            val dRatio = if (input.sourceDiameterMm > 0) input.sourceDiameterMm / target.diameterMm else 1.0
            val targetRpm = input.sourceRpm * dRatio.pow(kVal)
            val tipSpeedSource = (PI * (input.sourceDiameterMm / 1000.0) * input.sourceRpm) / 60.0
            val tipSpeedTarget = (PI * (target.diameterMm / 1000.0) * targetRpm) / 60.0

            Triple(targetRpm, tipSpeedTarget, target.volumeL / input.sourceVolumeL)
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = "STIR-01",
            title = "Liquid Agitator Speed Scale-Up",
            equationText = "N2 = N1 * (D1 / D2)^k",
            variablesDescription = mapOf(
                "N1" to "Source impeller rotational speed (RPM)",
                "N2" to "Target impeller rotational speed (RPM)",
                "D1" to "Source impeller diameter (mm)",
                "D2" to "Target impeller diameter (mm)",
                "k" to "Selected exponent (${formatNum(kVal, 3)})"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val d1 = formatNum(input.sourceDiameterMm, 1)
                val d2 = formatNum(input.targets[0].diameterMm, 1)
                "N2 = ${formatNum(input.sourceRpm, 1)} * ($d1 / $d2)^${formatNum(kVal, 3)} = ${formatNum(t1?.first ?: 0.0, 1)} RPM"
            } else "N/A",
            calculatedResultText = "${formatNum(t1?.first ?: 0.0, 1)} RPM",
            methodologyType = if (input.method == StirrerMethod.CUSTOM) MethodologyType.CUSTOM else MethodologyType.REFERENCE,
            engineeringNotes = "Distinguishes theoretical scaling assumptions from editable practical criteria."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Agitator Speed",
                unit = "RPM",
                sourceValue = input.sourceRpm,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                scaleFactor1 = if (input.sourceRpm > 0 && t1 != null) t1.first / input.sourceRpm else null,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Impeller Tip Speed",
                unit = "m/s",
                sourceValue = (PI * (input.sourceDiameterMm / 1000.0) * input.sourceRpm) / 60.0,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second
            ),
            ScaleParameter(
                name = "Volume Scale Factor",
                unit = "ratio",
                sourceValue = 1.0,
                target1Value = t1?.third,
                target2Value = t2?.third,
                target3Value = t3?.third
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.STIRRER,
            methodologyName = input.method.displayName,
            methodologyType = if (input.method == StirrerMethod.CUSTOM) MethodologyType.CUSTOM else MethodologyType.STANDARD_PRACTICE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 9. ROLLER COMPACTOR
    // ==========================================
    data class RollerCompactorInput(
        val sourceRollDiaMm: Double,
        val sourceRollWidthMm: Double,
        val sourceRollForceKn: Double,
        val sourceRollGapMm: Double,
        val sourceRollSpeedRpm: Double,
        val sourceScrewSpeedRpm: Double,
        val sourceThroughputKgHr: Double,
        val targets: List<RollerCompactorTargetInput>
    )

    data class RollerCompactorTargetInput(
        val name: String,
        val rollDiaMm: Double,
        val rollWidthMm: Double,
        val targetRollGapMm: Double? = null
    )

    fun calculateRollerCompactor(input: RollerCompactorInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        assumptions.add("Specific Compaction Force (SCF = Force / Roll Width in kN/cm) is held invariant.")
        assumptions.add("Screw-to-roll speed ratio is maintained to preserve ribbon solid fraction / porosity.")
        assumptions.add("Constant roll peripheral surface speed maintains identical powder nip angle residence time.")

        engineeringNotes.add("Specific compaction force (SCF) governs maximum hydraulic pressure exerted across ribbon width.")
        engineeringNotes.add("Target roll force = SCF * Target Roll Width (cm).")

        val sourceWidthCm = input.sourceRollWidthMm / 10.0
        val scfKnCm = if (sourceWidthCm > 0) input.sourceRollForceKn / sourceWidthCm else 0.0
        val screwToRollRatio = if (input.sourceRollSpeedRpm > 0) input.sourceScrewSpeedRpm / input.sourceRollSpeedRpm else 1.0

        val tResults = input.targets.map { target ->
            val targetWidthCm = target.rollWidthMm / 10.0
            val targetForceKn = scfKnCm * targetWidthCm
            val targetRollSpeed = if (target.rollDiaMm > 0) input.sourceRollSpeedRpm * (input.sourceRollDiaMm / target.rollDiaMm) else 0.0
            val targetScrewSpeed = targetRollSpeed * screwToRollRatio
            val targetThroughput = input.sourceThroughputKgHr * (target.rollWidthMm / input.sourceRollWidthMm) * (target.rollDiaMm / input.sourceRollDiaMm)
            val finalGap = target.targetRollGapMm ?: input.sourceRollGapMm

            Triple(targetForceKn, targetRollSpeed, Triple(targetScrewSpeed, targetThroughput, finalGap))
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = "RC-01",
            title = "Specific Compaction Force & Roll Speed Scale-Up",
            equationText = "SCF = Force1 / Width1 (kN/cm); Force2 = SCF * Width2; N2 = N1 * (D1 / D2)",
            variablesDescription = mapOf(
                "SCF" to "Specific compaction force (kN/cm)",
                "Force" to "Total hydraulic roll force (kN)",
                "Width" to "Roll face width (cm)",
                "N" to "Roll rotational speed (RPM)"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val scfStr = formatNum(scfKnCm, 2)
                val w2 = formatNum(input.targets[0].rollWidthMm / 10.0, 1)
                "SCF = ${formatNum(input.sourceRollForceKn, 1)} / ${formatNum(sourceWidthCm, 1)} = $scfStr kN/cm; Force2 = $scfStr * $w2 = ${formatNum(t1?.first ?: 0.0, 1)} kN"
            } else "N/A",
            calculatedResultText = "Target Force: ${formatNum(t1?.first ?: 0.0, 1)} kN",
            methodologyType = MethodologyType.REFERENCE,
            engineeringNotes = "Holds ribbon density and solid fraction constant across scales."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Specific Compaction Force (SCF)",
                unit = "kN/cm",
                sourceValue = scfKnCm,
                target1Value = scfKnCm,
                target2Value = scfKnCm,
                target3Value = scfKnCm,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Total Roll Force",
                unit = "kN",
                sourceValue = input.sourceRollForceKn,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                scaleFactor1 = if (input.sourceRollForceKn > 0 && t1 != null) t1.first / input.sourceRollForceKn else null
            ),
            ScaleParameter(
                name = "Roll Speed",
                unit = "RPM",
                sourceValue = input.sourceRollSpeedRpm,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second
            ),
            ScaleParameter(
                name = "Feed Screw Speed",
                unit = "RPM",
                sourceValue = input.sourceScrewSpeedRpm,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first
            ),
            ScaleParameter(
                name = "Estimated Throughput",
                unit = "kg/h",
                sourceValue = input.sourceThroughputKgHr,
                target1Value = t1?.third?.second,
                target2Value = t2?.third?.second,
                target3Value = t3?.third?.second
            ),
            ScaleParameter(
                name = "Roll Gap",
                unit = "mm",
                sourceValue = input.sourceRollGapMm,
                target1Value = t1?.third?.third,
                target2Value = t2?.third?.third,
                target3Value = t3?.third?.third
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.ROLLER_COMPACTOR,
            methodologyName = "Specific Compaction Force & Peripheral Speed Scaling",
            methodologyType = MethodologyType.REFERENCE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 10. MULTIMILL & 11. QUADRO MILL
    // ==========================================
    data class MillInput(
        val isQuadroMill: Boolean = false,
        val sourceRotorDiaMm: Double,
        val sourceRpm: Double,
        val sourceScreenOpeningMm: Double,
        val sourceThroughputKgHr: Double,
        val throughputExponent: Double = 2.0,
        val targets: List<MillTargetInput>
    )

    data class MillTargetInput(
        val name: String,
        val rotorDiaMm: Double,
        val screenOpeningMm: Double? = null
    )

    fun calculateMill(input: MillInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        val equipType = if (input.isQuadroMill) EquipmentType.QUADRO_MILL else EquipmentType.MULTIMILL
        val equipName = if (input.isQuadroMill) "Quadro Comil" else "Multimill"

        assumptions.add("Constant rotor tip speed ensures identical impact kinetic energy / shear stress for particle size reduction.")
        assumptions.add("Screen opening geometry (${input.sourceScreenOpeningMm} mm) and rotor-to-screen gap are kept constant unless specified.")
        assumptions.add("Throughput capacity scales with rotor cylindrical surface area: Q2 = Q1 * (D2/D1)^${input.throughputExponent}.")

        engineeringNotes.add("In conical mills (Quadro Comil), impeller profile (round vs square vs knife edge) greatly affects fines generation.")
        engineeringNotes.add("Check product temperature increase across high-speed hammer mills for low melting point active substances.")

        val tipSpeed1 = (PI * (input.sourceRotorDiaMm / 1000.0) * input.sourceRpm) / 60.0

        val tResults = input.targets.map { target ->
            val dRatio = if (input.sourceRotorDiaMm > 0) target.rotorDiaMm / input.sourceRotorDiaMm else 1.0
            val targetRpm = if (target.rotorDiaMm > 0) input.sourceRpm * (input.sourceRotorDiaMm / target.rotorDiaMm) else 0.0
            val targetThroughput = input.sourceThroughputKgHr * dRatio.pow(input.throughputExponent)
            val tipSpeed2 = (PI * (target.rotorDiaMm / 1000.0) * targetRpm) / 60.0
            val targetScreen = target.screenOpeningMm ?: input.sourceScreenOpeningMm

            Triple(targetRpm, targetThroughput, tipSpeed2 to targetScreen)
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = if (input.isQuadroMill) "QUAD-01" else "MULTI-01",
            title = "$equipName Rotor Speed & Throughput Scale-Up",
            equationText = "N2 = N1 * (D1 / D2); Q2 = Q1 * (D2 / D1)^${input.throughputExponent}",
            variablesDescription = mapOf(
                "N1" to "Source rotor speed (RPM)",
                "N2" to "Target rotor speed (RPM)",
                "D1" to "Source rotor diameter (mm)",
                "D2" to "Target rotor diameter (mm)",
                "Q" to "Milling feed throughput (kg/h)"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val d1 = formatNum(input.sourceRotorDiaMm, 1)
                val d2 = formatNum(input.targets[0].rotorDiaMm, 1)
                "N2 = ${formatNum(input.sourceRpm, 1)} * ($d1 / $d2) = ${formatNum(t1?.first ?: 0.0, 1)} RPM; Q2 = ${formatNum(input.sourceThroughputKgHr, 1)} * ($d2 / $d1)^2 = ${formatNum(t1?.second ?: 0.0, 1)} kg/h"
            } else "N/A",
            calculatedResultText = "${formatNum(t1?.first ?: 0.0, 1)} RPM, ${formatNum(t1?.second ?: 0.0, 1)} kg/h",
            methodologyType = MethodologyType.REFERENCE,
            engineeringNotes = "Constant tip speed preserves particle size distribution (PSD)."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Rotor Speed",
                unit = "RPM",
                sourceValue = input.sourceRpm,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Rotor Tip Speed",
                unit = "m/s",
                sourceValue = tipSpeed1,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first
            ),
            ScaleParameter(
                name = "Milling Throughput",
                unit = "kg/h",
                sourceValue = input.sourceThroughputKgHr,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second
            ),
            ScaleParameter(
                name = "Screen Opening",
                unit = "mm",
                sourceValue = input.sourceScreenOpeningMm,
                target1Value = t1?.third?.second,
                target2Value = t2?.third?.second,
                target3Value = t3?.third?.second,
                notes = "Hold identical across scales to maintain PSD"
            )
        )

        return ScaleUpResult(
            equipmentType = equipType,
            methodologyName = "Constant Tip Speed & Area-Based Feed Rate Scaling",
            methodologyType = MethodologyType.REFERENCE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 12. TABLET COMPRESSION MACHINE
    // ==========================================
    data class TabletPressInput(
        val sourceStations: Int,
        val sourceTurretRpm: Double,
        val tabletsPerRev: Int = 1,
        val targetTabletWeightMg: Double,
        val batchSizeTablets: Double,
        val machineEfficiencyPct: Double = 90.0,
        val turretUtilizationPct: Double = 95.0,
        val rejectPct: Double = 1.0,
        val targets: List<TabletPressTargetInput>
    )

    data class TabletPressTargetInput(
        val name: String,
        val stations: Int,
        val turretRpm: Double,
        val efficiencyPct: Double? = null
    )

    fun calculateTabletPress(input: TabletPressInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        assumptions.add("Dwell time = (Punch Flat Diameter / Turret Pitch Velocity) must remain above critical threshold (~10-15 ms) for capping prevention.")
        assumptions.add("Overall Equipment Effectiveness (OEE) incorporates machine efficiency, utilization, and reject rates.")

        engineeringNotes.add("Output (tabs/hr) = Stations * Turret RPM * 60 * tablets_per_revolution.")
        engineeringNotes.add("Effective output = Theoretical * (Efficiency % / 100) * (Utilization % / 100) * (1 - Reject % / 100).")

        val factorEff = (input.machineEfficiencyPct / 100.0) * (input.turretUtilizationPct / 100.0) * (1.0 - input.rejectPct / 100.0)
        val sourceTheoHr = input.sourceStations * input.sourceTurretRpm * input.tabletsPerRev * 60.0
        val sourceEffHr = sourceTheoHr * factorEff
        val sourceBatchHours = if (sourceEffHr > 0) input.batchSizeTablets / sourceEffHr else 0.0

        val tResults = input.targets.map { target ->
            val eff = (target.efficiencyPct ?: input.machineEfficiencyPct) / 100.0 * (input.turretUtilizationPct / 100.0) * (1.0 - input.rejectPct / 100.0)
            val theoHr = target.stations * target.turretRpm * input.tabletsPerRev * 60.0
            val effHr = theoHr * eff
            val batchHours = if (effHr > 0) input.batchSizeTablets / effHr else 0.0
            val sf = if (sourceEffHr > 0) effHr / sourceEffHr else 1.0

            Triple(theoHr, effHr, batchHours to sf)
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = "PRESS-01",
            title = "Tablet Press Output & Cycle Time",
            equationText = "Theo Output/hr = Stations * Turret RPM * 60; Eff Output = Theo * OEE factor",
            variablesDescription = mapOf(
                "Stations" to "Number of punch tooling stations",
                "Turret RPM" to "Turret rotation speed",
                "Batch Hours" to "Batch Size / Effective Output per Hour"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val s1 = input.targets[0].stations
                val r1 = formatNum(input.targets[0].turretRpm, 1)
                "Output = $s1 * $r1 * 60 = ${formatNum(t1?.first ?: 0.0, 0)} tabs/hr; Effective = ${formatNum(t1?.second ?: 0.0, 0)} tabs/hr"
            } else "N/A",
            calculatedResultText = "${formatNum(t1?.second ?: 0.0, 0)} tabs/hr (${formatNum(t1?.third?.first ?: 0.0, 2)} hrs)",
            methodologyType = MethodologyType.STANDARD_PRACTICE,
            engineeringNotes = "Accounts for machine efficiency, startup adjustment losses, and reject allowance."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Theoretical Output",
                unit = "tablets/hr",
                sourceValue = sourceTheoHr,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Adjusted Effective Output",
                unit = "tablets/hr",
                sourceValue = sourceEffHr,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second,
                scaleFactor1 = t1?.third?.second
            ),
            ScaleParameter(
                name = "Batch Processing Time",
                unit = "hours",
                sourceValue = sourceBatchHours,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first
            ),
            ScaleParameter(
                name = "Compression Station Count",
                unit = "stations",
                sourceValue = input.sourceStations.toDouble(),
                target1Value = input.targets.getOrNull(0)?.stations?.toDouble(),
                target2Value = input.targets.getOrNull(1)?.stations?.toDouble(),
                target3Value = input.targets.getOrNull(2)?.stations?.toDouble()
            ),
            ScaleParameter(
                name = "Turret Speed",
                unit = "RPM",
                sourceValue = input.sourceTurretRpm,
                target1Value = input.targets.getOrNull(0)?.turretRpm,
                target2Value = input.targets.getOrNull(1)?.turretRpm,
                target3Value = input.targets.getOrNull(2)?.turretRpm
            )
        )

        return ScaleUpResult(
            equipmentType = EquipmentType.TABLET_PRESS,
            methodologyName = "Machine Output & Dwell Cycle Scaling",
            methodologyType = MethodologyType.STANDARD_PRACTICE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }

    // ==========================================
    // 13. CAPSULE FILLER — POWDER & 14. PELLETS
    // ==========================================
    data class CapsuleFillerInput(
        val isPelletFill: Boolean = false,
        val capsuleSize: String = "Size 0",
        val targetFillWeightMg: Double,
        val sourceStations: Int,
        val sourceSpeedCpm: Double, // cycles per min
        val sourceBatchCapsules: Double,
        val efficiencyPct: Double = 92.0,
        val targets: List<CapsuleFillerTargetInput>
    )

    data class CapsuleFillerTargetInput(
        val name: String,
        val stations: Int,
        val speedCpm: Double,
        val efficiencyPct: Double? = null
    )

    fun calculateCapsuleFiller(input: CapsuleFillerInput): ScaleUpResult {
        val warnings = mutableListOf<CalculationWarning>()
        val assumptions = mutableListOf<String>()
        val engineeringNotes = mutableListOf<String>()

        val equipType = if (input.isPelletFill) EquipmentType.CAPSULE_PELLETS else EquipmentType.CAPSULE_POWDER
        val fillType = if (input.isPelletFill) "Pellet / Multiparticulate" else "Powder / Granule"

        assumptions.add("Dosing system principle (Dosator pin vs Tamping ring / Pellet chamber) is mechanically consistent.")
        assumptions.add("Machine overall operating efficiency incorporates capsule opening, dosing, closing, and sorting stations.")

        engineeringNotes.add("Theoretical output = Speed (cycles/min) * Dosing stations * 60.")
        engineeringNotes.add("Effective output = Theoretical * (Efficiency % / 100).")

        val sourceTheoHr = input.sourceSpeedCpm * input.sourceStations * 60.0
        val sourceEffHr = sourceTheoHr * (input.efficiencyPct / 100.0)
        val sourceBatchHours = if (sourceEffHr > 0) input.sourceBatchCapsules / sourceEffHr else 0.0
        val totalPowderKg = (input.sourceBatchCapsules * input.targetFillWeightMg) / 1_000_000.0

        val tResults = input.targets.map { target ->
            val eff = (target.efficiencyPct ?: input.efficiencyPct) / 100.0
            val theoHr = target.speedCpm * target.stations * 60.0
            val effHr = theoHr * eff
            val batchHours = if (effHr > 0) input.sourceBatchCapsules / effHr else 0.0
            val sf = if (sourceEffHr > 0) effHr / sourceEffHr else 1.0

            Triple(theoHr, effHr, batchHours to sf)
        }

        val t1 = tResults.getOrNull(0)
        val t2 = tResults.getOrNull(1)
        val t3 = tResults.getOrNull(2)

        val formula = FormulaDetail(
            formulaId = if (input.isPelletFill) "CAP-PEL-01" else "CAP-POW-01",
            title = "Capsule Filler ($fillType) Output Calculation",
            equationText = "Theo Output/hr = Cycles/min * Stations * 60; Eff Output = Theo * Eff %",
            variablesDescription = mapOf(
                "Speed" to "Machine cycle speed (cycles/min)",
                "Stations" to "Number of dosing segment bore stations",
                "Eff %" to "Machine operating efficiency percentage"
            ),
            substitutedText = if (input.targets.isNotEmpty()) {
                val c1 = formatNum(input.targets[0].speedCpm, 1)
                val s1 = input.targets[0].stations
                "Theo = $c1 * $s1 * 60 = ${formatNum(t1?.first ?: 0.0, 0)} caps/hr; Eff = ${formatNum(t1?.second ?: 0.0, 0)} caps/hr"
            } else "N/A",
            calculatedResultText = "${formatNum(t1?.second ?: 0.0, 0)} caps/hr (${formatNum(t1?.third?.first ?: 0.0, 2)} hrs)",
            methodologyType = MethodologyType.STANDARD_PRACTICE,
            engineeringNotes = "Dosing disk thickness / pellet chamber height controls fill weight accuracy."
        )

        val parameters = listOf(
            ScaleParameter(
                name = "Theoretical Output",
                unit = "capsules/hr",
                sourceValue = sourceTheoHr,
                target1Value = t1?.first,
                target2Value = t2?.first,
                target3Value = t3?.first,
                formulaDetail = formula
            ),
            ScaleParameter(
                name = "Effective Production Output",
                unit = "capsules/hr",
                sourceValue = sourceEffHr,
                target1Value = t1?.second,
                target2Value = t2?.second,
                target3Value = t3?.second,
                scaleFactor1 = t1?.third?.second
            ),
            ScaleParameter(
                name = "Batch Run Time",
                unit = "hours",
                sourceValue = sourceBatchHours,
                target1Value = t1?.third?.first,
                target2Value = t2?.third?.first,
                target3Value = t3?.third?.first
            ),
            ScaleParameter(
                name = "Total Batch Formulation Mass",
                unit = "kg",
                sourceValue = totalPowderKg,
                target1Value = totalPowderKg,
                target2Value = totalPowderKg,
                target3Value = totalPowderKg,
                notes = "Calculated from ${formatNum(input.targetFillWeightMg, 1)} mg fill weight"
            ),
            ScaleParameter(
                name = "Machine Speed",
                unit = "cycles/min",
                sourceValue = input.sourceSpeedCpm,
                target1Value = input.targets.getOrNull(0)?.speedCpm,
                target2Value = input.targets.getOrNull(1)?.speedCpm,
                target3Value = input.targets.getOrNull(2)?.speedCpm
            )
        )

        return ScaleUpResult(
            equipmentType = equipType,
            methodologyName = "Machine Cycle Capacity & Filling Output",
            methodologyType = MethodologyType.STANDARD_PRACTICE,
            parameters = parameters,
            warnings = warnings,
            assumptions = assumptions,
            engineeringNotes = engineeringNotes
        )
    }
}
