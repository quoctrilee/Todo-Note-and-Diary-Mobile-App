package com.example.todonotediary.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: "Next task"
        val taskId = intent?.getIntExtra("taskId", -1) ?: -1

        // You can log to see that notifications are triggered
        Log.d("TaskReminderReceiver", "Received task reminder: $title, TaskID: $taskId")

        // Show notification with the task title
        NotificationHelper.showNotification(
            context = context,
            title = "Nhắc nhở công việc",
            message = title,
            notificationId = taskId
        )
    }
}