package com.kaogong.app.data

import android.content.Context
import com.kaogong.app.data.model.ChineseItem

object ExclusionManager {

    private fun prefs(context: Context) =
        context.getSharedPreferences("exclusions", Context.MODE_PRIVATE)

    private fun prefKey(type: DataType) = "excl_${type.name}"

    fun getExcluded(context: Context, type: DataType): Set<String> =
        prefs(context).getStringSet(prefKey(type), emptySet()) ?: emptySet()

    fun exclude(context: Context, item: ChineseItem) {
        val set = getExcluded(context, item.type).toMutableSet().also { it.add(item.key) }
        prefs(context).edit().putStringSet(prefKey(item.type), set).apply()
    }

    fun include(context: Context, item: ChineseItem) {
        val set = getExcluded(context, item.type).toMutableSet().also { it.remove(item.key) }
        prefs(context).edit().putStringSet(prefKey(item.type), set).apply()
    }

    fun isExcluded(context: Context, item: ChineseItem): Boolean =
        item.key in getExcluded(context, item.type)
}
