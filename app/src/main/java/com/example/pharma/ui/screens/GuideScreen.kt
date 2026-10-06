package com.example.pharma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class GuideTopic(
    val title: String,
    val purpose: String,
    val formulaText: String,
    val basis: String,
    val assumptions: String,
    val validationNotes: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onNavigateBack: () -> Unit) {
    val topics = listOf(
        GuideTopic(
            title = "1. Rapid Mixer Granulator (RMG)",
            purpose = "Wet massing, binder distribution, and granule growth under high shear.",
            formulaText = "Constant Tip Speed: N2 = N1 * (D1 / D2)\nConstant Froude: N2 = N1 * sqrt(D1 / D2)\nTip Speed v = pi * D * N / 60\nFroude Fr = N^2 * D / g",
            basis = "Kinetic energy vs centrifugal force balance. Froude preserves vortex shape and bed turnover, while tip speed preserves maximum shear at the blade periphery.",
            assumptions = "Geometrically similar impeller/bowl, 30-70% fill ratio, consistent binder addition duration.",
            validationNotes = "Verify chopper speed, wet massing endpoint via torque/power curve, and drying shrinkage."
        ),
        GuideTopic(
            title = "2. Tumble & Bin Blenders",
            purpose = "Uniform powder blending, pre-mix, and lubricant addition.",
            formulaText = "Constant Froude: N2 = N1 * sqrt(D1 / D2)\nBlend Time: t2 = (N1 * t1) / N2\nTotal Revolutions = N * t = constant",
            basis = "Powder cascading dynamics under gravity. Preserving total revolutions maintains identical particulate shear encounters.",
            assumptions = "Fill ratio held strictly between 50% and 60% of total vessel volume to allow free void space.",
            validationNotes = "Evaluate blend uniformity across 10 stratified sampling locations; avoid lubricant over-blending."
        ),
        GuideTopic(
            title = "3. Fluid Bed Processor — Top Spray",
            purpose = "Fluidized bed agglomeration, binder drying, and moisture removal.",
            formulaText = "Scale Factor: SF = (D2 / D1)^2\nAirflow2 = Airflow1 * SF\nSpray Rate2 = Spray Rate1 * SF",
            basis = "Maintains constant superficial fluidization air velocity across distributor screen cross-sectional area.",
            assumptions = "Inlet air psychrometric properties (temperature, absolute humidity) remain identical.",
            validationNotes = "Check spray pattern overlap in multi-nozzle arrays and monitor product bed differential pressure."
        ),
        GuideTopic(
            title = "4. Fluid Bed Processor — Bottom Spray / Wurster",
            purpose = "Precision pellet layering, functional membrane barrier coating, and sustained-release pellets.",
            formulaText = "SF = Target Wurster Area / Source Wurster Area\nAirflow2 = Airflow1 * SF; Spray Rate2 = Spray Rate1 * SF\nGap2 = Gap1 * (D2 / D1)",
            basis = "Column air superficial velocity inside Wurster partition tube.",
            assumptions = "Individual Wurster column fluid mechanics replicate across multi-tube clusters.",
            validationNotes = "Optimize partition gap to prevent dead zones or pellet attrition; verify vendor gap tolerances."
        ),
        GuideTopic(
            title = "5. Screw Extruder",
            purpose = "Continuous wet mass densification and plastic cylindrical strand production.",
            formulaText = "Cube Rule: Q2 = Q1 * (D2 / D1)^3\nHeat-Limited: Q2 = Q1 * (D2 / D1)^2.5\nResidence Time tau ~ V / Q",
            basis = "Drag flow volumetric capacity vs thermal dissipation boundary layer.",
            assumptions = "Constant L/D screw configuration and temperature profile across barrel zones.",
            validationNotes = "Verify exit product temperature and die pressure to prevent API degradation."
        ),
        GuideTopic(
            title = "6. Spheronizer",
            purpose = "Break cylindrical extrudates into uniform spherical beads/pellets.",
            formulaText = "N2 = N1 * (D1 / D2)\nLoad2 = Load1 * (D2 / D1)^2",
            basis = "Constant peripheral friction plate velocity and area-proportional bed mass.",
            assumptions = "Groove pitch geometry matches extrudate diameter; spheronization time ~1-5 min is constant.",
            validationNotes = "Evaluate aspect ratio (circularity), particle size distribution (PSD), and friability."
        ),
        GuideTopic(
            title = "7. Perforated Pan Coater",
            purpose = "Aqueous and solvent-based film coating for aesthetics, taste masking, or enteric release.",
            formulaText = "N2 = N1 * (D1 / D2)\nSpray Rate2 = Spray Rate1 * (Batch2 / Batch1)^(2/3)\nExhaust Airflow2 = Airflow1 * SF",
            basis = "Linear peripheral rim speed and 2/3 exponent presentation surface area of tablet bed.",
            assumptions = "Brim volume fill maintained at 60-80% of perforated pan capacity.",
            validationNotes = "Balance thermodynamic drying capacity (exhaust air RH) to eliminate overwetting/sticking."
        ),
        GuideTopic(
            title = "8. Stirrer / Liquid Agitator",
            purpose = "Coating suspension, binder solution, and emulsion preparation.",
            formulaText = "N2 = N1 * (D1 / D2)^k\nk = 1.0 (Tip Speed), k = 2/3 (Equal P/V), k = 2.0 (Reynolds)",
            basis = "Dynamic shear, turbulent power dissipation, or laminar mixing similarity.",
            assumptions = "Geometric similarity: Impeller-to-tank diameter (D/T) and liquid depth (H/T) constant.",
            validationNotes = "Avoid vortex aeration when dissolving foaming polymers (e.g. HPMC, PVA)."
        ),
        GuideTopic(
            title = "9. Roller Compactor",
            purpose = "Dry granulation of moisture- or heat-sensitive active ingredients.",
            formulaText = "SCF = Force / Width (kN/cm)\nForce2 = SCF * Width2\nRoll Speed: N2 = N1 * (D1 / D2)",
            basis = "Specific Compaction Force (SCF) invariance preserves ribbon solid fraction and porosity.",
            assumptions = "Screw-to-roll ratio maintained to deliver steady pre-compaction feed.",
            validationNotes = "Measure ribbon tensile strength and fines fraction after dry milling."
        ),
        GuideTopic(
            title = "10. Multimill & 11. Quadro Comil",
            purpose = "Delumping, sizing wet granules, and post-drying sizing.",
            formulaText = "Constant Tip Speed: N2 = N1 * (D1 / D2)\nThroughput: Q2 = Q1 * (D2 / D1)^2",
            basis = "Kinetic impact velocity and cylindrical screen shearing area.",
            assumptions = "Screen perforation diameter and impeller-to-screen clearance held constant.",
            validationNotes = "Confirm sieve analysis (D10, D50, D90) does not shift at commercial throughput."
        ),
        GuideTopic(
            title = "12. Rotary Tablet Compression Press",
            purpose = "High-speed compaction of granules into precision-dosed tablets.",
            formulaText = "Theo Output/hr = Stations * RPM * 60 * tablets_per_rev\nEff Output = Theo * (Eff% / 100) * (Util% / 100) * (1 - Reject%)",
            basis = "Turret pitch velocity and compression dwell time (t_dwell = punch flat diameter / pitch velocity).",
            assumptions = "Dwell time remains above minimum critical threshold (~10-15 ms) to eliminate capping.",
            validationNotes = "Evaluate weight uniformity, breaking force (hardness), friability, and dissolution."
        ),
        GuideTopic(
            title = "13 & 14. Capsule Filler (Powder & Pellets)",
            purpose = "Volumetric or dosator encapsulation of dry powders, granules, or coated pellets.",
            formulaText = "Theo Output/hr = Cycles/min * Stations * 60\nEff Output = Theo * (Eff% / 100)\nBatch Run Time = Total Capsules / Eff Output",
            basis = "Continuous or intermittent segment motion filling cycles.",
            assumptions = "Dosing disk depth or pellet cavity volume calibrated for formulation tapped density.",
            validationNotes = "Confirm capsule weight variation (RSD < 3%) and lock length inspection."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scale-Up Technical Guide", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PharmaBluePrimary)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(color = Slate100, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("SCALE-UP PRINCIPLES & GOVERNING EQUATIONS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "This guide summarizes the governing scale-up physics, dimensional analysis invariants, and ICH Q8 pharmaceutical quality-by-design guidelines implemented in this calculator.",
                            fontSize = 11.sp,
                            color = Slate600,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            items(topics) { topic ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topic.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                contentDescription = "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = topic.purpose, fontSize = 11.sp, color = Slate600)

                        if (expanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(color = Slate100, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = topic.formulaText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = PharmaBlueDark,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Scale-Up Basis:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Slate500)
                            Text(topic.basis, fontSize = 11.sp, color = Slate700)

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Important Assumptions:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Slate500)
                            Text(topic.assumptions, fontSize = 11.sp, color = Slate700)

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Validation Considerations:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Slate500)
                            Text(topic.validationNotes, fontSize = 11.sp, color = Slate700)
                        }
                    }
                }
            }
        }
    }
}
