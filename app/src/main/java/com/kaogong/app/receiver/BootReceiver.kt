package com.kaogong.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaogong.app.scheduler.AlarmScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // 手机重启后重新注册所有闹钟
            AlarmScheduler.scheduleAll(context)
        }
    }
}
