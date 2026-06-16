package com.kaogong.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaogong.app.data.ChineseDataRepository
import com.kaogong.app.data.DataType
import com.kaogong.app.data.NewsRepository
import com.kaogong.app.notification.NotificationHelper
import com.kaogong.app.scheduler.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra("hour", 8)

        // 推送4种词汇，每种一条
        DataType.values().forEach { type ->
            ChineseDataRepository.getRandomItem(context, type)?.let {
                NotificationHelper.showItemNotification(context, it)
            }
        }

        // 重新安排明天的闹钟
        AlarmScheduler.rescheduleForTomorrow(context, hour)

        // 时政新闻需要网络，在后台线程获取
        val pending = goAsync()
        Thread {
            try {
                NewsRepository.getRandomNews(context)?.let {
                    NotificationHelper.showNewsNotification(context, it)
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }.start()
    }
}
