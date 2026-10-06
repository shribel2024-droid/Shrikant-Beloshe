package com.example.pharma.util.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.pharma.data.db.ProjectEntity
import com.example.pharma.domain.model.ScaleUpResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportGenerators {

    fun generatePdf(
        context: Context,
        project: ProjectEntity?,
        results: List<ScaleUpResult>,
        preparedBy: String = project?.preparedBy ?: "Process Engineer"
    ): File? {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.rgb(13, 71, 161) // Deep blue
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSub = Paint().apply {
            color = Color.rgb(60, 60, 60)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintHeader = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintText = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintBold = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = Color.rgb(200, 200, 200)
            strokeWidth = 0.8f
        }

        val paintWarn = Paint().apply {
            color = Color.rgb(180, 100, 0)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        var y = 40f

        // Document Title
        canvas.drawText("Pharmaceutical OSD Scale-Up Calculation Report", 40f, y, paintTitle)
        y += 14f
        canvas.drawText("Formulation R&D | Technology Transfer | Manufacturing Process Engineering", 40f, y, paintSub)
        y += 18f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        // Project Info
        canvas.drawText("1. PROJECT & PRODUCT METADATA", 40f, y, paintHeader)
        y += 14f

        val projName = project?.projectName ?: "OSD Scale-Up Assessment"
        val prodName = project?.productName ?: "Generic OSD Formulation"
        val code = project?.productCode ?: "N/A"
        val dosage = project?.dosageForm ?: "Tablet"
        val process = project?.manufacturingProcess ?: "Granulation & Compression"
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        canvas.drawText("Project Name: $projName", 40f, y, paintText)
        canvas.drawText("Product Code: $code", 320f, y, paintText)
        y += 12f
        canvas.drawText("Product Name: $prodName", 40f, y, paintText)
        canvas.drawText("Dosage Form: $dosage", 320f, y, paintText)
        y += 12f
        canvas.drawText("Process: $process", 40f, y, paintText)
        canvas.drawText("Date: $dateStr", 320f, y, paintText)
        y += 12f
        canvas.drawText("Prepared By: $preparedBy", 40f, y, paintText)
        canvas.drawText("Facility: ${project?.manufacturingSite ?: "Plant Scale-Up Suite"}", 320f, y, paintText)
        y += 18f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        // Results Summary
        canvas.drawText("2. SCALE-UP CALCULATION RESULTS & PARAMETERS", 40f, y, paintHeader)
        y += 16f

        for (result in results.take(3)) { // Fit up to 3 major equipment calculations on page
            if (y > 700f) break

            canvas.drawText(result.equipmentType.displayName, 40f, y, paintBold)
            canvas.drawText("Basis: ${result.methodologyName}", 280f, y, paintSub)
            y += 12f

            // Table Header
            canvas.drawText("Parameter", 45f, y, paintBold)
            canvas.drawText("Unit", 175f, y, paintBold)
            canvas.drawText("Source", 230f, y, paintBold)
            canvas.drawText("Target 1", 305f, y, paintBold)
            canvas.drawText("Target 2", 380f, y, paintBold)
            canvas.drawText("Target 3", 455f, y, paintBold)
            y += 6f
            canvas.drawLine(40f, y, 555f, y, paintLine)
            y += 10f

            for (param in result.parameters.take(6)) {
                val sVal = param.sourceValue?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
                val t1Val = param.target1Value?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
                val t2Val = param.target2Value?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
                val t3Val = param.target3Value?.let { String.format(Locale.US, "%.1f", it) } ?: "-"

                canvas.drawText(param.name.take(24), 45f, y, paintText)
                canvas.drawText(param.unit, 175f, y, paintText)
                canvas.drawText(sVal, 230f, y, paintText)
                canvas.drawText(t1Val, 305f, y, paintText)
                canvas.drawText(t2Val, 380f, y, paintText)
                canvas.drawText(t3Val, 455f, y, paintText)
                y += 11f
            }

            // Warnings if any
            for (w in result.warnings.take(2)) {
                canvas.drawText("• Note: ${w.message}", 45f, y, paintWarn)
                y += 10f
            }
            y += 8f
        }

        // Footer Disclaimer
        val footerY = 800f
        canvas.drawLine(40f, footerY - 10f, 555f, footerY - 10f, paintLine)
        canvas.drawText(
            "Engineering calculation aid — verify against development data, equipment capability and approved technical-transfer procedures.",
            40f,
            footerY,
            paintSub
        )

        pdfDoc.finishPage(page)

        return try {
            val fileName = "ScaleUp_Report_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            file
        } catch (e: Exception) {
            pdfDoc.close()
            null
        }
    }

    fun generateCsv(
        context: Context,
        project: ProjectEntity?,
        results: List<ScaleUpResult>
    ): File? {
        val sb = StringBuilder()
        sb.append("Pharmaceutical OSD Scale-Up Calculation Report\n")
        sb.append("Project,${project?.projectName ?: "OSD Scale-Up"}\n")
        sb.append("Product,${project?.productName ?: "N/A"}\n")
        sb.append("Dosage Form,${project?.dosageForm ?: "Tablet"}\n")
        sb.append("Process,${project?.manufacturingProcess ?: "N/A"}\n")
        sb.append("Date,${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}\n")
        sb.append("Prepared By,${project?.preparedBy ?: "Engineering"}\n\n")

        for (res in results) {
            sb.append("Equipment,${res.equipmentType.displayName}\n")
            sb.append("Methodology,${res.methodologyName}\n")
            sb.append("Methodology Type,${res.methodologyType.name}\n")
            sb.append("Parameter,Unit,Source,Target 1,Target 2,Target 3,Scale Factor 1\n")
            for (p in res.parameters) {
                sb.append("\"${p.name}\",")
                sb.append("${p.unit},")
                sb.append("${p.sourceValue ?: ""},")
                sb.append("${p.target1Value ?: ""},")
                sb.append("${p.target2Value ?: ""},")
                sb.append("${p.target3Value ?: ""},")
                sb.append("${p.scaleFactor1 ?: ""}\n")
            }
            if (res.warnings.isNotEmpty()) {
                sb.append("Warnings/Notes\n")
                for (w in res.warnings) {
                    sb.append("\"${w.level}: ${w.message.replace("\"", "\"\"")}\"\n")
                }
            }
            sb.append("\n")
        }

        return try {
            val file = File(context.cacheDir, "ScaleUp_Data_${System.currentTimeMillis()}.csv")
            file.writeText(sb.toString())
            file
        } catch (e: Exception) {
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Calculation Report"))
        } catch (e: Exception) {
            // Fallback
        }
    }
}
