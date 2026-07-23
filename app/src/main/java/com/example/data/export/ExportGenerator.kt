package com.example.data.export

import android.content.Context
import com.example.domain.model.Expense
import com.example.domain.model.Group
import com.example.domain.model.Settlement
import java.io.File

interface ExportGenerator {
    suspend fun generate(
        context: Context,
        scopeName: String,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        groups: List<Group> = emptyList()
    ): File
}
