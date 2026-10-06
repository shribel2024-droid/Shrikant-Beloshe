package com.example.pharma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pharma.ui.components.DisclaimerCard
import com.example.ui.theme.*

data class DashboardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val badge: String? = null,
    val color: Color = PharmaBluePrimary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    projectCount: Int = 1,
    machineCount: Int = 20
) {
    val dashboardItems = listOf(
        DashboardItem("Scale-Up Calculator", "14 OSD equipment modules", Icons.Default.Calculate, "calculator", "14 Types", PharmaBluePrimary),
        DashboardItem("Process Map", "Dosage form equipment train", Icons.Default.AccountTree, "process_map", "Smart Flow", PharmaCyanAccent),
        DashboardItem("New Project", "Setup technical transfer", Icons.Default.AddCircle, "new_project", null, PharmaTeal),
        DashboardItem("Saved Projects", "Manage formulation projects", Icons.Default.Folder, "projects", "$projectCount Saved", PharmaBluePrimary),
        DashboardItem("Equipment Library", "Plant machines & dimensions", Icons.Default.PrecisionManufacturing, "machine_library", "$machineCount Models", Slate700),
        DashboardItem("Reports & Export", "PDF & CSV calculation reports", Icons.Default.Description, "reports", "PDF / CSV", PharmaSuccess),
        DashboardItem("AI Tech Transfer", "Gemini Assistant, TTS & Vision", Icons.Default.AutoAwesome, "ai_assistant", "AI Suite", Color(0xFF6B21A8)),
        DashboardItem("Scale-Up Guide", "Reference equations & logic", Icons.AutoMirrored.Filled.MenuBook, "guide", null, Slate600),
        DashboardItem("Settings", "Units, precision & company", Icons.Default.Settings, "settings", null, Slate500)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pharma OSD Scale-Up Calculator",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Formulation R&D | Tech Transfer | Process Engineering | PMO",
                            fontSize = 10.sp,
                            color = PharmaBlueLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PharmaBluePrimary
                ),
                actions = {
                    IconButton(
                        onClick = { onNavigate("ai_assistant") },
                        modifier = Modifier.testTag("home_ai_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Hero Status Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pharmaceutical Engineering Suite",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Scale-up invariant preservation across Lab, Pilot & Commercial scales",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickMetric(label = "Modules", value = "14", color = PharmaBluePrimary, modifier = Modifier.weight(1f))
                        QuickMetric(label = "Projects", value = "$projectCount", color = PharmaTeal, modifier = Modifier.weight(1f))
                        QuickMetric(label = "Library", value = "$machineCount", color = PharmaCyanAccent, modifier = Modifier.weight(1f))
                        QuickMetric(label = "Validation", value = "ICH Q8", color = PharmaSuccess, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mandatory Disclaimer
            DisclaimerCard(modifier = Modifier.testTag("home_disclaimer_card"))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ENGINEERING MODULES & TOOLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Grid Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dashboardItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            DashboardCard(
                                item = item,
                                onClick = { onNavigate(item.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuickMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Slate100,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Slate600
            )
        }
    }
}

@Composable
fun DashboardCard(
    item: DashboardItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .height(115.dp)
            .clickable(onClick = onClick)
            .testTag("card_${item.route}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = item.color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = item.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (item.badge != null) {
                    Surface(
                        color = PharmaBlueLight,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = item.badge,
                            color = PharmaBlueDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
                Text(
                    text = item.subtitle,
                    fontSize = 10.sp,
                    color = Slate500,
                    maxLines = 1
                )
            }
        }
    }
}
