package com.example.todonotediary.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.showNotification(
            context,
            "TodoNoteDiary",
            "Hi..Let 's check your tasks to day!"
        )
    }
}
