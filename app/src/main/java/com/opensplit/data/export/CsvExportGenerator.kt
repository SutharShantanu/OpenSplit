package com.opensplit.data.export

import android.content.Context
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.Settlement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale

class CsvExportGenerator : ExportGenerator {
    override suspend fun generate(
        context: Context,
        scopeName: String,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        groups: List<Group>
    ): File = withContext(Dispatchers.IO) {
        val sanitizedScope = scopeName.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        val fileName = "opensplit_export_${sanitizedScope}_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        FileWriter(file).use { writer ->
            // Header
            writer.append("Type,Date,Description,Category,Amount,Currency,PaidBy,SplitType\n")

            // Expenses
            expenses.forEach { exp ->
                val dateStr = try { dateFormat.format(exp.date.toDate()) } catch (e: Exception) { "" }
                val desc = sanitizeCsv(exp.description)
                val cat = sanitizeCsv(exp.category)
                val paidBy = exp.paidBy
                val splitType = exp.splitType.name
                writer.append("Expense,$dateStr,$desc,$cat,${exp.amount},${exp.currency},$paidBy,$splitType\n")
            }

            // Settlements
            settlements.forEach { set ->
                val dateStr = try { dateFormat.format(set.date.toDate()) } catch (e: Exception) { "" }
                val desc = sanitizeCsv("Settlement to ${set.toUid}")
                writer.append("Settlement,$dateStr,$desc,Settlement,${set.amount},${set.currency},${set.fromUid},EQUAL\n")
            }
        }

        file
    }

    private fun sanitizeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
