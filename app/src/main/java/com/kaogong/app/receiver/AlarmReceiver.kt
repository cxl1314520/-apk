package com.kaogong.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaogong.app.data.KnowledgeData
import com.kaogong.app.notification.NotificationHelper
import com.kaogong.app.scheduler.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra("hour", 8)
        val item = KnowledgeData.getScheduledItem(hour)
        NotificationHelper.showKnowledgeNotification(context, item)
        // 精确闹钟触发后重新设置明天同一时间
        AlarmScheduler.rescheduleForTomorrow(context, hour)
    }
}
