package com.kaogong.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kaogong.app.KnowledgeDetailActivity
import com.kaogong.app.R
import com.kaogong.app.data.KnowledgeItem

object NotificationHelper {

    private const val CHANNEL_ID = "kaogong_knowledge"
    private const val CHANNEL_NAME = "考公知识推送"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "每日考公知识推送，早8点至晚10点每2小时推送一次"
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showKnowledgeNotification(context: Context, item: KnowledgeItem) {
        val intent = Intent(context, KnowledgeDetailActivity::class.java).apply {
            putExtra("knowledge_id", item.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, item.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📚 ${item.category}·每日一练")
            .setContentText(item.title)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(item.content.take(200))
                .setBigContentTitle("📚 ${item.category} | ${item.title}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(item.id, notification)
    }
}
