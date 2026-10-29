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

    @Test
    fun checksumLookupAndHash() {
        val listing = "abc123  OpenSplit-v1.0.5.apk\ndef456  OpenSplit-v1.0.5.aab\n"
        assertEquals("abc123", AppUpdater.expectedSha256(listing, "OpenSplit-v1.0.5.apk"))
        assertEquals("def456", AppUpdater.expectedSha256(listing, "OpenSplit-v1.0.5.aab"))
        assertEquals(null, AppUpdater.expectedSha256(listing, "other.apk"))
        // sha256("abc") from FIPS 180-2
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            AppUpdater.sha256Hex("abc".byteInputStream())
        )
    }

    @Test
    fun bodyWithoutChangeListHasNoNotes() {
        assertEquals("", AppUpdater.plainNotes("## OpenSplit v1.0.32\n```bash\nadb install -r x.apk\n```"))
    }

    @Test
    fun commitNotesNewestFirstWithoutMerges() {
        val notes = AppUpdater.commitNotes(listOf("fix: a\n\nbody", "Merge branch 'x'", "feat: b"))
        assertEquals("• feat: b\n• fix: a", notes)
    }
}
