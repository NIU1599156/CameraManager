package com.example.cameramanager

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private lateinit var retrofit: Retrofit
    lateinit var apiInterface: ApiInterface

    fun setBaseUrl(baseUrl: String) {
        retrofit = Retrofit.Builder()
            .baseUrl("http://$baseUrl:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiInterface = retrofit.create(ApiInterface::class.java)
    }
}