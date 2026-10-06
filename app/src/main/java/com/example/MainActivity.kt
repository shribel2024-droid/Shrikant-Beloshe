package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.pharma.data.db.AppDatabase
import com.example.pharma.data.repository.CalculationRepository
import com.example.pharma.data.repository.MachineRepository
import com.example.pharma.data.repository.ProjectRepository
import com.example.pharma.domain.model.EquipmentType
import com.example.pharma.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PharmaBluePrimary

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Projects : Screen("projects")
    object NewProject : Screen("new_project")
    object Calculator : Screen("calculator")
    object ProcessMap : Screen("process_map")
    object MachineLibrary : Screen("machine_library")
    object Reports : Screen("reports")
    object AiAssistant : Screen("ai_assistant")
    object Guide : Screen("guide")
    object Settings : Screen("settings")
}

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var projectRepository: ProjectRepository
    private lateinit var machineRepository: MachineRepository
    private lateinit var calculationRepository: CalculationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(this, lifecycleScope)
        projectRepository = ProjectRepository(database.projectDao())
        machineRepository = MachineRepository(database.machineDao())
        calculationRepository = CalculationRepository(database.calculationDao(), database.historyDao())

        setContent {
            MyApplicationTheme {
                MainAppNavHost(
                    projectRepository = projectRepository,
                    machineRepository = machineRepository,
                    calculationRepository = calculationRepository
                )
            }
        }
    }
}

@Composable
fun MainAppNavHost(
    projectRepository: ProjectRepository,
    machineRepository: MachineRepository,
    calculationRepository: CalculationRepository
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedEquipmentForCalc by remember { mutableStateOf(EquipmentType.RMG) }

    // Collect counts for dashboard
    val projects by projectRepository.allProjects.collectAsState(initial = emptyList())
    val machines by machineRepository.allMachines.collectAsState(initial = emptyList())

    // Back button handling
    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    val isTopLevelScreen = currentScreen in listOf(
        Screen.Home, Screen.Projects, Screen.Calculator, Screen.MachineLibrary, Screen.Reports
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Projects,
                        onClick = { currentScreen = Screen.Projects },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") },
                        label = { Text("Projects", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_projects")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Calculator,
                        onClick = { currentScreen = Screen.Calculator },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculator") },
                        label = { Text("Calculator", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_calculator")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.MachineLibrary,
                        onClick = { currentScreen = Screen.MachineLibrary },
                        icon = { Icon(Icons.Default.PrecisionManufacturing, contentDescription = "Equipment") },
                        label = { Text("Equipment", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_equipment")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Reports,
                        onClick = { currentScreen = Screen.Reports },
                        icon = { Icon(Icons.Default.Description, contentDescription = "Reports") },
                        label = { Text("Reports", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_reports")
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onNavigate = { route ->
                        when (route) {
                            "calculator" -> currentScreen = Screen.Calculator
                            "process_map" -> currentScreen = Screen.ProcessMap
                            "new_project" -> currentScreen = Screen.NewProject
                            "projects" -> currentScreen = Screen.Projects
                            "machine_library" -> currentScreen = Screen.MachineLibrary
                            "reports" -> currentScreen = Screen.Reports
                            "ai_assistant" -> currentScreen = Screen.AiAssistant
                            "guide" -> currentScreen = Screen.Guide
                            "settings" -> currentScreen = Screen.Settings
                        }
                    },
                    projectCount = projects.size,
                    machineCount = machines.size
                )
                Screen.Calculator -> CalculatorScreen(
                    initialEquipment = selectedEquipmentForCalc,
                    calculationRepository = calculationRepository,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.ProcessMap -> ProcessMapScreen(
                    onNavigateToEquipment = { eq ->
                        selectedEquipmentForCalc = eq
                        currentScreen = Screen.Calculator
                    },
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.Projects -> SavedProjectsScreen(
                    projectRepository = projectRepository,
                    onOpenProject = {
                        currentScreen = Screen.Reports
                    },
                    onNewProject = { currentScreen = Screen.NewProject },
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.NewProject -> NewProjectScreen(
                    projectRepository = projectRepository,
                    onProjectCreated = {
                        currentScreen = Screen.ProcessMap
                    },
                    onNavigateBack = { currentScreen = Screen.Projects }
                )
                Screen.MachineLibrary -> MachineLibraryScreen(
                    machineRepository = machineRepository,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.Reports -> ReportsScreen(
                    projectRepository = projectRepository,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.AiAssistant -> AiAssistantScreen(
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.Guide -> GuideScreen(
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.Settings -> SettingsScreen(
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
