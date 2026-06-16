package com.kaogong.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import android.content.Intent
import com.kaogong.app.data.ChineseDataRepository
import com.kaogong.app.data.DataDownloader
import com.kaogong.app.data.DataType
import com.kaogong.app.data.ExclusionManager
import com.kaogong.app.data.NewsItem
import com.kaogong.app.data.NewsRepository
import com.kaogong.app.data.model.ChineseItem
import com.kaogong.app.databinding.ActivityMainBinding
import com.kaogong.app.notification.NotificationHelper
import com.kaogong.app.scheduler.AlarmScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentType = DataType.IDIOM
    private var currentItem: ChineseItem? = null

    private var isNewsMode = false
    private var currentNewsItem: NewsItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        AlarmScheduler.scheduleAll(this)

        setupCategoryChips()
        setupButtons()
        checkAndDownload()
    }

    private fun setupCategoryChips() {
        // 3种词汇分类
        DataType.values().forEach { type ->
            val chip = Chip(this).apply {
                text = "${type.emoji} ${type.displayName}"
                isCheckable = true
                isChecked = (type == currentType)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        isNewsMode = false
                        currentType = type
                        refreshItem()
                    }
                }
            }
            binding.chipGroup.addView(chip)
        }

        // 时政分类（独立于 DataType）
        val newsChip = Chip(this).apply {
            text = "📰 时政"
            isCheckable = true
            setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    isNewsMode = true
                    loadAndShowNews()
                }
            }
        }
        binding.chipGroup.addView(newsChip)
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            if (isNewsMode) loadAndShowNews() else refreshItem()
        }

        binding.btnExclude.setOnClickListener {
            if (isNewsMode) {
                val news = currentNewsItem ?: return@setOnClickListener
                NewsRepository.markAsSeen(this, news.title)
                binding.btnExclude.text = "已知"
                Toast.makeText(this, "已标记为已知", Toast.LENGTH_SHORT).show()
                loadAndShowNews()
            } else {
                val item = currentItem ?: return@setOnClickListener
                if (ExclusionManager.isExcluded(this, item)) {
                    ExclusionManager.include(this, item)
                    binding.btnExclude.text = "已知"
                    Toast.makeText(this, "「${item.title}」已重新加入推送", Toast.LENGTH_SHORT).show()
                } else {
                    ExclusionManager.exclude(this, item)
                    binding.btnExclude.text = "重新加入"
                    Toast.makeText(this, "「${item.title}」已标记为已知", Toast.LENGTH_SHORT).show()
                    refreshItem()
                }
            }
        }

        binding.btnRetryDownload.setOnClickListener { startDownload() }
    }

    private fun checkAndDownload() {
        if (DataDownloader.allDownloaded(this)) showContent()
        else startDownload()
    }

    private fun startDownload() {
        binding.layoutDownloading.visibility = View.VISIBLE
        binding.layoutContent.visibility = View.GONE
        binding.tvDownloadStatus.text = "⏳ 正在从 GitHub 下载词汇数据..."
        binding.btnRetryDownload.visibility = View.GONE

        Thread {
            var anyFailed = false
            DataDownloader.downloadAll(this) { type, ok ->
                if (!ok) anyFailed = true
                runOnUiThread {
                    binding.tvDownloadStatus.text =
                        if (ok) "✅ ${type.displayName}（${type.emoji}）下载完成"
                        else    "❌ ${type.displayName}（${type.emoji}）下载失败，请检查网络"
                }
            }
            runOnUiThread {
                if (anyFailed) binding.btnRetryDownload.visibility = View.VISIBLE
                showContent()
            }
        }.start()
    }

    private fun showContent() {
        binding.layoutDownloading.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE
        refreshItem()
        triggerImmediatePush()
    }

    /** 打开App时立即推送一次（直接弹出全屏界面 + 通知兜底） */
    private fun triggerImmediatePush() {
        DataType.values().forEach { type ->
            val item = ChineseDataRepository.getRandomItem(this, type) ?: return@forEach
            // 直接弹出全屏弹窗
            startActivity(Intent(this, PushPopupActivity::class.java).apply {
                putExtra(PushPopupActivity.EXTRA_MODE, PushPopupActivity.MODE_KNOWLEDGE)
                putExtra(PushPopupActivity.EXTRA_TYPE, type.ordinal)
                putExtra(PushPopupActivity.EXTRA_KEY, item.key)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            // 同时发通知（锁屏/后台兜底）
            NotificationHelper.showItemNotification(this, item)
        }
        Thread {
            try {
                val newsItems = NewsRepository.getNewsItems(this, 2)
                newsItems.forEachIndexed { index, news ->
                    val notifId = if (index == 0) NewsRepository.NEWS_NOTIF_ID_1
                                  else             NewsRepository.NEWS_NOTIF_ID_2
                    startActivity(Intent(this, PushPopupActivity::class.java).apply {
                        putExtra(PushPopupActivity.EXTRA_MODE, PushPopupActivity.MODE_NEWS)
                        putExtra(PushPopupActivity.EXTRA_NEWS_TITLE, news.title)
                        putExtra(PushPopupActivity.EXTRA_NEWS_DESC, news.description)
                        putExtra(PushPopupActivity.EXTRA_NEWS_LINK, news.link)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    NotificationHelper.showNewsNotification(this, news, notifId)
                }
            } catch (_: Exception) { }
        }.start()
    }

    private fun refreshItem() {
        val item = ChineseDataRepository.getRandomItem(this, currentType)
        if (item == null) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.cardContent.visibility = View.GONE
            val hasFile = DataDownloader.isDownloaded(this, currentType)
            binding.tvNoData.text = if (!hasFile) "数据未下载，请检查网络连接"
                                    else "该分类所有内容均已标记为已知\n可点击「重新加入」恢复"
            return
        }
        binding.tvNoData.visibility = View.GONE
        binding.cardContent.visibility = View.VISIBLE
        currentItem = item
        with(binding) {
            tvType.text = "${item.type.emoji} ${item.type.displayName}"
            tvTitle.text = item.title
            tvSubtitle.visibility = if (item.subtitle.isNotEmpty()) View.VISIBLE else View.GONE
            tvSubtitle.text = item.subtitle
            tvContent.text = item.content
            tvExtra.visibility = if (item.extra.isNotEmpty()) View.VISIBLE else View.GONE
            tvExtra.text = item.extra
            btnExclude.text = if (ExclusionManager.isExcluded(this@MainActivity, item))
                "重新加入" else "已知"
            btnNext.text = "🔀 下一条"
        }
    }

    /** 时政 tab：后台拉取新闻并展示 */
    private fun loadAndShowNews() {
        binding.tvNoData.visibility = View.GONE
        binding.cardContent.visibility = View.VISIBLE
        with(binding) {
            tvType.text = "📰 时政要闻"
            tvTitle.text = "正在获取人民日报内容..."
            tvSubtitle.visibility = View.GONE
            tvContent.text = ""
            tvExtra.visibility = View.GONE
            btnExclude.text = "已知"
            btnNext.text = "🔀 换一条"
        }

        Thread {
            val news = NewsRepository.getRandomNews(this)
            runOnUiThread {
                if (news != null) {
                    currentNewsItem = news
                    binding.tvTitle.text = news.title
                    binding.tvContent.text = news.description
                    if (news.link.isNotEmpty()) {
                        binding.tvExtra.visibility = View.VISIBLE
                        binding.tvExtra.text = "🔗 来源：人民日报\n点击「换一条」查看更多时政"
                    }
                } else {
                    binding.tvTitle.text = "暂未获取到时政内容"
                    binding.tvContent.text = "网络不可用或人民日报 RSS 暂时无法访问，请稍后重试"
                }
            }
        }.start()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
            )
        }
    }
}
