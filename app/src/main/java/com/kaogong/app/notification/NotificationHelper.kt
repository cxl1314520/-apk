package com.kaogong.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.kaogong.app.KnowledgeDetailActivity
import com.kaogong.app.R
import com.kaogong.app.data.NewsItem
import com.kaogong.app.data.NewsRepository
import com.kaogong.app.data.model.ChineseItem
import com.kaogong.app.receiver.ExcludeActionReceiver

object NotificationHelper {

    private const val CHANNEL_ID   = "kaogong_chinese"
    private const val CHANNEL_NAME = "每日词汇推送"

    fun createNotificationChannel(context: Context) {
        val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "推送成语、歇后语、词语、时政要闻"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun showItemNotification(context: Context, item: ChineseItem) {
        val notifId = item.type.ordinal + 10

        // 全屏弹窗 Intent（锁屏时直接弹起详情页，解锁时作为横幅展示）
        val detailIntent = Intent(context, KnowledgeDetailActivity::class.java).apply {
            putExtra(KnowledgeDetailActivity.EXTRA_TYPE, item.type.ordinal)
            putExtra(KnowledgeDetailActivity.EXTRA_KEY, item.key)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, notifId, detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "已知" 按钮
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
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPi, true)   // ← 直接弹出界面
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .addAction(R.drawable.ic_notification, "✓ 已知", exPi)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(notifId, notif)
    }

    fun showNewsNotification(context: Context, news: NewsItem, notifId: Int) {
        // 全屏弹窗 → 在 KnowledgeDetailActivity 以 TYPE_NEWS 模式展示
        val detailIntent = Intent(context, KnowledgeDetailActivity::class.java).apply {
            putExtra(KnowledgeDetailActivity.EXTRA_TYPE, KnowledgeDetailActivity.TYPE_NEWS)
            putExtra(KnowledgeDetailActivity.EXTRA_NEWS_TITLE, news.title)
            putExtra(KnowledgeDetailActivity.EXTRA_NEWS_DESC, news.description)
            putExtra(KnowledgeDetailActivity.EXTRA_NEWS_LINK, news.link)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, notifId, detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val exIntent = Intent(context, ExcludeActionReceiver::class.java).apply {
            putExtra(ExcludeActionReceiver.EXTRA_TYPE_ORDINAL, ExcludeActionReceiver.TYPE_ORDINAL_NEWS)
            putExtra(ExcludeActionReceiver.EXTRA_ITEM_KEY, news.title)
            putExtra(ExcludeActionReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val exPi = PendingIntent.getBroadcast(
            context, notifId + 100, exIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📰 时政要闻｜${news.title}")
            .setContentText(news.description.take(60))
            .setStyle(NotificationCompat.BigTextStyle().bigText(news.description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPi, true)   // ← 直接弹出界面
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .addAction(R.drawable.ic_notification, "✓ 已知", exPi)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(notifId, notif)
    }
}
