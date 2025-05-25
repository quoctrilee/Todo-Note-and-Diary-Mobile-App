package com.example.todonotediary

import android.app.Application
import com.example.todonotediary.utils.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TodoNoteDiaryApp : Application(){
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.scheduleDailyReminder(this)
    }

}