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

        // 推送3种词汇，每种一条（无需网络，本地文件）
        DataType.values().forEach { type ->
            ChineseDataRepository.getRandomItem(context, type)?.let {
                NotificationHelper.showItemNotification(context, it)
            }
        }

        // 重新安排明天的闹钟
        AlarmScheduler.rescheduleForTomorrow(context, hour)

        // 时政：从人民日报获取1-2条，后台网络请求
        val pending = goAsync()
        Thread {
            try {
                val newsItems = NewsRepository.getNewsItems(context, count = 2)
                newsItems.forEachIndexed { index, news ->
                    val notifId = if (index == 0) NewsRepository.NEWS_NOTIF_ID_1
                                  else             NewsRepository.NEWS_NOTIF_ID_2
                    NotificationHelper.showNewsNotification(context, news, notifId)
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }.start()
    }
}
