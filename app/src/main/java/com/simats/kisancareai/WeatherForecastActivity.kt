package com.simats.kisancareai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.appbar.MaterialToolbar
import java.util.Locale
import java.util.Calendar
import java.text.SimpleDateFormat
import android.location.Geocoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherForecastActivity : BaseActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvLocationName: TextView
    private lateinit var tvCurrentTemp: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var tvFeelsLike: TextView
    private lateinit var ivMainWeatherIcon: ImageView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWindSpeed: TextView
    private lateinit var tvRainfall: TextView
    private lateinit var forecastContainer: LinearLayout
    private lateinit var suggestionsContainer: LinearLayout

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            checkLocationAndFetchData()
        } else {
            tvLocationName.text = "Permission denied. Tap to retry."
            Toast.makeText(this, "Location permission is required for weather data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_weather_forecast)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize UI References
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvLocationName = findViewById(R.id.tv_location_name)
        tvCurrentTemp = findViewById(R.id.tv_current_temp)
        tvCurrentStatus = findViewById(R.id.tv_current_status)
        tvFeelsLike = findViewById(R.id.tv_feels_like)
        ivMainWeatherIcon = findViewById(R.id.iv_main_weather_icon)
        tvHumidity = findViewById(R.id.tv_humidity)
        tvWindSpeed = findViewById(R.id.tv_wind_speed)
        tvRainfall = findViewById(R.id.tv_rainfall)
        forecastContainer = findViewById(R.id.forecast_container)
        suggestionsContainer = findViewById(R.id.suggestions_container)

        findViewById<View>(R.id.btn_detect_location).setOnClickListener {
            checkLocationAndFetchData()
        }

        checkLocationAndFetchData()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkLocationAndFetchData() {
        if (!isLocationEnabled()) {
            tvLocationName.text = "Please turn on your location"
            tvLocationName.setTextColor(android.graphics.Color.RED)
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
            // Optionally open settings
            // startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        tvLocationName.setTextColor(android.graphics.Color.parseColor("#64748B"))

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        tvLocationName.text = "Detecting exact location..."
        
        val priority = Priority.PRIORITY_HIGH_ACCURACY
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateLocationTextAndFetchWeather(location.latitude, location.longitude)
                } else {
                    // If fresh location fails, try last location as a quick fallback
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            updateLocationTextAndFetchWeather(lastLoc.latitude, lastLoc.longitude)
                        } else {
                            tvLocationName.text = "Location not found. Using default."
                            fetchWeatherData(13.0827, 80.2707) // Chennai
                        }
                    }
                }
            }.addOnFailureListener {
                tvLocationName.text = "Location error. Using default."
                fetchWeatherData(13.0827, 80.2707)
            }
    }

    private fun updateLocationTextAndFetchWeather(lat: Double, lon: Double) {
        val placeName = getPlaceName(lat, lon)
        tvLocationName.text = "Location: $placeName"
        fetchWeatherData(lat, lon)
    }

    private fun getPlaceName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Area: Neighborhood or Village or City
                val area = address.subLocality ?: address.locality ?: "Unknown Area"
                // District: Usually subAdminArea in Android Geocoder
                val district = address.subAdminArea ?: address.adminArea ?: "Unknown District"
                
                "$area, $district"
            } else {
                "(${String.format("%.2f", lat)}, ${String.format("%.2f", lon)})"
            }
        } catch (e: Exception) {
            "(${String.format("%.2f", lat)}, ${String.format("%.2f", lon)})"
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = WeatherApiService.create()
                val response = service.getForecast(lat, lon)
                withContext(Dispatchers.Main) {
                    updateUIWithRealData(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WeatherForecastActivity, "Failed to fetch weather data", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun updateUIWithRealData(response: WeatherResponse) {
        val current = response.current
        val daily = response.daily

        // Update Current UI
        tvCurrentTemp.text = "${current.temperature_2m.toInt()}°C"
        tvFeelsLike.text = "Feels like ${current.temperature_2m.toInt() + 2}°C"
        
        // Map Open-Meteo weather codes to our UI
        val weatherInfo = mapWeatherCode(daily.weather_code[0])
        tvCurrentStatus.text = weatherInfo.name
        ivMainWeatherIcon.setImageResource(weatherInfo.iconRes)
        
        tvHumidity.text = "${current.relative_humidity_2m}%"
        tvWindSpeed.text = "${current.wind_speed_10m} km/h"
        tvRainfall.text = "${current.precipitation} mm"

        // Update Forecast
        updateForecastUI(daily)
        
        // Update Farming Suggestions
        updateFarmingSuggestions(daily)
    }

    private fun updateForecastUI(daily: DailyForecast) {
        forecastContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val today = Calendar.getInstance()
        val todayStr = inputFormat.format(today.time)
        
        today.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = inputFormat.format(today.time)

        for (i in 0 until daily.time.size) {
            val dateStr = daily.time[i]
            val code = daily.weather_code[i]
            val info = mapWeatherCode(code)
            val maxTemp = daily.temperature_2m_max[i].toInt()
            val rainChance = daily.precipitation_probability_max[i]
            
            val itemView = inflater.inflate(R.layout.item_forecast, forecastContainer, false)
            
            val tvDay = itemView.findViewById<TextView>(R.id.tv_forecast_day)
            tvDay.text = when (dateStr) {
                todayStr -> "Today"
                tomorrowStr -> "Tomorrow"
                else -> {
                    try {
                        val date = inputFormat.parse(dateStr)
                        if (date != null) outputFormat.format(date) else dateStr
                    } catch (e: Exception) {
                        dateStr
                    }
                }
            }
            
            itemView.findViewById<TextView>(R.id.tv_forecast_status).text = info.name
            itemView.findViewById<TextView>(R.id.tv_forecast_temp).text = "$maxTemp°C"
            itemView.findViewById<TextView>(R.id.tv_forecast_emoji).text = info.emoji
            
            val tvRainChance = itemView.findViewById<TextView>(R.id.tv_rain_chance)
            tvRainChance.text = "Rain: $rainChance%"
            tvRainChance.visibility = if (rainChance > 0) View.VISIBLE else View.INVISIBLE
            
            forecastContainer.addView(itemView)
        }
    }

    private fun updateFarmingSuggestions(daily: DailyForecast) {
        suggestionsContainer.removeAllViews()
        val suggestions = mutableListOf<String>()

        val maxTemp = daily.temperature_2m_max.maxOrNull() ?: 0.0
        val isRainy = daily.weather_code.any { it in 51..99 }

        if (isRainy) {
            suggestions.add("Rain detected in forecast. Check and clear drainage channels.")
            suggestions.add("Postpone fertilizer application to avoid nutrient washout.")
        } else {
            suggestions.add("Dry weather expected. Ideal for harvesting and sun-drying crops.")
        }

        if (maxTemp > 35) {
            suggestions.add("High temperatures (${maxTemp.toInt()}°C) may cause heat stress. Increase irrigation.")
        }

        if (isRainy) {
            suggestions.add("High humidity may favor fungal diseases. Monitor crops closely.")
        } else {
            suggestions.add("Clear weather is perfect for weeding and field preparation.")
        }

        // Add suggestions to UI
        suggestions.forEach { suggestion ->
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.HORIZONTAL
            layout.setPadding(0, 8, 0, 8)

            val bullet = TextView(this)
            bullet.text = "• "
            bullet.setTextColor(android.graphics.Color.parseColor("#166534"))
            bullet.textSize = 14f
            
            val text = TextView(this)
            text.text = suggestion
            text.setTextColor(android.graphics.Color.parseColor("#166534"))
            text.textSize = 12f
            text.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)

            layout.addView(bullet)
            layout.addView(text)
            suggestionsContainer.addView(layout)
        }
    }

    private fun mapWeatherCode(code: Int): WeatherInfo {
        return when (code) {
            0 -> WeatherInfo("Clear Sky", "☀️", R.drawable.ic_sunny)
            1, 2, 3 -> WeatherInfo("Partly Cloudy", "⛅", R.drawable.ic_partly_cloudy)
            45, 48 -> WeatherInfo("Foggy", "🌫️", R.drawable.ic_cloud)
            51, 53, 55, 61, 63, 65 -> WeatherInfo("Rainy", "🌧️", R.drawable.ic_rain)
            95, 96, 99 -> WeatherInfo("Thunderstorm", "⛈️", R.drawable.ic_rain)
            else -> WeatherInfo("Cloudy", "☁️", R.drawable.ic_cloud)
        }
    }

    data class WeatherInfo(val name: String, val emoji: String, val iconRes: Int)
}
