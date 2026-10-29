package com.opensplit.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the startup path (Application, Firebase init, first Compose frame) into a baseline
 * profile. Run: ./gradlew :app:generateBaselineProfile with a device attached.
 * ponytail: startup only; signed-in journeys need a test account, add them when one exists.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = "open.split_07", includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
    }
}
