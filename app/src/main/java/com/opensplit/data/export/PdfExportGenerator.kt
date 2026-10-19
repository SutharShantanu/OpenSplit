package com.opensplit.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.Settlement
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

        if (expenses.isEmpty() && settlements.isNotEmpty()) {
            // Settlement Statement Header
            canvas.drawText("OpenSplit — Settlement Statement", 40f, y, titlePaint)
            y += 20f
            canvas.drawText("Scope: $scopeName • Generated: $generatedDate", 40f, y, subPaint)
            y += 25f
            canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
            y += 20f

            // Table Header
            val colDate = 40f
            val colParties = 115f
            val colMethod = 275f
            val colNote = 360f
            val colAmount = 475f

            canvas.drawText("Date", colDate, y, headerPaint)
            canvas.drawText("Transfer Flow", colParties, y, headerPaint)
            canvas.drawText("Method", colMethod, y, headerPaint)
            canvas.drawText("Note", colNote, y, headerPaint)
            canvas.drawText("Amount", colAmount, y, headerPaint)
            y += 10f
            canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
            y += 18f

            // Draw Settlements
            settlements.forEach { set ->
                if (y > pageHeight - 100) {
                    pdfDoc.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDoc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f

                    canvas.drawText("Date", colDate, y, headerPaint)
                    canvas.drawText("Transfer Flow", colParties, y, headerPaint)
                    canvas.drawText("Method", colMethod, y, headerPaint)
                    canvas.drawText("Note", colNote, y, headerPaint)
                    canvas.drawText("Amount", colAmount, y, headerPaint)
                    y += 10f
                    canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
                    y += 18f
                }

                val dateStr = try { dateFormat.format(set.date.toDate()) } catch (e: Exception) { "" }
                val fromDisplay = if (set.fromUid.length > 10) set.fromUid.take(8) + ".." else set.fromUid
                val toDisplay = if (set.toUid.length > 10) set.toUid.take(8) + ".." else set.toUid
                val flow = "$fromDisplay → $toDisplay"
                val methodStr = set.method.name
                val noteStr = if ((set.note ?: "").length > 18) (set.note ?: "").take(15) + "..." else (set.note ?: "-")
                val amountStr = "${set.currency} ${String.format(Locale.US, "%.2f", set.amount)}"

                canvas.drawText(dateStr, colDate, y, textPaint)
                canvas.drawText(flow, colParties, y, textPaint)
                canvas.drawText(methodStr, colMethod, y, textPaint)
                canvas.drawText(noteStr, colNote, y, textPaint)
                canvas.drawText(amountStr, colAmount, y, textPaint)

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

            val totalAmount = settlements.sumOf { it.amount }
            val primaryCurrency = settlements.firstOrNull()?.currency ?: "USD"
            val methodTotals = settlements.groupBy { it.method.name }
                .mapValues { (_, sets) -> sets.sumOf { it.amount } }

            canvas.drawText("TOTAL SETTLED: $primaryCurrency ${String.format(Locale.US, "%.2f", totalAmount)} (${settlements.size} transactions)", 40f, y, headerPaint)
            y += 18f

            methodTotals.forEach { (method, sum) ->
                if (y < pageHeight - 40) {
                    canvas.drawText("• $method: $primaryCurrency ${String.format(Locale.US, "%.2f", sum)}", 50f, y, subPaint)
                    y += 14f
                }
            }
        } else {
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
                val amountStr = "${exp.currency} ${String.format(Locale.US, "%.2f", exp.amount)}"
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

            canvas.drawText("TOTAL EXPENSES: ${String.format(Locale.US, "%.2f", totalAmount)}", 40f, y, headerPaint)
            y += 18f

            categoryTotals.forEach { (cat, sum) ->
                if (y < pageHeight - 40) {
                    canvas.drawText("• $cat: ${String.format(Locale.US, "%.2f", sum)}", 50f, y, subPaint)
                    y += 14f
                }
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
