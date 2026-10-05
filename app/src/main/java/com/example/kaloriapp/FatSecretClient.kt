package com.example.kaloriapp.com.example.kaloriapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FatSecretClient {
    val api: FatSecretApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://kaloriapp.alwaysdata.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FatSecretApi::class.java)
    }
}