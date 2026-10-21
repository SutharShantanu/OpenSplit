package com.opensplit.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {
    private fun cmp(a: String, b: String) =
        AppUpdater.compareVersions(AppUpdater.parseVersion(a), AppUpdater.parseVersion(b))

    @Test
    fun parsesTagsAndVersionNames() {
        assertEquals(listOf(1, 0, 31), AppUpdater.parseVersion("v1.0.31"))
        assertEquals(listOf(1, 0), AppUpdater.parseVersion("1.0"))
    }

    @Test
    fun newerReleaseWins() {
        assertTrue(cmp("v1.0.31", "1.0.30") > 0)
        assertTrue(cmp("v1.0.31", "1.0") > 0)      // local build without run number
        assertTrue(cmp("v2.0.0", "1.9.99") > 0)
        assertEquals(0, cmp("v1.0.31", "1.0.31"))
        assertTrue(cmp("v1.0.29", "1.0.31") < 0)
    }

    @Test
    fun notesKeepOnlyChangeList() {
        val body = """
            ## OpenSplit v1.0.31
            | Asset | Description |
            | :--- | :--- |
            ## What's Changed
            * fix: stable CI signing key by @SutharShantanu in https://github.com/x/y/pull/1
            * feat: in-app updates
        """.trimIndent()
        assertEquals("• fix: stable CI signing key\n• feat: in-app updates", AppUpdater.plainNotes(body))
    }
}
