package com.simats.kisancareai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUserName(view)

        // Set dynamic date for Today's Tip
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val today = Calendar.getInstance().time
        view.findViewById<TextView>(R.id.tv_today_date).text = dateFormat.format(today)

        // Set dynamic tip of the day
        val tipIndex = DailyTipsManager.getTipIndexForDate(today)
        view.findViewById<TextView>(R.id.tip_text).setText(DailyTipsManager.getTipDescResId(tipIndex))

        // View all tips navigation
        view.findViewById<TextView>(R.id.btn_view_all_tips).setOnClickListener {
            startActivity(Intent(requireContext(), PersonalizedTipsActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_notifications).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationSettingsActivity::class.java))
        }

        // Navigation for cards
        view.findViewById<View>(R.id.card_disease).setOnClickListener {
            startActivity(Intent(requireContext(), DiseaseDetectionActivity::class.java))
        }

        view.findViewById<View>(R.id.card_weather).setOnClickListener {
            startActivity(Intent(requireContext(), WeatherForecastActivity::class.java))
        }

        view.findViewById<View>(R.id.card_ask_question).setOnClickListener {
            startActivity(Intent(requireContext(), AskQuestionActivity::class.java))
        }

        view.findViewById<View>(R.id.card_voice_query).setOnClickListener {
            startActivity(Intent(requireContext(), VoiceQueryActivity::class.java))
        }

        view.findViewById<View>(R.id.card_feedback).setOnClickListener {
            startActivity(Intent(requireContext(), FeedbackActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_saved_advice).setOnClickListener {
            startActivity(Intent(requireContext(), SavedAdviceActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_query_history).setOnClickListener {
            startActivity(Intent(requireContext(), QueryHistoryActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_location_turn_on).setOnClickListener {
            handleLocationAction()
        }

        checkLocationStatus(view)
    }

    private fun updateUserName(view: View) {
        val sharedPrefs = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("full_name", "Farmer")
        view.findViewById<TextView>(R.id.tv_user_name).text = userName
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocationStatus(view: View) {
        val banner = view.findViewById<View>(R.id.card_location_suggestion)
        if (!isLocationEnabled() || !hasLocationPermission()) {
            banner.visibility = View.VISIBLE
        } else {
            banner.visibility = View.GONE
        }
    }

    private fun handleLocationAction() {
        if (!isLocationEnabled()) {
            try {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not open location settings", Toast.LENGTH_SHORT).show()
            }
        } else if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            updateUserName(it)
            checkLocationStatus(it)
        }
    }
}
