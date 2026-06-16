package com.kaogong.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaogong.app.PushPopupActivity
import com.kaogong.app.data.ChineseDataRepository
import com.kaogong.app.data.DataType
import com.kaogong.app.data.NewsRepository
import com.kaogong.app.notification.NotificationHelper
import com.kaogong.app.scheduler.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra("hour", 8)

        // 重新安排明天的闹钟（先安排，确保不丢失）
        AlarmScheduler.rescheduleForTomorrow(context, hour)

        // 方式1：直接启动全屏弹窗 Activity（亮屏+后台时生效）
        // 系统会自动处理多个 Activity 的堆叠，用户逐个查看
        DataType.values().forEach { type ->
            val item = ChineseDataRepository.getRandomItem(context, type) ?: return@forEach
            val popupIntent = Intent(context, PushPopupActivity::class.java).apply {
                putExtra(PushPopupActivity.EXTRA_MODE, PushPopupActivity.MODE_KNOWLEDGE)
                putExtra(PushPopupActivity.EXTRA_TYPE, type.ordinal)
                putExtra(PushPopupActivity.EXTRA_KEY, item.key)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(popupIntent)
        }

        // 方式2：同时发送 setFullScreenIntent 通知（锁屏/熄屏时生效）
        // 两种方式互补，覆盖所有场景
        DataType.values().forEach { type ->
            ChineseDataRepository.getRandomItem(context, type)?.let {
                NotificationHelper.showItemNotification(context, it)
            }
        }

        // 时政：后台网络请求
        val pending = goAsync()
        Thread {
            try {
                val newsItems = NewsRepository.getNewsItems(context, count = 2)
                newsItems.forEachIndexed { index, news ->
                    val notifId = if (index == 0) NewsRepository.NEWS_NOTIF_ID_1
                                  else             NewsRepository.NEWS_NOTIF_ID_2
                    // 直接启动弹窗
                    val popupIntent = Intent(context, PushPopupActivity::class.java).apply {
                        putExtra(PushPopupActivity.EXTRA_MODE, PushPopupActivity.MODE_NEWS)
                        putExtra(PushPopupActivity.EXTRA_NEWS_TITLE, news.title)
                        putExtra(PushPopupActivity.EXTRA_NEWS_DESC, news.description)
                        putExtra(PushPopupActivity.EXTRA_NEWS_LINK, news.link)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(popupIntent)
                    // 同时发送通知（锁屏兜底）
                    NotificationHelper.showNewsNotification(context, news, notifId)
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }.start()
    }
}
