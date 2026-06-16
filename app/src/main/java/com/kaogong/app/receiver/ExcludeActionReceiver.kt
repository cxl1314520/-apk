package com.kaogong.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.kaogong.app.data.ChineseDataRepository
import com.kaogong.app.data.DataType
import com.kaogong.app.data.ExclusionManager
import com.kaogong.app.data.NewsRepository
import com.kaogong.app.data.model.ChineseItem

class ExcludeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ordinal = intent.getIntExtra(EXTRA_TYPE_ORDINAL, 0)
        val key     = intent.getStringExtra(EXTRA_ITEM_KEY) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)

        if (ordinal == TYPE_ORDINAL_NEWS) {
            NewsRepository.markAsSeen(context, key)
            if (notifId >= 0) context.getSystemService(NotificationManager::class.java).cancel(notifId)
            Toast.makeText(context, "时政已标记为已知", Toast.LENGTH_SHORT).show()
            return
        }

        if (ordinal !in DataType.values().indices) return
        val type = DataType.values()[ordinal]
        val item = ChineseDataRepository.getData(context, type).find { it.key == key }
            ?: ChineseItem(type, key, key, "", "")
        ExclusionManager.exclude(context, item)

        if (notifId >= 0) context.getSystemService(NotificationManager::class.java).cancel(notifId)
        Toast.makeText(context, "「${item.title}」已标记为已知", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_TYPE_ORDINAL  = "type_ordinal"
        const val EXTRA_ITEM_KEY      = "item_key"
        const val EXTRA_NOTIF_ID      = "notif_id"
        const val TYPE_ORDINAL_NEWS   = -1
    }
}
