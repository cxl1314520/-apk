package com.kaogong.app.data

data class KnowledgeItem(
    val id: Int,
    val category: String,
    val title: String,
    val content: String,
    val tip: String = ""
)

enum class Category(val label: String) {
    CHANSHI("常识判断"),
    YANYU("言语理解"),
    PANDUAN("判断推理"),
    SHULIANG("数量关系"),
    ZILIAO("资料分析"),
    SHENLUN("申论技巧")
}
