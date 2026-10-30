package com.opensplit.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test

class BugReportBodyTest {
    @Test
    fun `body has every section and marks blanks`() {
        val body = bugReportBody("Crash on save", "App closed", "", "  ", "Sometimes", null)
        listOf("■ Summary\nCrash on save", "■ What happened\nApp closed", "■ Steps to reproduce\nNot provided",
            "■ Expected behaviour\nNot provided", "■ How often\nSometimes", "■ Device\nNot shared", "Reported: ")
            .forEach { assertTrue("missing: $it", it in body) }
    }
}
