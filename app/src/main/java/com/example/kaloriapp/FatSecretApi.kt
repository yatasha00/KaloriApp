package com.example.kaloriapp.com.example.kaloriapp

import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface FatSecretApi {
    @GET("fatsecret_api.php")
    suspend fun searchFood(@Query("yemek") yemek: String): JsonElement
}