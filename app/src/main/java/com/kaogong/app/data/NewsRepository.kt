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
    private const val NEWS_NOTIF_ID = 20

    private val RSS_URLS = listOf(
        "http://www.people.com.cn/rss/politics.xml",
        "http://www.chinanews.com.cn/rss/scroll-news.xml"
    )

    fun getNotifId() = NEWS_NOTIF_ID

    fun markAsSeen(context: Context, title: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(title.hashCode().toString(), true).apply()
    }

    private fun isSeen(context: Context, title: String) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(title.hashCode().toString(), false)

    fun getRandomNews(context: Context): NewsItem? {
        for (url in RSS_URLS) {
            try {
                val item = fetchFirst(context, url)
                if (item != null) return item
            } catch (_: Exception) { }
        }
        return null
    }

    private fun fetchFirst(context: Context, rssUrl: String): NewsItem? {
        val conn = URL(rssUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 5_000
        conn.readTimeout = 8_000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        if (conn.responseCode != 200) { conn.disconnect(); return null }

        val all = mutableListOf<NewsItem>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(conn.inputStream, null) // null → detect encoding from XML declaration

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
                                description = descBuf.toString().trim().take(200),
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

        val unseen = all.filter { !isSeen(context, it.title) }
        if (unseen.isEmpty() && all.isNotEmpty()) {
            // All seen — reset and re-use
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            return all.shuffled().firstOrNull()
        }
        return unseen.shuffled().firstOrNull()
    }
}
