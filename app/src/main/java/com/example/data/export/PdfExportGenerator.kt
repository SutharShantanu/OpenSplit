package com.example.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.domain.model.Expense
import com.example.domain.model.Group
import com.example.domain.model.Settlement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class PdfExportGenerator : ExportGenerator {
    override suspend fun generate(
        context: Context,
        scopeName: String,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        groups: List<Group>
    ): File = withContext(Dispatchers.IO) {
        val sanitizedScope = scopeName.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        val fileName = "opensplit_export_${sanitizedScope}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)

        val pdfDoc = PdfDocument()
        val pageWidth = 595 // A4
        val pageHeight = 842

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val generatedDate = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(java.util.Date())

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        var y = 40f

        // Title Header
        canvas.drawText("OpenSplit — Expense Report", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Scope: $scopeName • Generated: $generatedDate", 40f, y, subPaint)
        y += 25f
        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 20f

        // Table Header
        val colDate = 40f
        val colDesc = 120f
        val colCat = 300f
        val colAmount = 420f
        val colPaid = 500f

        canvas.drawText("Date", colDate, y, headerPaint)
        canvas.drawText("Description", colDesc, y, headerPaint)
        canvas.drawText("Category", colCat, y, headerPaint)
        canvas.drawText("Amount", colAmount, y, headerPaint)
        canvas.drawText("Paid By", colPaid, y, headerPaint)
        y += 10f
        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 18f

        // Draw Expenses
        expenses.forEach { exp ->
            if (y > pageHeight - 100) {
                // Finish current page and start a new page
                pdfDoc.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas
                y = 40f

                // Draw table header on new page
                canvas.drawText("Date", colDate, y, headerPaint)
                canvas.drawText("Description", colDesc, y, headerPaint)
                canvas.drawText("Category", colCat, y, headerPaint)
                canvas.drawText("Amount", colAmount, y, headerPaint)
                canvas.drawText("Paid By", colPaid, y, headerPaint)
                y += 10f
                canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
                y += 18f
            }

            val dateStr = try { dateFormat.format(exp.date.toDate()) } catch (e: Exception) { "" }
            val desc = if (exp.description.length > 25) exp.description.take(22) + "..." else exp.description
            val cat = exp.category
            val amountStr = "${exp.currency} ${String.format("%.2f", exp.amount)}"
            val paidBy = if (exp.paidBy.length > 10) exp.paidBy.take(8) + ".." else exp.paidBy

            canvas.drawText(dateStr, colDate, y, textPaint)
            canvas.drawText(desc, colDesc, y, textPaint)
            canvas.drawText(cat, colCat, y, textPaint)
            canvas.drawText(amountStr, colAmount, y, textPaint)
            canvas.drawText(paidBy, colPaid, y, textPaint)

            y += 16f
        }

        // Totals Footer
        y += 15f
        if (y > pageHeight - 80) {
            pdfDoc.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            page = pdfDoc.startPage(pageInfo)
            canvas = page.canvas
            y = 40f
        }

        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 20f

        val totalAmount = expenses.sumOf { it.amount }
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { (_, exps) -> exps.sumOf { it.amount } }

        canvas.drawText("TOTAL EXPENSES: ${String.format("%.2f", totalAmount)}", 40f, y, headerPaint)
        y += 18f

        categoryTotals.forEach { (cat, sum) ->
            if (y < pageHeight - 40) {
                canvas.drawText("• $cat: ${String.format("%.2f", sum)}", 50f, y, subPaint)
                y += 14f
            }
        }

        pdfDoc.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        file
    }
}
