package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object AppPreferences {
    private const val PREFS_NAME = "studylock_prefs"

    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_SCREEN_TIME_LIMITS = "screen_time_limits"
    private const val KEY_STUDY_BLOCKED_PACKAGES = "study_blocked_packages"
    private const val KEY_STUDY_BLOCKED_INITIALIZED = "study_blocked_initialized"
    private const val KEY_STUDY_TIME_ACTIVE = "study_time_active"
    private const val KEY_STUDY_TIME_START = "study_time_start"
    private const val KEY_STUDY_TIME_END = "study_time_end"
    private const val KEY_STUDY_TIME_DURATION_MIN = "study_time_duration_min"
    private const val PREF_TEMP_UNLOCK_PREFIX = "temp_unlock_"

    // Default distraction app package candidates
    val DEFAULT_CANDIDATE_PACKAGES = setOf(
        "com.instagram.android",      // Instagram
        "com.facebook.katana",        // Facebook
        "com.facebook.lite",          // Facebook Lite
        "com.snapchat.android",       // Snapchat
        "com.dts.freefireth",         // Free Fire
        "com.dts.freefiremax",        // Free Fire Max
        "com.pubg.imobile",           // BGMI
        "com.tencent.ig",             // PUBG Mobile
        "com.pubg.krmobile",          // PUBG KR
        "com.twitter.android",        // X / Twitter
        "com.zhiliaoapp.musically",   // TikTok
        "com.ss.android.ugc.trill",   // TikTok
        "com.reddit.frontpage",       // Reddit
        "com.pinterest",              // Pinterest
        "com.netflix.mediaclient"      // Netflix
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    // Screen Time Limits (PackageName -> Minutes)
    fun getScreenTimeLimits(context: Context): Map<String, Int> {
        val jsonStr = getPrefs(context).getString(KEY_SCREEN_TIME_LIMITS, null) ?: return emptyMap()
        val map = mutableMapOf<String, Int>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getInt(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun setScreenTimeLimit(context: Context, packageName: String, limitMinutes: Int) {
        val current = getScreenTimeLimits(context).toMutableMap()
        current[packageName] = limitMinutes
        saveScreenTimeLimits(context, current)
    }

    fun removeScreenTimeLimit(context: Context, packageName: String) {
        val current = getScreenTimeLimits(context).toMutableMap()
        current.remove(packageName)
        saveScreenTimeLimits(context, current)
    }

    private fun saveScreenTimeLimits(context: Context, map: Map<String, Int>) {
        val json = JSONObject()
        for ((k, v) in map) {
            json.put(k, v)
        }
        getPrefs(context).edit().putString(KEY_SCREEN_TIME_LIMITS, json.toString()).apply()
    }

    // Study Time Blocked Apps
    fun getStudyTimeBlockedApps(context: Context): Set<String> {
        val prefs = getPrefs(context)
        val initialized = prefs.getBoolean(KEY_STUDY_BLOCKED_INITIALIZED, false)
        if (!initialized) {
            // Find which candidate apps are installed
            val pm = context.packageManager
            val installedCandidates = DEFAULT_CANDIDATE_PACKAGES.filter { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (e: Exception) {
                    false
                }
            }.toSet()
            // Mark initialized
            prefs.edit()
                .putStringSet(KEY_STUDY_BLOCKED_PACKAGES, installedCandidates)
                .putBoolean(KEY_STUDY_BLOCKED_INITIALIZED, true)
                .apply()
            return installedCandidates
        }
        return prefs.getStringSet(KEY_STUDY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setStudyTimeBlockedApps(context: Context, packages: Set<String>) {
        getPrefs(context).edit()
            .putStringSet(KEY_STUDY_BLOCKED_PACKAGES, packages)
            .putBoolean(KEY_STUDY_BLOCKED_INITIALIZED, true)
            .apply()
    }

    fun addStudyTimeBlockedApp(context: Context, packageName: String) {
        val current = getStudyTimeBlockedApps(context).toMutableSet()
        current.add(packageName)
        setStudyTimeBlockedApps(context, current)
    }

    fun removeStudyTimeBlockedApp(context: Context, packageName: String) {
        val current = getStudyTimeBlockedApps(context).toMutableSet()
        current.remove(packageName)
        setStudyTimeBlockedApps(context, current)
    }

    // Study Time Active State
    fun isStudyTimeActive(context: Context): Boolean {
        val prefs = getPrefs(context)
        val active = prefs.getBoolean(KEY_STUDY_TIME_ACTIVE, false)
        if (!active) return false
        val endTime = prefs.getLong(KEY_STUDY_TIME_END, 0L)
        if (System.currentTimeMillis() >= endTime) {
            // Expired naturally
            stopStudyTime(context)
            return false
        }
        return true
    }

    fun getStudyTimeEndTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_STUDY_TIME_END, 0L)
    }

    fun getStudyTimeStartTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_STUDY_TIME_START, 0L)
    }

    fun getStudyTimeDurationMinutes(context: Context): Int {
        return getPrefs(context).getInt(KEY_STUDY_TIME_DURATION_MIN, 45)
    }

    fun startStudyTime(context: Context, durationMinutes: Int) {
        val now = System.currentTimeMillis()
        val end = now + (durationMinutes * 60 * 1000L)
        getPrefs(context).edit()
            .putBoolean(KEY_STUDY_TIME_ACTIVE, true)
            .putLong(KEY_STUDY_TIME_START, now)
            .putLong(KEY_STUDY_TIME_END, end)
            .putInt(KEY_STUDY_TIME_DURATION_MIN, durationMinutes)
            .apply()
    }

    fun stopStudyTime(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_STUDY_TIME_ACTIVE, false)
            .putLong(KEY_STUDY_TIME_END, 0L)
            .apply()
    }

    // 10-minute temporary unlock
    fun getTemporaryUnlockExpiry(context: Context, packageName: String): Long {
        return getPrefs(context).getLong(PREF_TEMP_UNLOCK_PREFIX + packageName, 0L)
    }

    fun setTemporaryUnlock(context: Context, packageName: String, durationMs: Long = 10 * 60 * 1000L) {
        val expiry = System.currentTimeMillis() + durationMs
        getPrefs(context).edit()
            .putLong(PREF_TEMP_UNLOCK_PREFIX + packageName, expiry)
            .apply()
    }

    fun isTemporarilyUnlocked(context: Context, packageName: String): Boolean {
        val expiry = getTemporaryUnlockExpiry(context, packageName)
        return System.currentTimeMillis() < expiry
    }
}
