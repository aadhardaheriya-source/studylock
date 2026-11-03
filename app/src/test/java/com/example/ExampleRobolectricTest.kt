package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppPreferences
import com.example.util.UsageStatsHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StudyLock", appName)
    }

    @Test
    fun `test screen time limits preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppPreferences.setScreenTimeLimit(context, "com.example.testapp", 45)
        val limits = AppPreferences.getScreenTimeLimits(context)
        assertEquals(45, limits["com.example.testapp"])

        AppPreferences.removeScreenTimeLimit(context, "com.example.testapp")
        val limitsAfter = AppPreferences.getScreenTimeLimits(context)
        assertFalse(limitsAfter.containsKey("com.example.testapp"))
    }

    @Test
    fun `test study time session and temporary unlock`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppPreferences.startStudyTime(context, 30)
        assertTrue(AppPreferences.isStudyTimeActive(context))

        AppPreferences.stopStudyTime(context)
        assertFalse(AppPreferences.isStudyTimeActive(context))

        AppPreferences.setTemporaryUnlock(context, "com.example.distractingapp", 10 * 60 * 1000L)
        assertTrue(AppPreferences.isTemporarilyUnlocked(context, "com.example.distractingapp"))
    }

    @Test
    fun `test format minutes to Hindi`() {
        assertEquals("30 मिनट", UsageStatsHelper.formatMinutesToHindi(30))
        assertEquals("1 घंटा", UsageStatsHelper.formatMinutesToHindi(60))
        assertEquals("1 घंटा 30 मिनट", UsageStatsHelper.formatMinutesToHindi(90))
        assertEquals("2 घंटा", UsageStatsHelper.formatMinutesToHindi(120))
    }
}
