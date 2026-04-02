package com.simats.kisancareai

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PersonalizedTipsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personalized_tips)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        // Set dynamic dates and tips
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Today
        val today = calendar.time
        findViewById<TextView>(R.id.tv_date_1).text = dateFormat.format(today)
        val index1 = DailyTipsManager.getTipIndexForDate(today)
        findViewById<TextView>(R.id.tv_title_1).setText(DailyTipsManager.getTipTitleResId(index1))
        findViewById<TextView>(R.id.tv_desc_1).setText(DailyTipsManager.getTipDescResId(index1))
        
        // Yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.time
        findViewById<TextView>(R.id.tv_date_2).text = dateFormat.format(yesterday)
        val index2 = DailyTipsManager.getTipIndexForDate(yesterday)
        findViewById<TextView>(R.id.tv_title_2).setText(DailyTipsManager.getTipTitleResId(index2))
        findViewById<TextView>(R.id.tv_desc_2).setText(DailyTipsManager.getTipDescResId(index2))
        
        // Day before yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val dayBefore = calendar.time
        findViewById<TextView>(R.id.tv_date_3).text = dateFormat.format(dayBefore)
        val index3 = DailyTipsManager.getTipIndexForDate(dayBefore)
        findViewById<TextView>(R.id.tv_title_3).setText(DailyTipsManager.getTipTitleResId(index3))
        findViewById<TextView>(R.id.tv_desc_3).setText(DailyTipsManager.getTipDescResId(index3))
    }
}
