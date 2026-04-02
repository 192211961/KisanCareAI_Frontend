package com.simats.kisancareai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QueryHistoryActivity : BaseActivity() {

    private lateinit var container: LinearLayout
    private lateinit var emptyState: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_query_history)

        container = findViewById(R.id.history_items_container)
        emptyState = findViewById(R.id.empty_state)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        findViewById<TextView>(R.id.btn_clear_all).setOnClickListener {
            if (container.childCount > 0) {
                showClearHistoryDialog()
            }
        }

        loadQueryHistory()
    }

    private fun showClearHistoryDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all query history?")
            .setPositiveButton("Clear") { _, _ ->
                clearHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearHistory() {
        val sharedPrefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("email", "") ?: ""
        
        if (email.isEmpty()) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.backendApi.clearHistory(email)
                if (response.isSuccessful) {
                    Toast.makeText(this@QueryHistoryActivity, "History cleared", Toast.LENGTH_SHORT).show()
                    loadQueryHistory()
                } else {
                    Toast.makeText(this@QueryHistoryActivity, "Failed to clear history", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@QueryHistoryActivity, "Connection Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadQueryHistory() {
        val sharedPrefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("email", "") ?: ""
        
        if (email.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.backendApi.getHistory(email)
                if (response.isSuccessful) {
                    val historyList = response.body() ?: emptyList()
                    container.removeAllViews()
                    
                    if (historyList.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                    } else {
                        emptyState.visibility = View.GONE
                        for (item in historyList) {
                            addHistoryItemToView(item)
                        }
                    }
                } else {
                    emptyState.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                android.util.Log.e("QueryHistory", "Error", e)
                emptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun addHistoryItemToView(item: HistoryItem) {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_query_history, container, false)
        
        val tvQuery = itemView.findViewById<TextView>(R.id.tv_history_query)
        val tvDate = itemView.findViewById<TextView>(R.id.tv_history_date)
        val ivIcon = itemView.findViewById<ImageView>(R.id.iv_icon)
        val btnDelete = itemView.findViewById<ImageView>(R.id.btn_delete_item)

        tvQuery.text = item.query
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        tvDate.text = sdf.format(Date(item.timestamp))

        if (item.isDisease) {
            ivIcon.setImageResource(R.drawable.ic_camera)
        }

        itemView.setOnClickListener {
            val intent = Intent(this, AiResponseActivity::class.java)
            intent.putExtra("QUERY_TEXT", item.query)
            intent.putExtra("IS_DISEASE_ANALYSIS", item.isDisease)
            if (item.response.isNotEmpty()) {
                intent.putExtra("PRECALCULATED_ADVICE", item.response)
            }
            startActivity(intent)
        }

        btnDelete.setOnClickListener {
            deleteSingleHistoryItem(item.id)
        }

        container.addView(itemView)
    }

    private fun deleteSingleHistoryItem(historyId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.backendApi.deleteHistoryItem(historyId)
                if (response.isSuccessful) {
                    loadQueryHistory()
                } else {
                    Toast.makeText(this@QueryHistoryActivity, "Failed to delete item", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@QueryHistoryActivity, "Connection Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
