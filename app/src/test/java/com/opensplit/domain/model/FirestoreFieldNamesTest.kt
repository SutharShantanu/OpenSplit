package com.opensplit.domain.model

import com.google.firebase.firestore.util.CustomClassMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Boolean "is" properties must round-trip under the field names the repositories query. */
class FirestoreFieldNamesTest {
    @Suppress("UNCHECKED_CAST")
    private fun serialize(value: Any) = CustomClassMapper.convertToPlainJavaTypes(value) as Map<String, Any?>

    @Test
    fun expenseDeletedFlagUsesIsDeletedField() {
        assertEquals(true, serialize(Expense(isDeleted = true))["isDeleted"])
        val read = CustomClassMapper.convertToCustomClass(mapOf("isDeleted" to true), Expense::class.java, null)
        assertTrue(read.isDeleted)
    }

    @Test
    fun groupDirectFlagUsesIsDirectField() {
        assertEquals(true, serialize(Group(isDirect = true))["isDirect"])
        val read = CustomClassMapper.convertToCustomClass(mapOf("isDirect" to true), Group::class.java, null)
        assertTrue(read.isDirect)
    }
}
