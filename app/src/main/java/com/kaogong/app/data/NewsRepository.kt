package com.kaogong.app.data

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL

data class NewsItem(
    val title: String,
    val description: String,
    val link: String = ""
)

object NewsRepository {

    private const val PREF_NAME = "news_seen"
    const val NEWS_NOTIF_ID_1 = 20
    const val NEWS_NOTIF_ID_2 = 21

    // 多个RSS源，依次尝试
    private val RSS_URLS = listOf(
        "https://feedx.net/rss/peopledaily.xml",      // 人民日报 UTF-8 代理（最可靠）
        "http://www.people.com.cn/rss/politics.xml",  // 人民日报官方
        "http://www.chinanews.com.cn/rss/scroll-news.xml"
    )

    fun markAsSeen(context: Context, title: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(title.hashCode().toString(), true).apply()
    }

    private fun isSeen(context: Context, title: String) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(title.hashCode().toString(), false)

    /** 返回最多 count 条未读新闻，全读过则重置后返回 */
    fun getNewsItems(context: Context, count: Int = 2): List<NewsItem> {
        for (url in RSS_URLS) {
            try {
                val items = fetchAll(url)
                if (items.isEmpty()) continue

                val unseen = items.filter { !isSeen(context, it.title) }
                return if (unseen.isNotEmpty()) {
                    unseen.shuffled().take(count)
                } else {
                    // 全部已读，重置后重取
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    items.shuffled().take(count)
                }
            } catch (_: Exception) { }
        }
        return emptyList()
    }

    /** 返回 1 条（兼容旧接口） */
    fun getRandomNews(context: Context): NewsItem? =
        getNewsItems(context, 1).firstOrNull()

    private fun fetchAll(rssUrl: String): List<NewsItem> {
        val conn = URL(rssUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 12_000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        if (conn.responseCode != 200) { conn.disconnect(); return emptyList() }

        val all = mutableListOf<NewsItem>()
        val parser: XmlPullParser = Xml.newPullParser()
        // null → 从 XML 声明自动检测编码（支持 GBK/UTF-8）
        parser.setInput(conn.inputStream, null)

        var inItem = false
        var tag = ""
        val titleBuf = StringBuilder()
        val descBuf  = StringBuilder()
        val linkBuf  = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    tag = parser.name ?: ""
                    if (tag == "item") {
                        inItem = true; titleBuf.clear(); descBuf.clear(); linkBuf.clear()
                    }
                }
                XmlPullParser.TEXT -> if (inItem) when (tag) {
                    "title"       -> titleBuf.append(parser.text)
                    "description" -> descBuf.append(parser.text)
                    "link"        -> linkBuf.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) {
                        val t = titleBuf.toString().trim()
                        if (t.isNotEmpty()) {
                            all.add(NewsItem(
                                title = t,
                                description = descBuf.toString().trim()
                                    .replace(Regex("<[^>]+>"), "")  // 去掉 HTML 标签
                                    .take(300),
                                link = linkBuf.toString().trim()
                            ))
                        }
                        inItem = false
                    }
                    if (parser.name != "item") tag = ""
                }
            }
            event = parser.next()
        }
        conn.disconnect()
        return all
    }
}
