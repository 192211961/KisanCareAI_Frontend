package com.simats.kisancareai
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val PINCODE_BASE_URL = "https://api.postalpincode.in/"
    private const val WEATHER_BASE_URL = "https://api.open-meteo.com/"
    
    // Fixed: Removed the extra slash from the URL
    // For Android Emulator, use "10.0.2.2". For physical devices, use your computer's IP.
    // Replace the IP below with your computer's actual IP address if using a physical device.
    const val BACKEND_BASE_URL = "http://180.235.121.245:8022/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val pincodeApi: PincodeApiService by lazy {
        createRetrofit(PINCODE_BASE_URL).create(PincodeApiService::class.java)
    }

    val weatherApi: WeatherApiService by lazy {
        createRetrofit(WEATHER_BASE_URL).create(WeatherApiService::class.java)
    }

    val backendApi: BackendApiService by lazy {
        createRetrofit(BACKEND_BASE_URL).create(BackendApiService::class.java)
    }
}