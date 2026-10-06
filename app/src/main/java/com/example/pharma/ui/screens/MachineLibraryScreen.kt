package com.example.pharma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.window.Dialog
import com.example.pharma.data.db.MachineLibraryEntity
import com.example.pharma.data.repository.MachineRepository
import com.example.pharma.domain.model.EquipmentType
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineLibraryScreen(
    machineRepository: MachineRepository?,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val allMachines by machineRepository?.allMachines?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredMachines = remember(allMachines, selectedFilter, searchQuery) {
        allMachines.filter { machine ->
            (selectedFilter == null || machine.equipmentType == selectedFilter) &&
            (searchQuery.isBlank() || machine.manufacturer.contains(searchQuery, true) || machine.model.contains(searchQuery, true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Equipment Library",
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
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Machine", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PharmaBluePrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PharmaBluePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_machine")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Machine")
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
                placeholder = { Text("Search by manufacturer or model...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All (${allMachines.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "RMG",
                    onClick = { selectedFilter = if (selectedFilter == "RMG") null else "RMG" },
                    label = { Text("RMG", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "BLENDER",
                    onClick = { selectedFilter = if (selectedFilter == "BLENDER") null else "BLENDER" },
                    label = { Text("Blender", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "PAN_COATER",
                    onClick = { selectedFilter = if (selectedFilter == "PAN_COATER") null else "PAN_COATER" },
                    label = { Text("Coater", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredMachines) { machine ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth().testTag("machine_item_${machine.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${machine.manufacturer} ${machine.model}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = PharmaBlueLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = machine.equipmentType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PharmaBlueDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (machine.capacityL != null) {
                                    Text("Capacity: ${machine.capacityL} L", fontSize = 11.sp, color = Slate600)
                                }
                                if (machine.diameterMm != null) {
                                    Text("Diameter: ${machine.diameterMm} mm", fontSize = 11.sp, color = Slate600)
                                }
                                if (machine.maxRpm != null) {
                                    Text("Max: ${machine.maxRpm} RPM", fontSize = 11.sp, color = Slate600)
                                }
                                if (machine.stations != null) {
                                    Text("Stations: ${machine.stations}", fontSize = 11.sp, color = Slate600)
                                }
                            }

                            if (machine.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = machine.notes, fontSize = 11.sp, color = Slate500)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Machine Dialog
    if (showAddDialog) {
        AddMachineDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newMachine ->
                coroutineScope.launch {
                    machineRepository?.insert(newMachine)
                    showAddDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMachineDialog(
    onDismiss: () -> Unit,
    onSave: (MachineLibraryEntity) -> Unit
) {
    var manufacturer by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var equipType by remember { mutableStateOf("RMG") }
    var capacity by remember { mutableStateOf("") }
    var diameter by remember { mutableStateOf("") }
    var maxRpm by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Plant Equipment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = manufacturer, onValueChange = { manufacturer = it }, label = { Text("Manufacturer *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model Name / ID *") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Capacity (L)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = diameter, onValueChange = { diameter = it }, label = { Text("Diameter (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = maxRpm, onValueChange = { maxRpm = it }, label = { Text("Max RPM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Plant / Suite Notes") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (manufacturer.isNotBlank() && model.isNotBlank()) {
                            onSave(
                                MachineLibraryEntity(
                                    manufacturer = manufacturer,
                                    model = model,
                                    equipmentType = equipType,
                                    capacityL = capacity.toDoubleOrNull(),
                                    diameterMm = diameter.toDoubleOrNull(),
                                    maxRpm = maxRpm.toDoubleOrNull(),
                                    notes = notes,
                                    isCustom = true
                                )
                            )
                        }
                    }) {
                        Text("Add Machine")
                    }
                }
            }
        }
    }
}
