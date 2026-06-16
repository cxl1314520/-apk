package com.kaogong.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kaogong.app.KnowledgeDetailActivity
import com.kaogong.app.R
import com.kaogong.app.data.model.ChineseItem
import com.kaogong.app.receiver.ExcludeActionReceiver

object NotificationHelper {

    private const val CHANNEL_ID   = "kaogong_chinese"
    private const val CHANNEL_NAME = "每日词汇推送"

    fun createNotificationChannel(context: Context) {
        val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "推送成语、歇后语、词语、汉字等内容"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun showItemNotification(context: Context, item: ChineseItem) {
        // Notification ID unique per type (stable – only latest per type is shown)
        val notifId = item.type.ordinal + 10

        // Tap → open detail
        val tapIntent = Intent(context, KnowledgeDetailActivity::class.java).apply {
            putExtra(KnowledgeDetailActivity.EXTRA_TYPE, item.type.ordinal)
            putExtra(KnowledgeDetailActivity.EXTRA_KEY, item.key)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPi = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: "不再推送"
        val exIntent = Intent(context, ExcludeActionReceiver::class.java).apply {
            putExtra(ExcludeActionReceiver.EXTRA_TYPE_ORDINAL, item.type.ordinal)
            putExtra(ExcludeActionReceiver.EXTRA_ITEM_KEY, item.key)
            putExtra(ExcludeActionReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val exPi = PendingIntent.getBroadcast(
            context, notifId + 100, exIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = buildString {
            if (item.subtitle.isNotEmpty()) appendLine("[${item.subtitle}]")
            append(item.content)
            if (item.extra.isNotEmpty()) { appendLine(); append(item.extra) }
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${item.type.emoji} ${item.type.displayName}｜${item.title}")
            .setContentText(item.content.take(60))
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .addAction(R.drawable.ic_notification, "不再推送", exPi)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(notifId, notif)
    }
}
