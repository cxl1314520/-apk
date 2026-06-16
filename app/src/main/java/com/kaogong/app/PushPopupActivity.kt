package com.kaogong.app

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

/**
 * 全屏推送弹窗：无论锁屏、亮屏、后台，都直接占满屏幕弹出。
 * 弹出后用户可阅读内容，点击「已知」或「返回」关闭。
 */
class PushPopupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKnowledgeDetailBinding
    private var currentItem: ChineseItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 锁屏/熄屏时点亮并直接显示（最高优先级窗口标志）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON  or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON  or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        super.onCreate(savedInstanceState)
        binding = ActivityKnowledgeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_KNOWLEDGE

        if (mode == MODE_NEWS) {
            showNews()
        } else {
            showKnowledge()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    // ── 词汇模式 ─────────────────────────────────────────────────────
    private fun showKnowledge() {
        val typeOrdinal = intent.getIntExtra(EXTRA_TYPE, 0)
            .coerceIn(DataType.values().indices)
        val type = DataType.values()[typeOrdinal]
        val key  = intent.getStringExtra(EXTRA_KEY)

        val item = if (key != null) {
            ChineseDataRepository.getData(this, type).find { it.key == key }
                ?: ChineseDataRepository.getRandomItem(this, type)
        } else {
            ChineseDataRepository.getRandomItem(this, type)
        }

        currentItem = item
        if (item == null) { finish(); return }

        with(binding) {
            tvType.text = "${item.type.emoji} ${item.type.displayName}"
            tvTitle.text = item.title
            tvSubtitle.visibility = if (item.subtitle.isNotEmpty()) View.VISIBLE else View.GONE
            tvSubtitle.text = item.subtitle
            tvContent.text = item.content
            tvExtra.visibility = if (item.extra.isNotEmpty()) View.VISIBLE else View.GONE
            tvExtra.text = item.extra
            btnExclude.text = if (ExclusionManager.isExcluded(this@PushPopupActivity, item))
                "重新加入" else "已知"
            btnNext.text = "下一条 →"
        }

        binding.btnNext.setOnClickListener {
            val next = ChineseDataRepository.getRandomItem(this, type)
            currentItem = next
            if (next == null) { finish(); return@setOnClickListener }
            with(binding) {
                tvType.text = "${next.type.emoji} ${next.type.displayName}"
                tvTitle.text = next.title
                tvSubtitle.visibility = if (next.subtitle.isNotEmpty()) View.VISIBLE else View.GONE
                tvSubtitle.text = next.subtitle
                tvContent.text = next.content
                tvExtra.visibility = if (next.extra.isNotEmpty()) View.VISIBLE else View.GONE
                tvExtra.text = next.extra
                btnExclude.text = if (ExclusionManager.isExcluded(this@PushPopupActivity, next))
                    "重新加入" else "已知"
            }
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

    // ── 时政模式 ─────────────────────────────────────────────────────
    private fun showNews() {
        val title = intent.getStringExtra(EXTRA_NEWS_TITLE) ?: ""
        val desc  = intent.getStringExtra(EXTRA_NEWS_DESC)  ?: ""
        val link  = intent.getStringExtra(EXTRA_NEWS_LINK)  ?: ""

        with(binding) {
            tvType.text = "📰 时政要闻"
            tvTitle.text = title.ifEmpty { "人民日报时政" }
            tvSubtitle.visibility = View.GONE
            tvContent.text = desc.ifEmpty { "内容获取中..." }
            if (link.isNotEmpty()) {
                tvExtra.visibility = View.VISIBLE
                tvExtra.text = "🔗 来源：人民日报"
            } else {
                tvExtra.visibility = View.GONE
            }
            btnExclude.text = "已知"
            btnNext.text = "关闭"
        }

        binding.btnExclude.setOnClickListener {
            if (title.isNotEmpty()) NewsRepository.markAsSeen(this, title)
            Toast.makeText(this, "已标记为已知", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnNext.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_MODE       = "push_mode"
        const val EXTRA_TYPE       = "item_type"
        const val EXTRA_KEY        = "item_key"
        const val EXTRA_NEWS_TITLE = "news_title"
        const val EXTRA_NEWS_DESC  = "news_desc"
        const val EXTRA_NEWS_LINK  = "news_link"
        const val MODE_KNOWLEDGE   = "knowledge"
        const val MODE_NEWS        = "news"
    }
}
