package com.example

import android.app.Application

class StudyLockApp : Application() {
    companion object {
        lateinit var instance: StudyLockApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
