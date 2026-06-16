package com.kaogong.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kaogong.app.data.ChineseDataRepository
import com.kaogong.app.data.DataType
import com.kaogong.app.data.ExclusionManager
import com.kaogong.app.data.model.ChineseItem
import com.kaogong.app.databinding.ActivityKnowledgeDetailBinding

class KnowledgeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKnowledgeDetailBinding
    private var currentItem: ChineseItem? = null
    private var currentType = DataType.IDIOM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKnowledgeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ordinal = intent.getIntExtra(EXTRA_TYPE, DataType.IDIOM.ordinal)
        val key     = intent.getStringExtra(EXTRA_KEY)
        currentType = DataType.values()[ordinal.coerceIn(DataType.values().indices)]

        val item = if (key != null) {
            ChineseDataRepository.getData(this, currentType).find { it.key == key }
                ?: ChineseDataRepository.getRandomItem(this, currentType)
        } else {
            ChineseDataRepository.getRandomItem(this, currentType)
        }
        showItem(item)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnNext.setOnClickListener {
            showItem(ChineseDataRepository.getRandomItem(this, currentType))
        }

        binding.btnExclude.setOnClickListener {
            val it = currentItem ?: return@setOnClickListener
            if (ExclusionManager.isExcluded(this, it)) {
                ExclusionManager.include(this, it)
                binding.btnExclude.text = "不再推送"
                Toast.makeText(this, "「${it.title}」已重新加入推送", Toast.LENGTH_SHORT).show()
            } else {
                ExclusionManager.exclude(this, it)
                binding.btnExclude.text = "重新加入推送"
                Toast.makeText(this, "「${it.title}」已从推送列表移除", Toast.LENGTH_SHORT).show()
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
                    this@KnowledgeDetailActivity, item)) "重新加入推送" else "不再推送"
        }
    }

    companion object {
        const val EXTRA_TYPE = "item_type"
        const val EXTRA_KEY  = "item_key"
    }
}
