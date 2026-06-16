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
import com.kaogong.app.data.ChineseDataRepository
import com.kaogong.app.data.DataDownloader
import com.kaogong.app.data.DataType
import com.kaogong.app.data.ExclusionManager
import com.kaogong.app.data.NewsRepository
import com.kaogong.app.data.model.ChineseItem
import com.kaogong.app.databinding.ActivityMainBinding
import com.kaogong.app.notification.NotificationHelper
import com.kaogong.app.scheduler.AlarmScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentType = DataType.IDIOM
    private var currentItem: ChineseItem? = null

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
        DataType.values().forEach { type ->
            val chip = Chip(this).apply {
                text = "${type.emoji} ${type.displayName}"
                isCheckable = true
                isChecked = (type == currentType)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) { currentType = type; refreshItem() }
                }
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener { refreshItem() }

        binding.btnExclude.setOnClickListener {
            val item = currentItem ?: return@setOnClickListener
            if (ExclusionManager.isExcluded(this, item)) {
                ExclusionManager.include(this, item)
                binding.btnExclude.text = "已知"
                Toast.makeText(this, "「${item.title}」已重新加入推送", Toast.LENGTH_SHORT).show()
            } else {
                ExclusionManager.exclude(this, item)
                binding.btnExclude.text = "重新加入"
                Toast.makeText(this, "「${item.title}」已标记为已知，自动换下一条", Toast.LENGTH_SHORT).show()
                refreshItem()
            }
        }

        binding.btnRetryDownload.setOnClickListener { startDownload() }
    }

    private fun checkAndDownload() {
        if (DataDownloader.allDownloaded(this)) {
            showContent()
        } else {
            startDownload()
        }
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

    /** 打开App时立即推送一次（4种词汇 + 时政） */
    private fun triggerImmediatePush() {
        DataType.values().forEach { type ->
            ChineseDataRepository.getRandomItem(this, type)?.let {
                NotificationHelper.showItemNotification(this, it)
            }
        }
        Thread {
            try {
                NewsRepository.getRandomNews(this)?.let {
                    NotificationHelper.showNewsNotification(this, it)
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
            binding.tvNoData.text = if (!hasFile) "数据未下载，请检查网络连接" else "该分类所有内容均已标记为已知\n可点击「重新加入」按钮恢复"
            return
        }
        binding.tvNoData.visibility = View.GONE
        binding.cardContent.visibility = View.VISIBLE
        currentItem = item
        with(binding) {
            tvType.text    = "${item.type.emoji} ${item.type.displayName}"
            tvTitle.text   = item.title
            tvSubtitle.visibility = if (item.subtitle.isNotEmpty()) View.VISIBLE else View.GONE
            tvSubtitle.text = item.subtitle
            tvContent.text = item.content
            tvExtra.visibility = if (item.extra.isNotEmpty()) View.VISIBLE else View.GONE
            tvExtra.text   = item.extra
            btnExclude.text = if (ExclusionManager.isExcluded(this@MainActivity, item))
                "重新加入" else "已知"
        }
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
