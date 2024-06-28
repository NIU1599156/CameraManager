package com.example.cameramanager

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiInterface {
    @GET("/cameras")
    fun getCameras(): Call<List<Camera>>

    @POST("/cameras")
    fun addCamera(@Body camera: Camera): Call<Camera>

    @POST("/cameras/{id}/start")
    fun startCamera(@Path("id") id: Int): Call<Void>

    @POST("/cameras/{id}/stop")
    fun stopCamera(@Path("id") id: Int): Call<Void>
}