package com.kaogong.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaogong.app.data.ChineseDataRepository
import com.kaogong.app.data.DataType
import com.kaogong.app.notification.NotificationHelper
import com.kaogong.app.scheduler.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra("hour", 8)

        // 每次推送：每种数据类型各推一条
        DataType.values().forEach { type ->
            ChineseDataRepository.getRandomItem(context, type)?.let {
                NotificationHelper.showItemNotification(context, it)
            }
        }

        AlarmScheduler.rescheduleForTomorrow(context, hour)
    }
}
