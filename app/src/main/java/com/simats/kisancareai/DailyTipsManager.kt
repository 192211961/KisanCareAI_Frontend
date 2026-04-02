package com.simats.kisancareai

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

object DailyTipsManager {
    private const val TOTAL_TIPS = 10

    fun getTipIndexForDate(date: Date): Int {
        // Use logic that ensures the same index for the same calendar day regardless of time
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val days = TimeUnit.MILLISECONDS.toDays(calendar.timeInMillis)
        // Offset to avoid 0 if we want 1-indexed tips
        return (days % TOTAL_TIPS).toInt() + 1
    }

    fun getTipTitleResId(index: Int): Int {
        return when (index) {
            1 -> R.string.tip_1_title
            2 -> R.string.tip_2_title
            3 -> R.string.tip_3_title
            4 -> R.string.tip_4_title
            5 -> R.string.tip_5_title
            6 -> R.string.tip_6_title
            7 -> R.string.tip_7_title
            8 -> R.string.tip_8_title
            9 -> R.string.tip_9_title
            10 -> R.string.tip_10_title
            else -> R.string.tip_1_title
        }
    }

    fun getTipDescResId(index: Int): Int {
        return when (index) {
            1 -> R.string.tip_1_desc
            2 -> R.string.tip_2_desc
            3 -> R.string.tip_3_desc
            4 -> R.string.tip_4_desc
            5 -> R.string.tip_5_desc
            6 -> R.string.tip_6_desc
            7 -> R.string.tip_7_desc
            8 -> R.string.tip_8_desc
            9 -> R.string.tip_9_desc
            10 -> R.string.tip_10_desc
            else -> R.string.tip_1_desc
        }
    }
}
