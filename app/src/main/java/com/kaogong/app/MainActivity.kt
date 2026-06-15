package com.kaogong.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kaogong.app.data.KnowledgeData
import com.kaogong.app.databinding.ActivityMainBinding
import com.kaogong.app.notification.NotificationHelper
import com.kaogong.app.scheduler.AlarmScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentItem = KnowledgeData.getRandom()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        AlarmScheduler.scheduleAll(this)

        showCurrentKnowledge()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        // 每次打开自动刷新一个新知识
        currentItem = KnowledgeData.getRandom()
        showCurrentKnowledge()
    }

    private fun showCurrentKnowledge() {
        binding.apply {
            tvCategory.text = currentItem.category
            tvTitle.text = currentItem.title
            tvContent.text = currentItem.content
            if (currentItem.tip.isNotEmpty()) {
                tvTip.visibility = View.VISIBLE
                tvTip.text = "💡 ${currentItem.tip}"
            } else {
                tvTip.visibility = View.GONE
            }
        }
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            currentItem = KnowledgeData.getRandom()
            showCurrentKnowledge()
        }

        binding.btnChanshi.setOnClickListener {
            currentItem = KnowledgeData.getRandomByCategory("常识判断")
            showCurrentKnowledge()
        }

        binding.btnYanyu.setOnClickListener {
            currentItem = KnowledgeData.getRandomByCategory("言语理解")
            showCurrentKnowledge()
        }

        binding.btnPanduan.setOnClickListener {
            currentItem = KnowledgeData.getRandomByCategory("判断推理")
            showCurrentKnowledge()
        }

        binding.btnShuliang.setOnClickListener {
            currentItem = KnowledgeData.getRandomByCategory("数量关系")
            showCurrentKnowledge()
        }

        binding.btnZiliao.setOnClickListener {
            currentItem = KnowledgeData.getRandomByCategory("资料分析")
            showCurrentKnowledge()
        }

        binding.btnShenlun.setOnClickListener {
            currentItem = KnowledgeData.getRandomByCategory("申论技巧")
            showCurrentKnowledge()
        }

        binding.btnDetail.setOnClickListener {
            val intent = Intent(this, KnowledgeDetailActivity::class.java).apply {
                putExtra("knowledge_id", currentItem.id)
            }
            startActivity(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "请开启通知权限以接收每日知识推送", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "✅ 已开启通知，将在8:00-22:00每2小时推送知识", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATION = 1001
    }
}
