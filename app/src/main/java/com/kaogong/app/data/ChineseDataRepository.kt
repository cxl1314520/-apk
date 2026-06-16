package com.kaogong.app.data

import android.content.Context
import android.util.JsonReader
import com.kaogong.app.data.model.ChineseItem
import java.io.File

object ChineseDataRepository {

    private val cache = HashMap<DataType, List<ChineseItem>>()

    @Synchronized
    fun getData(context: Context, type: DataType): List<ChineseItem> {
        cache[type]?.let { return it }
        val file = File(context.filesDir, type.fileName)
        if (!file.exists() || file.length() < 100) return emptyList()
        return parseFile(type, file).also { cache[type] = it }
    }

    fun clearCache(type: DataType) = cache.remove(type)

    fun getRandomItem(context: Context, type: DataType): ChineseItem? {
        val excluded = ExclusionManager.getExcluded(context, type)
        return getData(context, type).filter { it.key !in excluded }.randomOrNull()
    }

    fun isAvailable(context: Context, type: DataType): Boolean {
        val f = File(context.filesDir, type.fileName)
        return f.exists() && f.length() > 1000
    }

    private fun parseFile(type: DataType, file: File): List<ChineseItem> {
        val items = mutableListOf<ChineseItem>()
        try {
            JsonReader(file.bufferedReader()).use { reader ->
                reader.beginArray()
                while (reader.hasNext()) {
                    parseItem(reader, type)?.let { items.add(it) }
                }
                reader.endArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    private fun parseItem(reader: JsonReader, type: DataType): ChineseItem? = when (type) {
        DataType.IDIOM      -> parseIdiom(reader)
        DataType.XIEHOUYU  -> parseXiehouyu(reader)
        DataType.WORD       -> parseWord(reader)
        DataType.CHARACTER  -> parseCharacter(reader)
    }

    private fun parseIdiom(reader: JsonReader): ChineseItem? {
        var word = ""; var pinyin = ""; var explanation = ""; var example = ""; var derivation = ""
        reader.beginObject()
        while (reader.hasNext()) when (reader.nextName()) {
            "word"        -> word        = reader.nextString()
            "pinyin"      -> pinyin      = reader.nextString()
            "explanation" -> explanation = reader.nextString()
            "example"     -> example     = reader.nextString()
            "derivation"  -> derivation  = reader.nextString()
            else          -> reader.skipValue()
        }
        reader.endObject()
        if (word.isEmpty()) return null
        val extra = buildString {
            if (derivation.isNotEmpty() && derivation != "null") append("【出处】$derivation\n")
            if (example.isNotEmpty() && example != "null") append("【例句】$example")
        }.trim()
        return ChineseItem(DataType.IDIOM, word, word, pinyin, explanation.ifEmpty { "暂无释义" }, extra)
    }

    private fun parseXiehouyu(reader: JsonReader): ChineseItem? {
        var riddle = ""; var answer = ""
        reader.beginObject()
        while (reader.hasNext()) when (reader.nextName()) {
            "riddle" -> riddle = reader.nextString()
            "answer" -> answer = reader.nextString()
            else     -> reader.skipValue()
        }
        reader.endObject()
        if (riddle.isEmpty()) return null
        return ChineseItem(DataType.XIEHOUYU, riddle, riddle, "歇后语", answer.ifEmpty { "（答案略）" }, "")
    }

    private fun parseWord(reader: JsonReader): ChineseItem? {
        var word = ""; var explanation = ""
        reader.beginObject()
        while (reader.hasNext()) when (reader.nextName()) {
            "word", "ci"  -> word        = reader.nextString()
            "explanation" -> explanation = reader.nextString()
            else          -> reader.skipValue()
        }
        reader.endObject()
        if (word.isEmpty()) return null
        return ChineseItem(DataType.WORD, word, word, "词语", explanation.ifEmpty { "暂无释义" }, "")
    }

    private fun parseCharacter(reader: JsonReader): ChineseItem? {
        var char = ""; var pinyin = ""; var definition = ""; var radical = ""
        reader.beginObject()
        while (reader.hasNext()) when (reader.nextName()) {
            "character"  -> char       = reader.nextString()
            "pinyin"     -> pinyin     = reader.nextString()
            "definition" -> definition = reader.nextString()
            "radical"    -> radical    = reader.nextString()
            else         -> reader.skipValue()
        }
        reader.endObject()
        if (char.isEmpty()) return null
        val extra = if (radical.isNotEmpty()) "【部首】$radical" else ""
        return ChineseItem(DataType.CHARACTER, char, char, pinyin, definition.ifEmpty { "暂无释义" }, extra)
    }
}
