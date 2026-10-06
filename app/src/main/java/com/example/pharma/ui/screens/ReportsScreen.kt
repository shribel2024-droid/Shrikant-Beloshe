package com.example.pharma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pharma.data.db.ProjectEntity
import com.example.pharma.data.repository.ProjectRepository
import com.example.pharma.domain.engine.CalculationEngine
import com.example.pharma.domain.model.ScaleUpResult
import com.example.pharma.ui.components.DisclaimerCard
import com.example.pharma.util.export.ReportGenerators
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    projectRepository: ProjectRepository?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val projects by projectRepository?.allProjects?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    var selectedProject by remember { mutableStateOf<ProjectEntity?>(null) }

    LaunchedEffect(projects) {
        if (selectedProject == null && projects.isNotEmpty()) {
            selectedProject = projects.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calculation Reports",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PharmaBluePrimary)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SELECT PROJECT TO REPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                    Spacer(modifier = Modifier.height(6.dp))

                    var projExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = projExpanded,
                        onExpandedChange = { projExpanded = !projExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProject?.projectName ?: "Select or create a project",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = projExpanded,
                            onDismissRequest = { projExpanded = false }
                        ) {
                            projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.projectName) },
                                    onClick = {
                                        selectedProject = proj
                                        projExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    selectedProject?.let { proj ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Product: ${proj.productName} (${proj.productCode})", fontSize = 12.sp, color = Slate700)
                        Text("Dosage Form: ${proj.dosageForm} | Process: ${proj.manufacturingProcess}", fontSize = 11.sp, color = Slate600)
                        Text("Target Batch Size: ${proj.batchSizeKg} kg | Prepared By: ${proj.preparedBy}", fontSize = 11.sp, color = Slate500)
                    }
                }
            }

            // Report Actions Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("EXPORT FORMATS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)

                    Button(
                        onClick = {
                            // Compute sample suite of results for report
                            val rmgRes = CalculationEngine.calculateRmg(
                                CalculationEngine.RmgInput(
                                    170.0, 500.0, 4.0, 2.5, 1.5, 0.55, 1200.0,
                                    listOf(CalculationEngine.RmgTargetInput("Commercial P300", 680.0, 300.0, 200.0, 150.0))
                                )
                            )
                            val blendRes = CalculationEngine.calculateBlender(
                                CalculationEngine.BlenderInput(
                                    sourceVolumeL = 400.0, sourceWorkingL = 240.0, sourceDiameterM = 1.0, sourceRpm = 25.0,
                                    sourceBlendTimeMin = 15.0, bulkDensityGml = 0.55, sourceBatchKg = 120.0,
                                    targets = listOf(CalculationEngine.BlenderTargetInput("Commercial PM1200", 1200.0, 2.5))
                                )
                            )
                            val pdf = ReportGenerators.generatePdf(context, selectedProject, listOf(rmgRes, blendRes))
                            if (pdf != null) {
                                ReportGenerators.shareFile(context, pdf, "application/pdf")
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Failed to generate PDF") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_generate_pdf")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate & Share PDF Report")
                    }

                    OutlinedButton(
                        onClick = {
                            val rmgRes = CalculationEngine.calculateRmg(
                                CalculationEngine.RmgInput(
                                    170.0, 500.0, 4.0, 2.5, 1.5, 0.55, 1200.0,
                                    listOf(CalculationEngine.RmgTargetInput("Commercial P300", 680.0, 300.0, 200.0, 150.0))
                                )
                            )
                            val csv = ReportGenerators.generateCsv(context, selectedProject, listOf(rmgRes))
                            if (csv != null) {
                                ReportGenerators.shareFile(context, csv, "text/csv")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_export_csv_report")
                    ) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = "CSV")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Excel-Compatible CSV")
                    }
                }
            }

            DisclaimerCard()
        }
    }
}
