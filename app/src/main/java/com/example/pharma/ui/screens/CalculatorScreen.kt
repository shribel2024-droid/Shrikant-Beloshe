package com.example.pharma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pharma.data.db.CalculationHistoryEntity
import com.example.pharma.data.db.EquipmentCalculationEntity
import com.example.pharma.data.repository.CalculationRepository
import com.example.pharma.domain.engine.CalculationEngine
import com.example.pharma.domain.model.*
import com.example.pharma.ui.components.FormulaDetailDialog
import com.example.pharma.ui.components.ParameterRow
import com.example.pharma.ui.components.TrafficLightBadge
import com.example.pharma.util.export.ReportGenerators
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    initialEquipment: EquipmentType = EquipmentType.RMG,
    calculationRepository: CalculationRepository? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedEquipment by remember { mutableStateOf(initialEquipment) }
    var selectedMethodIndex by remember { mutableIntStateOf(0) }
    var showFormulaDialog by remember { mutableStateOf<FormulaDetail?>(null) }
    var activeTargetTab by remember { mutableIntStateOf(0) } // 0: Target 1, 1: Target 2, 2: Target 3

    // RMG State (Defaulted to workbook example D1=170mm, D2=390mm, N1=500RPM)
    var rmgD1 by remember { mutableStateOf("170") }
    var rmgD2 by remember { mutableStateOf("390") }
    var rmgD3 by remember { mutableStateOf("680") }
    var rmgD4 by remember { mutableStateOf("850") }
    var rmgN1 by remember { mutableStateOf("500") }
    var rmgBowl1 by remember { mutableStateOf("4") }
    var rmgBowl2 by remember { mutableStateOf("65") }
    var rmgBowl3 by remember { mutableStateOf("300") }
    var rmgBowl4 by remember { mutableStateOf("600") }
    var rmgBatch1 by remember { mutableStateOf("1.5") }
    var rmgDensity by remember { mutableStateOf("0.55") }
    var rmgMaxRpm by remember { mutableStateOf("500") }

    // Blender State (Workbook example D1=1.0m, D2=2.5m, N1=25RPM, t1=15min)
    var blendD1 by remember { mutableStateOf("1.0") }
    var blendD2 by remember { mutableStateOf("2.5") }
    var blendN1 by remember { mutableStateOf("25") }
    var blendT1 by remember { mutableStateOf("15") }
    var blendV1 by remember { mutableStateOf("400") }
    var blendV2 by remember { mutableStateOf("1200") }
    var blendBatch1 by remember { mutableStateOf("120") }
    var blendDensity by remember { mutableStateOf("0.55") }

    // FBP Top Spray State
    var fbpD1 by remember { mutableStateOf("150") }
    var fbpD2 by remember { mutableStateOf("450") }
    var fbpD3 by remember { mutableStateOf("900") }
    var fbpAir1 by remember { mutableStateOf("120") }
    var fbpSpray1 by remember { mutableStateOf("25") }
    var fbpAtom1 by remember { mutableStateOf("1.5") }
    var fbpBatch1 by remember { mutableStateOf("2.5") }

    // FBP Wurster State
    var wurCols1 by remember { mutableStateOf("1") }
    var wurCols2 by remember { mutableStateOf("1") }
    var wurCols3 by remember { mutableStateOf("3") }
    var wurDia1 by remember { mutableStateOf("70") }
    var wurDia2 by remember { mutableStateOf("150") }
    var wurDia3 by remember { mutableStateOf("225") }
    var wurAir1 by remember { mutableStateOf("80") }
    var wurSpray1 by remember { mutableStateOf("15") }
    var wurGap1 by remember { mutableStateOf("18") }

    // Extruder State
    var extD1 by remember { mutableStateOf("18") }
    var extD2 by remember { mutableStateOf("35") }
    var extD3 by remember { mutableStateOf("50") }
    var extQ1 by remember { mutableStateOf("5.0") }
    var extN1 by remember { mutableStateOf("250") }
    var extCustomExp by remember { mutableStateOf("2.5") }

    // Spheronizer State
    var sphD1 by remember { mutableStateOf("250") }
    var sphD2 by remember { mutableStateOf("500") }
    var sphD3 by remember { mutableStateOf("700") }
    var sphN1 by remember { mutableStateOf("1200") }
    var sphLoad1 by remember { mutableStateOf("1.0") }

    // Pan Coater State
    var panD1 by remember { mutableStateOf("300") }
    var panD2 by remember { mutableStateOf("750") }
    var panD3 by remember { mutableStateOf("1500") }
    var panN1 by remember { mutableStateOf("18") }
    var panBatch1 by remember { mutableStateOf("2.5") }
    var panSpray1 by remember { mutableStateOf("12") }
    var panAir1 by remember { mutableStateOf("150") }

    // Stirrer State
    var stirD1 by remember { mutableStateOf("100") }
    var stirD2 by remember { mutableStateOf("250") }
    var stirD3 by remember { mutableStateOf("450") }
    var stirN1 by remember { mutableStateOf("600") }
    var stirV1 by remember { mutableStateOf("5") }
    var stirV2 by remember { mutableStateOf("50") }
    var stirV3 by remember { mutableStateOf("250") }
    var stirCustomK by remember { mutableStateOf("1.0") }

    // Roller Compactor State
    var rcD1 by remember { mutableStateOf("120") }
    var rcW1 by remember { mutableStateOf("25") }
    var rcForce1 by remember { mutableStateOf("15") }
    var rcGap1 by remember { mutableStateOf("2.5") }
    var rcRollN1 by remember { mutableStateOf("6.0") }
    var rcScrewN1 by remember { mutableStateOf("30.0") }
    var rcQ1 by remember { mutableStateOf("10.0") }
    var rcD2 by remember { mutableStateOf("200") }
    var rcW2 by remember { mutableStateOf("75") }

    // Mill State
    var millD1 by remember { mutableStateOf("150") }
    var millD2 by remember { mutableStateOf("300") }
    var millN1 by remember { mutableStateOf("2800") }
    var millScreen1 by remember { mutableStateOf("1.0") }
    var millQ1 by remember { mutableStateOf("45") }

    // Tablet Compression State
    var pressStations1 by remember { mutableStateOf("8") }
    var pressStations2 by remember { mutableStateOf("29") }
    var pressStations3 by remember { mutableStateOf("61") }
    var pressRpm1 by remember { mutableStateOf("40") }
    var pressRpm2 by remember { mutableStateOf("65") }
    var pressRpm3 by remember { mutableStateOf("75") }
    var pressWeightMg by remember { mutableStateOf("500") }
    var pressBatchTabs by remember { mutableStateOf("500000") }
    var pressEffPct by remember { mutableStateOf("92") }

    // Capsule Filler State
    var capStations1 by remember { mutableStateOf("8") }
    var capStations2 by remember { mutableStateOf("12") }
    var capSpeed1 by remember { mutableStateOf("400") }
    var capSpeed2 by remember { mutableStateOf("1200") }
    var capFillMg by remember { mutableStateOf("350") }
    var capBatchCaps by remember { mutableStateOf("250000") }

    // Calculate dynamically based on selectedEquipment
    val currentResult: ScaleUpResult = remember(
        selectedEquipment, selectedMethodIndex,
        rmgD1, rmgD2, rmgD3, rmgD4, rmgN1, rmgBowl1, rmgBowl2, rmgBowl3, rmgBowl4, rmgBatch1, rmgDensity, rmgMaxRpm,
        blendD1, blendD2, blendN1, blendT1, blendV1, blendV2, blendBatch1, blendDensity,
        fbpD1, fbpD2, fbpD3, fbpAir1, fbpSpray1, fbpAtom1, fbpBatch1,
        wurCols1, wurCols2, wurCols3, wurDia1, wurDia2, wurDia3, wurAir1, wurSpray1, wurGap1,
        extD1, extD2, extD3, extQ1, extN1, extCustomExp,
        sphD1, sphD2, sphD3, sphN1, sphLoad1,
        panD1, panD2, panD3, panN1, panBatch1, panSpray1, panAir1,
        stirD1, stirD2, stirD3, stirN1, stirV1, stirV2, stirV3, stirCustomK,
        rcD1, rcW1, rcForce1, rcGap1, rcRollN1, rcScrewN1, rcQ1, rcD2, rcW2,
        millD1, millD2, millN1, millScreen1, millQ1,
        pressStations1, pressStations2, pressStations3, pressRpm1, pressRpm2, pressRpm3, pressWeightMg, pressBatchTabs, pressEffPct,
        capStations1, capStations2, capSpeed1, capSpeed2, capFillMg, capBatchCaps
    ) {
        when (selectedEquipment) {
            EquipmentType.RMG -> {
                val targets = listOf(
                    CalculationEngine.RmgTargetInput("Pilot 1", rmgD2.toDoubleOrNull() ?: 390.0, rmgBowl2.toDoubleOrNull() ?: 65.0, (rmgBowl2.toDoubleOrNull() ?: 65.0) * 0.65, null, rmgMaxRpm.toDoubleOrNull() ?: 500.0),
                    CalculationEngine.RmgTargetInput("Pilot 2", rmgD3.toDoubleOrNull() ?: 680.0, rmgBowl3.toDoubleOrNull() ?: 300.0, (rmgBowl3.toDoubleOrNull() ?: 300.0) * 0.65, null, 280.0),
                    CalculationEngine.RmgTargetInput("Commercial", rmgD4.toDoubleOrNull() ?: 850.0, rmgBowl4.toDoubleOrNull() ?: 600.0, (rmgBowl4.toDoubleOrNull() ?: 600.0) * 0.65, null, 200.0)
                )
                val method = if (selectedMethodIndex == 1) CalculationEngine.RmgMethod.CONSTANT_FROUDE else CalculationEngine.RmgMethod.CONSTANT_TIP_SPEED
                CalculationEngine.calculateRmg(
                    CalculationEngine.RmgInput(
                        sourceDiameterMm = rmgD1.toDoubleOrNull() ?: 170.0,
                        sourceRpm = rmgN1.toDoubleOrNull() ?: 500.0,
                        sourceBowlTotalL = rmgBowl1.toDoubleOrNull() ?: 4.0,
                        sourceWorkingL = (rmgBowl1.toDoubleOrNull() ?: 4.0) * 0.65,
                        sourceBatchKg = rmgBatch1.toDoubleOrNull() ?: 1.5,
                        bulkDensityGml = rmgDensity.toDoubleOrNull() ?: 0.55,
                        targets = targets,
                        method = method
                    )
                )
            }
            EquipmentType.BLENDER -> {
                val targets = listOf(
                    CalculationEngine.BlenderTargetInput("Pilot/Commercial", blendD2.toDoubleOrNull() ?: 2.5, blendV2.toDoubleOrNull() ?: 1200.0, null, 20.0)
                )
                val method = if (selectedMethodIndex == 1) CalculationEngine.BlenderMethod.CONSTANT_TIP_SPEED else CalculationEngine.BlenderMethod.CONSTANT_FROUDE
                CalculationEngine.calculateBlender(
                    CalculationEngine.BlenderInput(
                        sourceVolumeL = blendV1.toDoubleOrNull() ?: 400.0,
                        sourceWorkingL = (blendV1.toDoubleOrNull() ?: 400.0) * 0.55,
                        sourceDiameterM = blendD1.toDoubleOrNull() ?: 1.0,
                        sourceRpm = blendN1.toDoubleOrNull() ?: 25.0,
                        sourceBlendTimeMin = blendT1.toDoubleOrNull() ?: 15.0,
                        bulkDensityGml = blendDensity.toDoubleOrNull() ?: 0.55,
                        sourceBatchKg = blendBatch1.toDoubleOrNull() ?: 120.0,
                        targets = targets,
                        method = method
                    )
                )
            }
            EquipmentType.FBP_TOP_SPRAY -> {
                val targets = listOf(
                    CalculationEngine.FbpTopSprayTargetInput("Pilot FBP", fbpD2.toDoubleOrNull() ?: 450.0),
                    CalculationEngine.FbpTopSprayTargetInput("Commercial FBP", fbpD3.toDoubleOrNull() ?: 900.0, null, 3)
                )
                CalculationEngine.calculateFbpTopSpray(
                    CalculationEngine.FbpTopSprayInput(
                        sourceDiameterMm = fbpD1.toDoubleOrNull() ?: 150.0,
                        sourceBatchKg = fbpBatch1.toDoubleOrNull() ?: 2.5,
                        sourceAirflowCfm = fbpAir1.toDoubleOrNull() ?: 120.0,
                        sourceSprayRateGpm = fbpSpray1.toDoubleOrNull() ?: 25.0,
                        sourceAtomizingBar = fbpAtom1.toDoubleOrNull() ?: 1.5,
                        targets = targets
                    )
                )
            }
            EquipmentType.FBP_WURSTER -> {
                val targets = listOf(
                    CalculationEngine.FbpWursterTargetInput("Pilot Wurster", wurCols2.toIntOrNull() ?: 1, wurDia2.toDoubleOrNull() ?: 150.0),
                    CalculationEngine.FbpWursterTargetInput("Commercial Wurster", wurCols3.toIntOrNull() ?: 3, wurDia3.toDoubleOrNull() ?: 225.0)
                )
                CalculationEngine.calculateFbpWurster(
                    CalculationEngine.FbpWursterInput(
                        sourceColumns = wurCols1.toIntOrNull() ?: 1,
                        sourceColumnDiaMm = wurDia1.toDoubleOrNull() ?: 70.0,
                        sourceAirflowCfm = wurAir1.toDoubleOrNull() ?: 80.0,
                        sourceSprayRateGpm = wurSpray1.toDoubleOrNull() ?: 15.0,
                        sourcePartitionGapMm = wurGap1.toDoubleOrNull() ?: 18.0,
                        targets = targets
                    )
                )
            }
            EquipmentType.EXTRUDER -> {
                val targets = listOf(
                    CalculationEngine.ExtruderTargetInput("Pilot Extruder", extD2.toDoubleOrNull() ?: 35.0),
                    CalculationEngine.ExtruderTargetInput("Commercial Extruder", extD3.toDoubleOrNull() ?: 50.0)
                )
                val method = when (selectedMethodIndex) {
                    0 -> CalculationEngine.ExtruderMethod.HEAT_LIMITED
                    1 -> CalculationEngine.ExtruderMethod.CUBE_RULE
                    else -> CalculationEngine.ExtruderMethod.CUSTOM
                }
                CalculationEngine.calculateExtruder(
                    CalculationEngine.ExtruderInput(
                        sourceScrewDiaMm = extD1.toDoubleOrNull() ?: 18.0,
                        sourceThroughputKgHr = extQ1.toDoubleOrNull() ?: 5.0,
                        sourceScrewRpm = extN1.toDoubleOrNull() ?: 250.0,
                        sourceProductTempC = 35.0,
                        method = method,
                        customExponent = extCustomExp.toDoubleOrNull() ?: 2.5,
                        targets = targets
                    )
                )
            }
            EquipmentType.SPHERONIZER -> {
                val targets = listOf(
                    CalculationEngine.SpheronizerTargetInput("Pilot Spheronizer", sphD2.toDoubleOrNull() ?: 500.0),
                    CalculationEngine.SpheronizerTargetInput("Commercial Spheronizer", sphD3.toDoubleOrNull() ?: 700.0)
                )
                CalculationEngine.calculateSpheronizer(
                    CalculationEngine.SpheronizerInput(
                        sourcePlateDiaMm = sphD1.toDoubleOrNull() ?: 250.0,
                        sourceRpm = sphN1.toDoubleOrNull() ?: 1200.0,
                        sourceBatchLoadKg = sphLoad1.toDoubleOrNull() ?: 1.0,
                        targets = targets
                    )
                )
            }
            EquipmentType.PAN_COATER -> {
                val targets = listOf(
                    CalculationEngine.PanCoaterTargetInput("Pilot Coater", panD2.toDoubleOrNull() ?: 750.0),
                    CalculationEngine.PanCoaterTargetInput("Commercial Coater", panD3.toDoubleOrNull() ?: 1500.0)
                )
                val method = if (selectedMethodIndex == 1) CalculationEngine.PanCoaterSprayMethod.LINEAR_BATCH_SIZE else CalculationEngine.PanCoaterSprayMethod.SURFACE_AREA_2_3
                CalculationEngine.calculatePanCoater(
                    CalculationEngine.PanCoaterInput(
                        sourcePanDiaMm = panD1.toDoubleOrNull() ?: 300.0,
                        sourcePanRpm = panN1.toDoubleOrNull() ?: 18.0,
                        sourceBatchKg = panBatch1.toDoubleOrNull() ?: 2.5,
                        sourceSprayRateGpm = panSpray1.toDoubleOrNull() ?: 12.0,
                        sourceExhaustAirflowCfm = panAir1.toDoubleOrNull() ?: 150.0,
                        sprayMethod = method,
                        targets = targets
                    )
                )
            }
            EquipmentType.STIRRER -> {
                val targets = listOf(
                    CalculationEngine.StirrerTargetInput("Pilot Vessel", stirD2.toDoubleOrNull() ?: 250.0, stirV2.toDoubleOrNull() ?: 50.0),
                    CalculationEngine.StirrerTargetInput("Commercial Tank", stirD3.toDoubleOrNull() ?: 450.0, stirV3.toDoubleOrNull() ?: 250.0)
                )
                val method = when (selectedMethodIndex) {
                    0 -> CalculationEngine.StirrerMethod.CONSTANT_TIP_SPEED
                    1 -> CalculationEngine.StirrerMethod.CONSTANT_POWER_VOLUME
                    2 -> CalculationEngine.StirrerMethod.CONSTANT_REYNOLDS
                    else -> CalculationEngine.StirrerMethod.CUSTOM
                }
                CalculationEngine.calculateStirrer(
                    CalculationEngine.StirrerInput(
                        sourceDiameterMm = stirD1.toDoubleOrNull() ?: 100.0,
                        sourceRpm = stirN1.toDoubleOrNull() ?: 600.0,
                        sourceVolumeL = stirV1.toDoubleOrNull() ?: 5.0,
                        method = method,
                        customK = stirCustomK.toDoubleOrNull() ?: 1.0,
                        targets = targets
                    )
                )
            }
            EquipmentType.ROLLER_COMPACTOR -> {
                val targets = listOf(
                    CalculationEngine.RollerCompactorTargetInput("Commercial WP 200", rcD2.toDoubleOrNull() ?: 200.0, rcW2.toDoubleOrNull() ?: 75.0)
                )
                CalculationEngine.calculateRollerCompactor(
                    CalculationEngine.RollerCompactorInput(
                        sourceRollDiaMm = rcD1.toDoubleOrNull() ?: 120.0,
                        sourceRollWidthMm = rcW1.toDoubleOrNull() ?: 25.0,
                        sourceRollForceKn = rcForce1.toDoubleOrNull() ?: 15.0,
                        sourceRollGapMm = rcGap1.toDoubleOrNull() ?: 2.5,
                        sourceRollSpeedRpm = rcRollN1.toDoubleOrNull() ?: 6.0,
                        sourceScrewSpeedRpm = rcScrewN1.toDoubleOrNull() ?: 30.0,
                        sourceThroughputKgHr = rcQ1.toDoubleOrNull() ?: 10.0,
                        targets = targets
                    )
                )
            }
            EquipmentType.MULTIMILL, EquipmentType.QUADRO_MILL -> {
                val targets = listOf(
                    CalculationEngine.MillTargetInput("Target Mill", millD2.toDoubleOrNull() ?: 300.0)
                )
                CalculationEngine.calculateMill(
                    CalculationEngine.MillInput(
                        isQuadroMill = (selectedEquipment == EquipmentType.QUADRO_MILL),
                        sourceRotorDiaMm = millD1.toDoubleOrNull() ?: 150.0,
                        sourceRpm = millN1.toDoubleOrNull() ?: 2800.0,
                        sourceScreenOpeningMm = millScreen1.toDoubleOrNull() ?: 1.0,
                        sourceThroughputKgHr = millQ1.toDoubleOrNull() ?: 45.0,
                        targets = targets
                    )
                )
            }
            EquipmentType.TABLET_PRESS -> {
                val targets = listOf(
                    CalculationEngine.TabletPressTargetInput("Commercial Press 1", pressStations2.toIntOrNull() ?: 29, pressRpm2.toDoubleOrNull() ?: 65.0),
                    CalculationEngine.TabletPressTargetInput("High Speed Press 2", pressStations3.toIntOrNull() ?: 61, pressRpm3.toDoubleOrNull() ?: 75.0)
                )
                CalculationEngine.calculateTabletPress(
                    CalculationEngine.TabletPressInput(
                        sourceStations = pressStations1.toIntOrNull() ?: 8,
                        sourceTurretRpm = pressRpm1.toDoubleOrNull() ?: 40.0,
                        targetTabletWeightMg = pressWeightMg.toDoubleOrNull() ?: 500.0,
                        batchSizeTablets = pressBatchTabs.toDoubleOrNull() ?: 500000.0,
                        machineEfficiencyPct = pressEffPct.toDoubleOrNull() ?: 92.0,
                        targets = targets
                    )
                )
            }
            EquipmentType.CAPSULE_POWDER, EquipmentType.CAPSULE_PELLETS -> {
                val targets = listOf(
                    CalculationEngine.CapsuleFillerTargetInput("Commercial Filler", capStations2.toIntOrNull() ?: 12, capSpeed2.toDoubleOrNull() ?: 1200.0)
                )
                CalculationEngine.calculateCapsuleFiller(
                    CalculationEngine.CapsuleFillerInput(
                        isPelletFill = (selectedEquipment == EquipmentType.CAPSULE_PELLETS),
                        targetFillWeightMg = capFillMg.toDoubleOrNull() ?: 350.0,
                        sourceStations = capStations1.toIntOrNull() ?: 8,
                        sourceSpeedCpm = capSpeed1.toDoubleOrNull() ?: 400.0,
                        sourceBatchCapsules = capBatchCaps.toDoubleOrNull() ?: 250000.0,
                        targets = targets
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scale-Up Calculator",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PharmaBluePrimary
                ),
                actions = {
                    IconButton(
                        onClick = {
                            val pdf = ReportGenerators.generatePdf(context, null, listOf(currentResult))
                            if (pdf != null) ReportGenerators.shareFile(context, pdf, "application/pdf")
                        },
                        modifier = Modifier.testTag("action_export_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export PDF",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Equipment Selector Dropdown
            Text(
                text = "SELECT EQUIPMENT MODULE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )
            Spacer(modifier = Modifier.height(6.dp))

            var equipExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = equipExpanded,
                onExpandedChange = { equipExpanded = !equipExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedEquipment.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("equipment_dropdown"),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = equipExpanded,
                    onDismissRequest = { equipExpanded = false }
                ) {
                    EquipmentType.values().forEach { eq ->
                        DropdownMenuItem(
                            text = { Text(eq.displayName, fontSize = 13.sp) },
                            onClick = {
                                selectedEquipment = eq
                                selectedMethodIndex = 0
                                equipExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scale-Up Methodology Selection
            Text(
                text = "SCALE-UP METHODOLOGY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )
            Spacer(modifier = Modifier.height(6.dp))

            when (selectedEquipment) {
                EquipmentType.RMG -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMethodIndex == 0,
                            onClick = { selectedMethodIndex = 0 },
                            label = { Text("Constant Tip Speed (N1 * D1/D2)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethodIndex == 1,
                            onClick = { selectedMethodIndex = 1 },
                            label = { Text("Constant Froude (Fr)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                EquipmentType.BLENDER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMethodIndex == 0,
                            onClick = { selectedMethodIndex = 0 },
                            label = { Text("Constant Froude", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethodIndex == 1,
                            onClick = { selectedMethodIndex = 1 },
                            label = { Text("Constant Tip Speed", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                EquipmentType.EXTRUDER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedMethodIndex == 0,
                            onClick = { selectedMethodIndex = 0 },
                            label = { Text("Heat-Limited (2.5)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethodIndex == 1,
                            onClick = { selectedMethodIndex = 1 },
                            label = { Text("Cube Rule (3.0)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethodIndex == 2,
                            onClick = { selectedMethodIndex = 2 },
                            label = { Text("Custom Exponent", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (selectedMethodIndex == 2) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = extCustomExp,
                            onValueChange = { extCustomExp = it },
                            label = { Text("Custom Exponent n") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                EquipmentType.STIRRER -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = selectedMethodIndex == 0, onClick = { selectedMethodIndex = 0 }, label = { Text("Tip Speed (k=1)", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                            FilterChip(selected = selectedMethodIndex == 1, onClick = { selectedMethodIndex = 1 }, label = { Text("Power/Vol (k=2/3)", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = selectedMethodIndex == 2, onClick = { selectedMethodIndex = 2 }, label = { Text("Reynolds (k=2)", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                            FilterChip(selected = selectedMethodIndex == 3, onClick = { selectedMethodIndex = 3 }, label = { Text("Custom k", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                        }
                    }
                    if (selectedMethodIndex == 3) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = stirCustomK,
                            onValueChange = { stirCustomK = it },
                            label = { Text("Custom Exponent k") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                EquipmentType.PAN_COATER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = selectedMethodIndex == 0, onClick = { selectedMethodIndex = 0 }, label = { Text("Surface Area (Batch^2/3)", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                        FilterChip(selected = selectedMethodIndex == 1, onClick = { selectedMethodIndex = 1 }, label = { Text("Linear Batch Size", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                    }
                }
                else -> {
                    Surface(color = Slate100, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = currentResult.methodologyName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inputs Card (Source + Targets)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EQUIPMENT PARAMETERS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = PharmaBlueLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "DEMO DATA ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PharmaBlueDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Equipment-Specific Input Fields
                    when (selectedEquipment) {
                        EquipmentType.RMG -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = rmgD1, onValueChange = { rmgD1 = it }, label = { Text("Source D1 (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = rmgN1, onValueChange = { rmgN1 = it }, label = { Text("Source RPM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = rmgD2, onValueChange = { rmgD2 = it }, label = { Text("Target 1 D2 (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = rmgD3, onValueChange = { rmgD3 = it }, label = { Text("Target 2 D3 (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = rmgD4, onValueChange = { rmgD4 = it }, label = { Text("Target 3 D4 (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = rmgBatch1, onValueChange = { rmgBatch1 = it }, label = { Text("Source Batch (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = rmgDensity, onValueChange = { rmgDensity = it }, label = { Text("Bulk Density (g/mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = rmgMaxRpm, onValueChange = { rmgMaxRpm = it }, label = { Text("Max Machine RPM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                        }
                        EquipmentType.BLENDER -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = blendD1, onValueChange = { blendD1 = it }, label = { Text("Source Dia D1 (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = blendN1, onValueChange = { blendN1 = it }, label = { Text("Source RPM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = blendT1, onValueChange = { blendT1 = it }, label = { Text("Source Time (min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = blendD2, onValueChange = { blendD2 = it }, label = { Text("Target Dia D2 (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = blendV2, onValueChange = { blendV2 = it }, label = { Text("Target Vol (L)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                        }
                        EquipmentType.FBP_TOP_SPRAY -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = fbpD1, onValueChange = { fbpD1 = it }, label = { Text("Source Screen (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = fbpAir1, onValueChange = { fbpAir1 = it }, label = { Text("Source Airflow") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = fbpSpray1, onValueChange = { fbpSpray1 = it }, label = { Text("Spray (g/min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = fbpD2, onValueChange = { fbpD2 = it }, label = { Text("Target 1 Screen (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = fbpD3, onValueChange = { fbpD3 = it }, label = { Text("Target 2 Screen (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                        }
                        EquipmentType.FBP_WURSTER -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = wurDia1, onValueChange = { wurDia1 = it }, label = { Text("Source Col Dia (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = wurAir1, onValueChange = { wurAir1 = it }, label = { Text("Source Airflow") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = wurSpray1, onValueChange = { wurSpray1 = it }, label = { Text("Spray (g/min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = wurDia2, onValueChange = { wurDia2 = it }, label = { Text("Target 1 Col Dia") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = wurDia3, onValueChange = { wurDia3 = it }, label = { Text("Target 2 Col Dia") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = wurCols3, onValueChange = { wurCols3 = it }, label = { Text("Target 2 Cols") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                        }
                        EquipmentType.EXTRUDER -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = extD1, onValueChange = { extD1 = it }, label = { Text("Source Screw (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = extQ1, onValueChange = { extQ1 = it }, label = { Text("Source Rate (kg/h)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = extN1, onValueChange = { extN1 = it }, label = { Text("Screw RPM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = extD2, onValueChange = { extD2 = it }, label = { Text("Target 1 Screw (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = extD3, onValueChange = { extD3 = it }, label = { Text("Target 2 Screw (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                        }
                        EquipmentType.ROLLER_COMPACTOR -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = rcD1, onValueChange = { rcD1 = it }, label = { Text("Source Dia (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = rcW1, onValueChange = { rcW1 = it }, label = { Text("Source Width (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = rcForce1, onValueChange = { rcForce1 = it }, label = { Text("Force (kN)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = rcD2, onValueChange = { rcD2 = it }, label = { Text("Target Dia (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = rcW2, onValueChange = { rcW2 = it }, label = { Text("Target Width (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                        }
                        EquipmentType.TABLET_PRESS -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = pressStations1, onValueChange = { pressStations1 = it }, label = { Text("Source Stations") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = pressRpm1, onValueChange = { pressRpm1 = it }, label = { Text("Turret RPM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = pressStations2, onValueChange = { pressStations2 = it }, label = { Text("Target 1 Stations") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = pressRpm2, onValueChange = { pressRpm2 = it }, label = { Text("Target 1 RPM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = pressBatchTabs, onValueChange = { pressBatchTabs = it }, label = { Text("Batch Size (Tabs)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = pressEffPct, onValueChange = { pressEffPct = it }, label = { Text("Efficiency %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                        }
                        else -> {
                            // Default generic parameters for remaining modules
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = panD1, onValueChange = { panD1 = it }, label = { Text("Source Dia (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = panN1, onValueChange = { panN1 = it }, label = { Text("Source Speed (RPM)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = panD2, onValueChange = { panD2 = it }, label = { Text("Target 1 Dia (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = panD3, onValueChange = { panD3 = it }, label = { Text("Target 2 Dia (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calculated Results Header & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CALCULATED PARAMETERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                val worstStatus = currentResult.warnings.maxByOrNull { it.level }?.level ?: ValidationStatus.VALID
                TrafficLightBadge(status = worstStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parameters Table List
            currentResult.parameters.forEach { param ->
                ParameterRow(
                    param = param,
                    onShowFormula = { formula ->
                        showFormulaDialog = formula
                    }
                )
            }

            // Warnings Box
            if (currentResult.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentResult.warnings.forEach { warning ->
                        Surface(
                            color = if (warning.level == ValidationStatus.INVALID) PharmaErrorBg else PharmaCautionBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = if (warning.level == ValidationStatus.INVALID) Icons.Default.Error else Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = if (warning.level == ValidationStatus.INVALID) PharmaError else PharmaCaution,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = warning.message,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (warning.level == ValidationStatus.INVALID) PharmaError else PharmaCaution
                                    )
                                    if (warning.recommendation.isNotEmpty()) {
                                        Text(
                                            text = "Recommendation: ${warning.recommendation}",
                                            fontSize = 10.sp,
                                            color = Slate700
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            calculationRepository?.addHistory(
                                CalculationHistoryEntity(
                                    equipmentType = selectedEquipment.shortCode,
                                    title = "${selectedEquipment.displayName} Scale-Up",
                                    scaleSummary = "Calculated across ${currentResult.parameters.size} parameters",
                                    keyResult = currentResult.parameters.firstOrNull()?.let { "${it.name}: ${it.target1Value} ${it.unit}" } ?: "Completed"
                                )
                            )
                            snackbarHostState.showSnackbar("Calculation saved to history successfully!")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_save_calculation")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Calculation")
                }

                OutlinedButton(
                    onClick = {
                        val csv = ReportGenerators.generateCsv(context, null, listOf(currentResult))
                        if (csv != null) {
                            ReportGenerators.shareFile(context, csv, "text/csv")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_export_csv")
                ) {
                    Icon(imageVector = Icons.Default.TableChart, contentDescription = "CSV", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Formula Breakdown Dialog
    showFormulaDialog?.let { formula ->
        FormulaDetailDialog(
            formula = formula,
            onDismiss = { showFormulaDialog = null }
        )
    }
}
