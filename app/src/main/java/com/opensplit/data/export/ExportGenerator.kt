package com.opensplit.data.export

import android.content.Context
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.Settlement
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
