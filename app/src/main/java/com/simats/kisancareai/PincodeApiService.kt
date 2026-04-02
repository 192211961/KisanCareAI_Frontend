package com.simats.kisancareai

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

data class PincodeResponse(
    val Message: String,
    val Status: String,
    val PostOffice: List<PostOfficeDetails>?
)

data class PostOfficeDetails(
    val Name: String,
    val Description: String?,
    val BranchType: String,
    val DeliveryStatus: String,
    val Circle: String,
    val District: String,
    val Division: String,
    val Region: String,
    val State: String,
    val Country: String,
    val Pincode: String
)

interface PincodeApiService {
    @GET("pincode/{pincode}")
    fun getPincodeDetails(@Path("pincode") pincode: String): Call<List<PincodeResponse>>
}
