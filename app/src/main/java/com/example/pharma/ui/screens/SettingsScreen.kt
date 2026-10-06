package com.example.pharma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pharma.ui.components.DisclaimerCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var companyName by remember { mutableStateOf("Global Pharma Technologies Ltd.") }
    var defaultPreparer by remember { mutableStateOf("Senior Formulation Scientist") }
    var selectedDecimals by remember { mutableIntStateOf(2) }
    var defaultUnitSystem by remember { mutableStateOf("Metric (SI)") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ORGANIZATION & USER DEFAULTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company / Facility Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = defaultPreparer,
                        onValueChange = { defaultPreparer = it },
                        label = { Text("Default Preparer / Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CALCULATION PRECISION & UNITS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)

                    Text("Decimal Places Displayed:", fontSize = 12.sp, color = Slate700)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3).forEach { dec ->
                            FilterChip(
                                selected = selectedDecimals == dec,
                                onClick = { selectedDecimals = dec },
                                label = { Text("$dec Decimals") }
                            )
                        }
                    }

                    Text("Unit Standard:", fontSize = 12.sp, color = Slate700)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Metric (SI)", "Industrial Pharma").forEach { sys ->
                            FilterChip(
                                selected = defaultUnitSystem == sys,
                                onClick = { defaultUnitSystem = sys },
                                label = { Text(sys) }
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ABOUT PHARMA OSD SCALE-UP CALCULATOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                    Text("Version: 1.0.0 (Native Android Engineering Suite)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Built for Formulation R&D, Tech Transfer, Process Validation, and Manufacturing Engineering.", fontSize = 11.sp, color = Slate600)
                }
            }

            DisclaimerCard()

            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Settings saved successfully!")
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_save_settings")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Preferences")
            }
        }
    }
}
