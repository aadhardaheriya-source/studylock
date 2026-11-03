package com.example.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val usageMinutesToday: Int = 0,
    val limitMinutes: Int = 0,
    val isBlockedInStudyTime: Boolean = false
)
