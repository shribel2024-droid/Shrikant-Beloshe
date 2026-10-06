package com.example.pharma.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pharma.domain.model.FormulaDetail
import com.example.pharma.domain.model.ScaleParameter
import com.example.pharma.domain.model.ValidationStatus
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun TrafficLightBadge(
    status: ValidationStatus,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val (bgColor, textColor, icon) = when (status) {
        ValidationStatus.VALID -> Triple(PharmaSuccessBg, PharmaSuccess, Icons.Default.CheckCircle)
        ValidationStatus.CAUTION -> Triple(PharmaCautionBg, PharmaCaution, Icons.Default.Warning)
        ValidationStatus.INVALID -> Triple(PharmaErrorBg, PharmaError, Icons.Default.Error)
    }

    val displayLabel = label ?: when (status) {
        ValidationStatus.VALID -> "Acceptable"
        ValidationStatus.CAUTION -> "Engineering Review"
        ValidationStatus.INVALID -> "Out of Spec / Limit"
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = displayLabel,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayLabel,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DisclaimerCard(
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate100),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Regulatory Notice",
                tint = Slate600,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Calculated values are engineering scale-up estimates and must be verified through development data, equipment capability, process validation and appropriate technical/vendor justification.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = Slate600
            )
        }
    }
}

@Composable
fun FormulaDetailDialog(
    formula: FormulaDetail,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("formula_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formula.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Methodology: ${formula.methodologyType.name.replace("_", " ")}",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "EQUATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formula.equationText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "VARIABLES & SUBSTITUTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = PharmaBlueLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = formula.substitutedText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PharmaBlueDark
                        )
                    }
                }

                if (formula.variablesDescription.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        formula.variablesDescription.forEach { (variable, desc) ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "$variable: ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ENGINEERING NOTES & ASSUMPTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formula.engineeringNotes,
                    fontSize = 12.sp,
                    color = Slate700,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("dialog_close_button")
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun ParameterRow(
    param: ScaleParameter,
    onShowFormula: ((FormulaDetail) -> Unit)? = null
) {
    fun formatVal(v: Double?): String {
        return v?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = param.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = param.unit,
                            fontSize = 11.sp,
                            color = Slate600,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (param.formulaDetail != null && onShowFormula != null) {
                    TextButton(
                        onClick = { onShowFormula(param.formulaDetail) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("view_formula_${param.name.lowercase().replace(" ", "_")}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = "Formula",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Formula", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Values Table Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate50, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ValueColumn(label = "Source", value = formatVal(param.sourceValue), isHighlight = false)
                ValueColumn(label = "Target 1", value = formatVal(param.target1Value), isHighlight = true)
                if (param.target2Value != null) {
                    ValueColumn(label = "Target 2", value = formatVal(param.target2Value), isHighlight = true)
                }
                if (param.target3Value != null) {
                    ValueColumn(label = "Target 3", value = formatVal(param.target3Value), isHighlight = true)
                }
                if (param.scaleFactor1 != null) {
                    ValueColumn(
                        label = "Scale (T1/S)",
                        value = "${String.format(Locale.US, "%.2f", param.scaleFactor1)}x",
                        isHighlight = false
                    )
                }
            }

            if (param.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = param.notes,
                    fontSize = 10.sp,
                    color = Slate500
                )
            }
        }
    }
}

@Composable
private fun ValueColumn(
    label: String,
    value: String,
    isHighlight: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Slate500,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else Slate800
        )
    }
}
