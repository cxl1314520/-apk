package com.kaogong.app.data.model

import com.kaogong.app.data.DataType

data class ChineseItem(
    val type: DataType,
    val key: String,
    val title: String,
    val subtitle: String = "",
    val content: String,
    val extra: String = ""
)
