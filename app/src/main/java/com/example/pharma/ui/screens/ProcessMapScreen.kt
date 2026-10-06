package com.example.pharma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pharma.domain.model.DosageForm
import com.example.pharma.domain.model.EquipmentType
import com.example.pharma.domain.model.ManufacturingProcess
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessMapScreen(
    onNavigateToEquipment: (EquipmentType) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedDosageForm by remember { mutableStateOf(DosageForm.FILM_COATED_TABLET) }
    var selectedProcess by remember { mutableStateOf(ManufacturingProcess.WET_GRANULATION) }

    // Equipment sequence based on dosage form & process
    val defaultEquipmentSequence = remember(selectedDosageForm, selectedProcess) {
        when (selectedProcess) {
            ManufacturingProcess.DIRECT_COMPRESSION -> {
                mutableStateListOf(EquipmentType.BLENDER, EquipmentType.MULTIMILL, EquipmentType.TABLET_PRESS).apply {
                    if (selectedDosageForm == DosageForm.FILM_COATED_TABLET) add(EquipmentType.PAN_COATER)
                }
            }
            ManufacturingProcess.DRY_GRANULATION -> {
                mutableStateListOf(EquipmentType.BLENDER, EquipmentType.ROLLER_COMPACTOR, EquipmentType.QUADRO_MILL, EquipmentType.BLENDER, EquipmentType.TABLET_PRESS).apply {
                    if (selectedDosageForm == DosageForm.FILM_COATED_TABLET) add(EquipmentType.PAN_COATER)
                }
            }
            ManufacturingProcess.WET_GRANULATION -> {
                mutableStateListOf(EquipmentType.RMG, EquipmentType.QUADRO_MILL, EquipmentType.FBP_TOP_SPRAY, EquipmentType.BLENDER, EquipmentType.TABLET_PRESS).apply {
                    if (selectedDosageForm == DosageForm.FILM_COATED_TABLET) add(EquipmentType.PAN_COATER)
                }
            }
            ManufacturingProcess.FLUID_BED_GRANULATION -> {
                mutableStateListOf(EquipmentType.FBP_TOP_SPRAY, EquipmentType.QUADRO_MILL, EquipmentType.BLENDER, EquipmentType.TABLET_PRESS).apply {
                    if (selectedDosageForm == DosageForm.FILM_COATED_TABLET) add(EquipmentType.PAN_COATER)
                }
            }
            ManufacturingProcess.EXTRUSION_SPHERONIZATION -> {
                mutableStateListOf(EquipmentType.RMG, EquipmentType.EXTRUDER, EquipmentType.SPHERONIZER, EquipmentType.FBP_TOP_SPRAY, EquipmentType.FBP_WURSTER, EquipmentType.CAPSULE_PELLETS)
            }
            ManufacturingProcess.WURSTER_COATING -> {
                mutableStateListOf(EquipmentType.STIRRER, EquipmentType.FBP_WURSTER, EquipmentType.CAPSULE_PELLETS)
            }
        }
    }

    var equipmentList by remember(defaultEquipmentSequence) {
        mutableStateOf(defaultEquipmentSequence.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OSD Process Map",
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
                .padding(16.dp)
        ) {
            // Configuration Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. DOSAGE FORM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    var dosageExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = dosageExpanded,
                        onExpandedChange = { dosageExpanded = !dosageExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDosageForm.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dosageExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = dosageExpanded,
                            onDismissRequest = { dosageExpanded = false }
                        ) {
                            DosageForm.values().forEach { df ->
                                DropdownMenuItem(
                                    text = { Text(df.displayName, fontSize = 13.sp) },
                                    onClick = {
                                        selectedDosageForm = df
                                        dosageExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "2. MANUFACTURING PROCESS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    var procExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = procExpanded,
                        onExpandedChange = { procExpanded = !procExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProcess.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = procExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = procExpanded,
                            onDismissRequest = { procExpanded = false }
                        ) {
                            ManufacturingProcess.values().forEach { mp ->
                                DropdownMenuItem(
                                    text = { Text(mp.displayName, fontSize = 13.sp) },
                                    onClick = {
                                        selectedProcess = mp
                                        procExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUGGESTED EQUIPMENT TRAIN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate600
                )
                Text(
                    text = "${equipmentList.size} Steps",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sequence Flow List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(equipmentList) { index, equipment ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToEquipment(equipment) }
                            .testTag("process_node_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PharmaBlueLight,
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        color = PharmaBlueDark,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = equipment.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Category: ${equipment.category} | Tap to Calculate",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }

                            IconButton(
                                onClick = {
                                    equipmentList = equipmentList.toMutableList().apply { removeAt(index) }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove step",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Calculate",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (index < equipmentList.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Next step",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add Step Button
            var addMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { addMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Step")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Unit Operation / Equipment")
                }
                DropdownMenu(
                    expanded = addMenuExpanded,
                    onDismissRequest = { addMenuExpanded = false }
                ) {
                    EquipmentType.values().forEach { eq ->
                        DropdownMenuItem(
                            text = { Text(eq.displayName) },
                            onClick = {
                                equipmentList = equipmentList + eq
                                addMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
