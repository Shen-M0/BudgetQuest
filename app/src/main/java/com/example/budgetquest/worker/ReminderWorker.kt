package com.example.budgetquest.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.budgetquest.R

class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Log.d("ReminderWorker", "工作開始執行！檢查時間...")

        // 這裡可以加入時間判斷，例如：如果在半夜被系統喚醒，可能不發送
        // 但目前我們先強制發送以測試功能

        try {
            // [提取] 使用 getString 取得標題與內容
            val title = applicationContext.getString(R.string.notification_title_daily)
            val message = applicationContext.getString(R.string.notification_msg_daily)

            sendNotification(title, message)
            Log.d("ReminderWorker", "通知發送成功")
            return Result.success()
        } catch (e: Exception) {
            Log.e("ReminderWorker", "通知發送失敗", e)
            return Result.retry()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "daily_reminder_channel"
        val notificationId = 1001

        // 1. 建立 Channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // [提取] Channel 名稱與描述
            val name = applicationContext.getString(R.string.notification_channel_name)
            val descriptionText = applicationContext.getString(R.string.notification_channel_desc)

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 2. 建構通知
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // 3. 發送 (含權限檢查)
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        } else {
            Log.w("ReminderWorker", "沒有通知權限，無法發送")
        }
    }
}