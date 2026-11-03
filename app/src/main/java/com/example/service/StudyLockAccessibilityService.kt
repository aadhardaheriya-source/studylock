package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.LockActivity
import com.example.data.AppPreferences
import com.example.util.UsageStatsHelper

class StudyLockAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastLockedPackage: String? = null
    private var lastLockLaunchTime: Long = 0L
    private var lastShortsDetectedTime: Long = 0L

    // Known system packages to ignore
    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.android.settings",
        "android",
        "com.google.android.inputmethod.latin",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.oppo.launcher",
        "com.huawei.android.launcher"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (ignoredPackages.contains(packageName)) return

        val eventType = event.eventType

        // 1. YouTube Shorts Special Detection Rule
        if (packageName == "com.google.android.youtube") {
            if (AppPreferences.isStudyTimeActive(applicationContext)) {
                checkAndBlockYouTubeShorts(event)
            }
            return
        }

        // Only evaluate blocking when window changes
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            checkAndBlockApp(packageName)
        }
    }

    private fun checkAndBlockYouTubeShorts(event: AccessibilityEvent) {
        val now = System.currentTimeMillis()
        if (now - lastShortsDetectedTime < 2500L) return

        var isShorts = false

        // Check event text
        val eventTexts = event.text
        if (eventTexts != null) {
            for (text in eventTexts) {
                if (text.contains("Shorts", ignoreCase = true) || text.contains("शॉर्ट्स", ignoreCase = true)) {
                    isShorts = true
                    break
                }
            }
        }

        // Check node tree
        if (!isShorts) {
            val root = rootInActiveWindow
            if (root != null) {
                isShorts = scanNodeForShorts(root)
            }
        }

        if (isShorts) {
            lastShortsDetectedTime = now
            // Push back out of Shorts
            performGlobalAction(GLOBAL_ACTION_BACK)
            mainHandler.post {
                Toast.makeText(
                    applicationContext,
                    "स्टडी टाइम में शॉर्ट्स ब्लॉक हैं! अपनी पढ़ाई पर ध्यान दें।",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun scanNodeForShorts(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 6) return false

        val viewId = node.viewIdResourceName
        if (viewId != null) {
            val lowerId = viewId.lowercase()
            if (lowerId.contains("shorts") || lowerId.contains("reel_recycler") || lowerId.contains("reel_player")) {
                return true
            }
        }

        val text = node.text?.toString()
        if (text != null && text.equals("Shorts", ignoreCase = true)) {
            return true
        }

        val desc = node.contentDescription?.toString()
        if (desc != null && (desc.contains("Shorts", ignoreCase = true) || desc.contains("शॉर्ट्स", ignoreCase = true))) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = scanNodeForShorts(child, depth + 1)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun checkAndBlockApp(packageName: String) {
        val context = applicationContext

        // Check if temporarily unlocked
        if (AppPreferences.isTemporarilyUnlocked(context, packageName)) {
            return
        }

        // Check Study Time mode
        val isStudyActive = AppPreferences.isStudyTimeActive(context)
        if (isStudyActive) {
            val blockedInStudy = AppPreferences.getStudyTimeBlockedApps(context)
            if (blockedInStudy.contains(packageName)) {
                val remainingMs = AppPreferences.getStudyTimeEndTime(context) - System.currentTimeMillis()
                val remainingMinutes = (remainingMs / (1000 * 60)).coerceAtLeast(1).toInt()
                val reasonText = "स्टडी टाइम चालू है ($remainingMinutes मिनट बाकी)"
                launchLockScreen(packageName, reasonText)
                return
            }
        }

        // Check Screen Time Daily Limit
        val limits = AppPreferences.getScreenTimeLimits(context)
        val limitMinutes = limits[packageName]
        if (limitMinutes != null && limitMinutes > 0) {
            val usageMinutes = UsageStatsHelper.getTodayUsageMinutes(context, packageName)
            if (usageMinutes >= limitMinutes) {
                val reasonText = "दैनिक स्क्रीन टाइम सीमा समाप्त (${limitMinutes} मिनट पूरे)"
                launchLockScreen(packageName, reasonText)
                return
            }
        }
    }

    private fun launchLockScreen(packageName: String, reason: String) {
        val now = System.currentTimeMillis()
        if (packageName == lastLockedPackage && now - lastLockLaunchTime < 1500L) {
            return
        }
        lastLockedPackage = packageName
        lastLockLaunchTime = now

        val intent = Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LockActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(LockActivity.EXTRA_REASON, reason)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
