package com.kaogong.app.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object DataDownloader {

    fun downloadAll(
        context: Context,
        onEachDone: (type: DataType, success: Boolean) -> Unit
    ) {
        DataType.values().forEach { type ->
            val ok = download(context, type)
            if (ok) ChineseDataRepository.clearCache(type)
            onEachDone(type, ok)
        }
    }

    fun download(context: Context, type: DataType): Boolean = try {
        val conn = URL(type.downloadUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout    = 90_000
        conn.connect()
        if (conn.responseCode != 200) {
            conn.disconnect(); false
        } else {
            val dest = File(context.filesDir, type.fileName)
            conn.inputStream.use { src -> dest.outputStream().use { src.copyTo(it) } }
            conn.disconnect()
            true
        }
    } catch (e: Exception) {
        e.printStackTrace(); false
    }

    fun isDownloaded(context: Context, type: DataType): Boolean {
        val f = File(context.filesDir, type.fileName)
        return f.exists() && f.length() > 1000L
    }

    fun allDownloaded(context: Context) = DataType.values().all { isDownloaded(context, it) }
}
