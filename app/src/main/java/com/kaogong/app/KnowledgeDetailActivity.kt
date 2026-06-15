package com.kaogong.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kaogong.app.data.KnowledgeData
import com.kaogong.app.databinding.ActivityKnowledgeDetailBinding

class KnowledgeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKnowledgeDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKnowledgeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getIntExtra("knowledge_id", -1)
        val item = if (id != -1) KnowledgeData.getById(id) else KnowledgeData.getRandom()

        item?.let {
            binding.tvCategory.text = it.category
            binding.tvTitle.text = it.title
            binding.tvContent.text = it.content
            if (it.tip.isNotEmpty()) {
                binding.tvTip.visibility = View.VISIBLE
                binding.tvTip.text = "💡 考试提示：${it.tip}"
            } else {
                binding.tvTip.visibility = View.GONE
            }
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnNext.setOnClickListener {
            val nextItem = KnowledgeData.getRandom()
            binding.tvCategory.text = nextItem.category
            binding.tvTitle.text = nextItem.title
            binding.tvContent.text = nextItem.content
            if (nextItem.tip.isNotEmpty()) {
                binding.tvTip.visibility = View.VISIBLE
                binding.tvTip.text = "💡 考试提示：${nextItem.tip}"
            } else {
                binding.tvTip.visibility = View.GONE
            }
        }
    }
}
