package com.example.pharma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pharma.data.db.ProjectEntity
import com.example.pharma.data.repository.ProjectRepository
import com.example.pharma.domain.model.DosageForm
import com.example.pharma.domain.model.ManufacturingProcess
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    projectRepository: ProjectRepository?,
    onProjectCreated: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var projectName by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var productCode by remember { mutableStateOf("") }
    var apiName by remember { mutableStateOf("") }
    var selectedDosageForm by remember { mutableStateOf(DosageForm.FILM_COATED_TABLET) }
    var selectedProcess by remember { mutableStateOf(ManufacturingProcess.WET_GRANULATION) }
    var batchSizeKg by remember { mutableStateOf("150") }
    var sourceScale by remember { mutableStateOf("Lab Scale (5 L / 2.5 kg)") }
    var targetScale by remember { mutableStateOf("Commercial Scale (300 L / 150 kg)") }
    var site by remember { mutableStateOf("Tech Transfer Facility B") }
    var preparedBy by remember { mutableStateOf("Formulation Scientist") }
    var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var comments by remember { mutableStateOf("Scale-up validation assessment for regulatory dossier.") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Scale-Up Project",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PharmaBluePrimary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PROJECT DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)

                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_project_name")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = productName,
                            onValueChange = { productName = it },
                            label = { Text("Product Name *") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = productCode,
                            onValueChange = { productCode = it },
                            label = { Text("Product Code") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = apiName,
                        onValueChange = { apiName = it },
                        label = { Text("Active Pharmaceutical Ingredient (API)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dosage Form Selector
                    var dfExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = dfExpanded,
                        onExpandedChange = { dfExpanded = !dfExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDosageForm.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dosage Form") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dfExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dfExpanded,
                            onDismissRequest = { dfExpanded = false }
                        ) {
                            DosageForm.values().forEach { df ->
                                DropdownMenuItem(
                                    text = { Text(df.displayName) },
                                    onClick = {
                                        selectedDosageForm = df
                                        dfExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Process Selector
                    var mpExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = mpExpanded,
                        onExpandedChange = { mpExpanded = !mpExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProcess.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Manufacturing Process") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mpExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = mpExpanded,
                            onDismissRequest = { mpExpanded = false }
                        ) {
                            ManufacturingProcess.values().forEach { mp ->
                                DropdownMenuItem(
                                    text = { Text(mp.displayName) },
                                    onClick = {
                                        selectedProcess = mp
                                        mpExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = batchSizeKg,
                        onValueChange = { batchSizeKg = it },
                        label = { Text("Target Batch Size (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TRANSFER PARAMETERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)

                    OutlinedTextField(value = sourceScale, onValueChange = { sourceScale = it }, label = { Text("Source Scale") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = targetScale, onValueChange = { targetScale = it }, label = { Text("Target Scale") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = site, onValueChange = { site = it }, label = { Text("Manufacturing Site") }, modifier = Modifier.fillMaxWidth())

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = preparedBy, onValueChange = { preparedBy = it }, label = { Text("Prepared By") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        label = { Text("Comments / Regulatory Reference") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = PharmaError, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    if (projectName.isBlank()) {
                        errorMessage = "Please enter a Project Name"
                        return@Button
                    }
                    val project = ProjectEntity(
                        projectName = projectName,
                        productName = if (productName.isBlank()) projectName else productName,
                        productCode = productCode,
                        apiName = apiName,
                        dosageForm = selectedDosageForm.displayName,
                        manufacturingProcess = selectedProcess.displayName,
                        batchSizeKg = batchSizeKg.toDoubleOrNull() ?: 100.0,
                        sourceScaleName = sourceScale,
                        targetScaleName = targetScale,
                        manufacturingSite = site,
                        preparedBy = preparedBy,
                        date = dateStr,
                        comments = comments,
                        selectedEquipmentJson = "[\"RMG\", \"FBP_TOP_SPRAY\", \"BLENDER\", \"TABLET_PRESS\"]"
                    )
                    coroutineScope.launch {
                        val id = projectRepository?.insert(project) ?: 1L
                        onProjectCreated(id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_create_project")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Create")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Project & Launch Process Map")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedProjectsScreen(
    projectRepository: ProjectRepository?,
    onOpenProject: (Long) -> Unit,
    onNewProject: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val projects by projectRepository?.allProjects?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else projects.filter {
            it.projectName.contains(searchQuery, ignoreCase = true) ||
            it.productName.contains(searchQuery, ignoreCase = true) ||
            it.apiName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Projects",
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
                actions = {
                    IconButton(onClick = onNewProject) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Project", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PharmaBluePrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewProject,
                containerColor = PharmaBluePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_project")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Project")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search projects, products, API...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No projects found", color = Slate500, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredProjects) { proj ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProject(proj.id) }
                                .testTag("project_item_${proj.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = proj.projectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = PharmaBlueLight,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = proj.dosageForm,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PharmaBlueDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Product: ${proj.productName} | API: ${proj.apiName.ifEmpty { "N/A" }}",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                                Text(
                                    text = "Process: ${proj.manufacturingProcess} | Target: ${proj.batchSizeKg} kg",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "By ${proj.preparedBy} • ${proj.date}",
                                        fontSize = 10.sp,
                                        color = Slate400
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    projectRepository?.delete(proj)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Slate400, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
