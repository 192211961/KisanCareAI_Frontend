package com.simats.kisancareai

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.DELETE

// Data models for Backend API
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(
    val message: String, 
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("email") val email: String? = null,
    val redirect: String? = null
)

data class RegisterRequest(
    @SerializedName("full_name") val full_name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("confirm_password") val confirm_password: String
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class ResendOtpRequest(val email: String)

data class ForgotPasswordRequest(val email: String)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("new_password") val new_password: String,
    @SerializedName("confirm_password") val confirm_password: String
)

data class UserProfile(
    @SerializedName("full_name") val full_name: String,
    val email: String,
    val mobile: String,
    val pincode: String,
    val state: String,
    val district: String
)

data class GenericResponse(val message: String, val error: String? = null)

data class HistoryItem(
    val id: Int,
    val query: String,
    val response: String,
    @SerializedName("is_disease") val isDisease: Boolean,
    val timestamp: Long
)

data class SaveHistoryRequest(
    val email: String,
    val query: String,
    val response: String?,
    @SerializedName("is_disease") val isDisease: Boolean
)

interface BackendApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<GenericResponse>

    @POST("verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<GenericResponse>

    @POST("resend-otp")
    suspend fun resendOtp(@Body request: ResendOtpRequest): Response<GenericResponse>

    @POST("forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericResponse>

    @POST("reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericResponse>

    @GET("api/user/profile")
    suspend fun getProfile(@retrofit2.http.Query("email") email: String): Response<UserProfile>

    @PUT("api/user/profile")
    suspend fun updateProfile(@Body profile: UserProfile): Response<GenericResponse>

    @GET("api/user/history")
    suspend fun getHistory(@retrofit2.http.Query("email") email: String): Response<List<HistoryItem>>

    @POST("api/user/history")
    suspend fun saveHistory(@Body request: SaveHistoryRequest): Response<GenericResponse>

    @DELETE("api/user/history/{id}")
    suspend fun deleteHistoryItem(@retrofit2.http.Path("id") id: Int): Response<GenericResponse>

    @DELETE("api/user/history/clear")
    suspend fun clearHistory(@retrofit2.http.Query("email") email: String): Response<GenericResponse>
}
