package com.example.data.export

import android.content.Context
import com.example.domain.model.Expense
import com.example.domain.model.Group
import com.example.domain.model.Settlement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale

class JsonExportGenerator : ExportGenerator {
    override suspend fun generate(
        context: Context,
        scopeName: String,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        groups: List<Group>
    ): File = withContext(Dispatchers.IO) {
        val sanitizedScope = scopeName.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        val fileName = "opensplit_backup_${sanitizedScope}_${System.currentTimeMillis()}.json"
        val file = File(context.cacheDir, fileName)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

        val rootJson = JSONObject().apply {
            put("app", "OpenSplit")
            put("type", "backup")
            put("scope", scopeName)
            put("exportedAt", dateFormat.format(java.util.Date()))

            // Groups
            val groupsArray = JSONArray()
            groups.forEach { g ->
                groupsArray.put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("currency", g.currency)
                    put("createdBy", g.createdBy)
                    put("memberIds", JSONArray(g.memberIds))
                })
            }
            put("groups", groupsArray)

            // Expenses
            val expensesArray = JSONArray()
            expenses.forEach { exp ->
                expensesArray.put(JSONObject().apply {
                    put("id", exp.id)
                    put("groupId", exp.groupId)
                    put("description", exp.description)
                    put("amount", exp.amount)
                    put("currency", exp.currency)
                    put("category", exp.category)
                    put("paidBy", exp.paidBy)
                    put("splitType", exp.splitType.name)
                    put("date", try { dateFormat.format(exp.date.toDate()) } catch (e: Exception) { "" })

                    val splitsObj = JSONObject()
                    exp.splits.forEach { (uid, amount) ->
                        splitsObj.put(uid, amount)
                    }
                    put("splits", splitsObj)
                })
            }
            put("expenses", expensesArray)

            // Settlements
            val settlementsArray = JSONArray()
            settlements.forEach { set ->
                settlementsArray.put(JSONObject().apply {
                    put("id", set.id)
                    put("fromUid", set.fromUid)
                    put("toUid", set.toUid)
                    put("amount", set.amount)
                    put("currency", set.currency)
                    put("date", try { dateFormat.format(set.date.toDate()) } catch (e: Exception) { "" })
                })
            }
            put("settlements", settlementsArray)
        }

        FileWriter(file).use { writer ->
            writer.write(rootJson.toString(2))
        }

        file
    }
}
