package com.kaogong.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kaogong.app.data.ChineseDataRepository
import com.kaogong.app.data.DataType
import com.kaogong.app.data.ExclusionManager
import com.kaogong.app.data.NewsRepository
import com.kaogong.app.data.model.ChineseItem
import com.kaogong.app.databinding.ActivityKnowledgeDetailBinding

class KnowledgeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKnowledgeDetailBinding
    private var currentItem: ChineseItem? = null
    private var currentType = DataType.IDIOM
    private var isNewsMode = false
    private var newsLink = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // 锁屏时直接点亮屏幕展示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        super.onCreate(savedInstanceState)
        binding = ActivityKnowledgeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val typeValue = intent.getIntExtra(EXTRA_TYPE, DataType.IDIOM.ordinal)

        if (typeValue == TYPE_NEWS) {
            setupNewsMode()
        } else {
            setupKnowledgeMode(typeValue)
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    // ── 时政模式 ──────────────────────────────────────────────────
    private fun setupNewsMode() {
        isNewsMode = true
        val title = intent.getStringExtra(EXTRA_NEWS_TITLE) ?: "时政要闻"
        val desc  = intent.getStringExtra(EXTRA_NEWS_DESC)  ?: ""
        newsLink  = intent.getStringExtra(EXTRA_NEWS_LINK)  ?: ""

        with(binding) {
            tvType.text = "📰 时政要闻"
            tvTitle.text = title
            tvSubtitle.visibility = View.GONE
            tvContent.text = desc.ifEmpty { "内容加载中..." }
            if (newsLink.isNotEmpty()) {
                tvExtra.visibility = View.VISIBLE
                tvExtra.text = "🔗 点击「打开原文」可查看完整报道"
            } else {
                tvExtra.visibility = View.GONE
            }
            btnExclude.text = "已知"
            btnNext.text = "打开原文 →"
        }

        binding.btnExclude.setOnClickListener {
            NewsRepository.markAsSeen(this, title)
            Toast.makeText(this, "已标记为已知", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnNext.setOnClickListener {
            if (newsLink.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(newsLink)))
            } else {
                Toast.makeText(this, "暂无原文链接", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── 知识词条模式 ───────────────────────────────────────────────
    private fun setupKnowledgeMode(typeOrdinal: Int) {
        currentType = DataType.values()[typeOrdinal.coerceIn(DataType.values().indices)]
        val key = intent.getStringExtra(EXTRA_KEY)
        val item = if (key != null) {
            ChineseDataRepository.getData(this, currentType).find { it.key == key }
                ?: ChineseDataRepository.getRandomItem(this, currentType)
        } else {
            ChineseDataRepository.getRandomItem(this, currentType)
        }
        showItem(item)

        binding.btnNext.text = "下一条 →"
        binding.btnNext.setOnClickListener {
            showItem(ChineseDataRepository.getRandomItem(this, currentType))
        }

        binding.btnExclude.setOnClickListener {
            val it = currentItem ?: return@setOnClickListener
            if (ExclusionManager.isExcluded(this, it)) {
                ExclusionManager.include(this, it)
                binding.btnExclude.text = "已知"
                Toast.makeText(this, "「${it.title}」已重新加入推送", Toast.LENGTH_SHORT).show()
            } else {
                ExclusionManager.exclude(this, it)
                binding.btnExclude.text = "重新加入"
                Toast.makeText(this, "「${it.title}」已标记为已知", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showItem(item: ChineseItem?) {
        currentItem = item ?: return
        with(binding) {
            tvType.text = "${item.type.emoji} ${item.type.displayName}"
            tvTitle.text = item.title
            tvSubtitle.visibility = if (item.subtitle.isNotEmpty()) View.VISIBLE else View.GONE
            tvSubtitle.text = item.subtitle
            tvContent.text = item.content
            tvExtra.visibility = if (item.extra.isNotEmpty()) View.VISIBLE else View.GONE
            tvExtra.text = item.extra
            btnExclude.text = if (ExclusionManager.isExcluded(
                    this@KnowledgeDetailActivity, item)) "重新加入" else "已知"
        }
    }

    companion object {
        const val EXTRA_TYPE       = "item_type"
        const val EXTRA_KEY        = "item_key"
        const val EXTRA_NEWS_TITLE = "news_title"
        const val EXTRA_NEWS_DESC  = "news_desc"
        const val EXTRA_NEWS_LINK  = "news_link"
        const val TYPE_NEWS        = -1
    }
}
