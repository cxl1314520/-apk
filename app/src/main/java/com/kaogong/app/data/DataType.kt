package com.kaogong.app.data

enum class DataType(
    val displayName: String,
    val emoji: String,
    val fileName: String,
    val downloadUrl: String
) {
    IDIOM(
        "成语", "📖", "idiom.json",
        "https://raw.githubusercontent.com/pwxcoo/chinese-xinhua/master/data/idiom.json"
    ),
    XIEHOUYU(
        "歇后语", "🎭", "xiehouyu.json",
        "https://raw.githubusercontent.com/pwxcoo/chinese-xinhua/master/data/xiehouyu.json"
    ),
    WORD(
        "词语", "📝", "word.json",
        "https://raw.githubusercontent.com/pwxcoo/chinese-xinhua/master/data/word.json"
    )
}
