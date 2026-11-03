package com.example.util

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.AppInfo
import com.example.data.AppPreferences
import java.util.Calendar

object UsageStatsHelper {

    fun getTodayUsageMinutes(context: Context, packageName: String): Int {
        val usageMillis = getTodayUsageMillis(context, packageName)
        return (usageMillis / (1000 * 60)).toInt()
    }

    fun getTodayUsageMillis(context: Context, packageName: String): Long {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0L
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val statsMap = usm.queryAndAggregateUsageStats(startTime, endTime)
            statsMap[packageName]?.totalTimeInForeground ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val limits = AppPreferences.getScreenTimeLimits(context)
        val studyBlocked = AppPreferences.getStudyTimeBlockedApps(context)

        val seenPackages = mutableSetOf<String>()
        val list = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName || seenPackages.contains(pkg)) {
                continue
            }
            seenPackages.add(pkg)

            val name = resolveInfo.loadLabel(pm)?.toString() ?: pkg
            val icon = try {
                resolveInfo.loadIcon(pm)
            } catch (e: Exception) {
                null
            }

            val usageMinutes = getTodayUsageMinutes(context, pkg)
            val limit = limits[pkg] ?: 0
            val isBlockedInStudy = studyBlocked.contains(pkg)

            list.add(
                AppInfo(
                    packageName = pkg,
                    appName = name,
                    icon = icon,
                    usageMinutesToday = usageMinutes,
                    limitMinutes = limit,
                    isBlockedInStudyTime = isBlockedInStudy
                )
            )
        }

        return list.sortedBy { it.appName.lowercase() }
    }

    fun formatMinutesToHindi(minutes: Int): String {
        if (minutes <= 0) return "0 मिनट"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when {
            hours > 0 && remainingMinutes > 0 -> "$hours घंटा $remainingMinutes मिनट"
            hours > 0 -> "$hours घंटा"
            else -> "$remainingMinutes मिनट"
        }
    }
}
