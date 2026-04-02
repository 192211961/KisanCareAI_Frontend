package com.simats.kisancareai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedAdviceActivity : BaseActivity() {

    private lateinit var container: LinearLayout
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saved_advice)

        container = findViewById(R.id.saved_items_container)
        tvCount = findViewById(R.id.tv_items_count)
        tvEmpty = findViewById(R.id.tv_empty_state)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        findViewById<Button>(R.id.btn_export).setOnClickListener {
            exportAllAdvice()
        }

        loadSavedAdvice()
    }

    private fun exportAllAdvice() {
        val sharedPrefs = getSharedPreferences("kisanmitra_prefs", Context.MODE_PRIVATE)
        val savedData = sharedPrefs.getString("saved_advice", "[]")
        val jsonArray = JSONArray(savedData)

        if (jsonArray.length() == 0) {
            android.widget.Toast.makeText(this, "No advice to export", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        
        val headerPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = android.graphics.Color.DKGRAY
        }

        val bodyPaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.BLACK
        }

        var y = 50f
        canvas.drawText("KisanCare AI - Saved Agricultural Advice", 50f, y, titlePaint)
        y += 40f

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val query = item.optString("query")
            val advice = item.optString("advice")
            val timestamp = item.optLong("timestamp")

            if (y > 750f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }

            canvas.drawText("Query: $query", 50f, y, headerPaint)
            y += 20f
            canvas.drawText("Date: ${sdf.format(Date(timestamp))}", 50f, y, bodyPaint)
            y += 25f
            
            // Text Wrapping Logic
            val margin = 50f
            val maxWidth = 500f
            val words = advice.split(" ")
            var line = ""
            
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (bodyPaint.measureText(testLine) > maxWidth) {
                    canvas.drawText(line, margin, y, bodyPaint)
                    y += 20f
                    line = word
                    
                    if (y > 800f) {
                        pdfDocument.finishPage(page)
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = 50f
                    }
                } else {
                    line = testLine
                }
            }
            canvas.drawText(line, margin, y, bodyPaint)
            y += 35f
            
            // Separator
            val linePaint = Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 1f
            }
            canvas.drawLine(margin, y, margin + maxWidth, y, linePaint)
            y += 35f
        }

        pdfDocument.finishPage(page)

        val file = File(cacheDir, "KisanCareAi_Advice_Report.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Advice PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Failed to generate PDF", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedAdvice() {
        container.removeAllViews()
        val sharedPrefs = getSharedPreferences("kisanmitra_prefs", Context.MODE_PRIVATE)
        val savedData = sharedPrefs.getString("saved_advice", "[]")
        val jsonArray = JSONArray(savedData)

        tvCount.text = "${jsonArray.length()} items saved"

        if (jsonArray.length() == 0) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            for (i in (jsonArray.length() - 1) downTo 0) {
                val item = jsonArray.getJSONObject(i)
                addSavedItemToView(item, i)
            }
        }
    }

    private fun addSavedItemToView(item: JSONObject, index: Int) {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_saved_advice, container, false)
        
        val tvTitle = itemView.findViewById<TextView>(R.id.tv_item_title)
        val tvDate = itemView.findViewById<TextView>(R.id.tv_item_date)
        val tvExcerpt = itemView.findViewById<TextView>(R.id.tv_item_excerpt)
        val btnViewFull = itemView.findViewById<Button>(R.id.btn_view_full)
        val btnShare = itemView.findViewById<ImageView>(R.id.btn_share)
        val btnDelete = itemView.findViewById<ImageView>(R.id.btn_delete)

        val query = item.optString("query", "Farming Query")
        val timestamp = item.optLong("timestamp", System.currentTimeMillis())
        val adviceText = item.optString("advice", "Detailed AI recommendation for your farming needs.")
        val isDisease = item.optBoolean("isDisease", false)

        tvTitle.text = query
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        tvDate.text = "Saved on ${sdf.format(Date(timestamp))}"
        tvExcerpt.text = if (adviceText.length > 100) adviceText.take(100) + "..." else adviceText

        btnViewFull.setOnClickListener {
            val intent = Intent(this, AiResponseActivity::class.java)
            intent.putExtra("QUERY_TEXT", query)
            intent.putExtra("IS_DISEASE_ANALYSIS", isDisease)
            intent.putExtra("PRECALCULATED_ADVICE", adviceText)
            startActivity(intent)
        }

        btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                val shareBody = "🌾 *KisanCare AI Farming Advice*\n\n*Query:* $query\n\n*Advice:*\n$adviceText\n\n_Shared via KisanCare AI App_"
                putExtra(Intent.EXTRA_SUBJECT, "Farming Advice: $query")
                putExtra(Intent.EXTRA_TEXT, shareBody)
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        btnDelete.setOnClickListener {
            deleteItem(index)
        }

        container.addView(itemView)
    }

    private fun deleteItem(index: Int) {
        val sharedPrefs = getSharedPreferences("kisanmitra_prefs", Context.MODE_PRIVATE)
        val savedData = sharedPrefs.getString("saved_advice", "[]")
        val jsonArray = JSONArray(savedData)
        
        val newArray = JSONArray()
        for (i in 0 until jsonArray.length()) {
            if (i != index) {
                newArray.put(jsonArray.get(i))
            }
        }

        sharedPrefs.edit().putString("saved_advice", newArray.toString()).apply()
        loadSavedAdvice()
    }
}
