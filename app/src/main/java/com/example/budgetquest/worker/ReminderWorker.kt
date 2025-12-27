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
import com.example.budgetquest.R // 請確保有 import R

class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Log.d("ReminderWorker", "工作開始執行！檢查時間...") // [Debug]

        // 這裡可以加入時間判斷，例如：如果在半夜被系統喚醒，可能不發送
        // 但目前我們先強制發送以測試功能

        try {
            sendNotification("記帳提醒", "今天過得如何？別忘了記錄今天的開銷喔！")
            Log.d("ReminderWorker", "通知發送成功") // [Debug]
            return Result.success()
        } catch (e: Exception) {
            Log.e("ReminderWorker", "通知發送失敗", e)
            return Result.retry()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "daily_reminder_channel" // 修改為更明確的 ID
        val notificationId = 1001

        // 1. 建立 Channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "每日記帳提醒"
            val descriptionText = "提醒您每日記錄開銷"
            val importance = NotificationManager.IMPORTANCE_HIGH // [修改] 改為 HIGH 確保會跳出來
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 2. 建構通知
        // 注意：小圖示使用系統預設的 ic_menu_edit 或是你自己的 R.drawable.ic_launcher_foreground
        // 如果圖示資源不存在，通知會發送失敗
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // [修改] 改為 HIGH
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
            Log.w("ReminderWorker", "沒有通知權限，無法發送") // [Debug]
        }
    }
}