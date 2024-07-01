package com.example.cameramanager

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

// Hacemos una interfaz para definir las llamadas a la API de la rpi
interface ApiInterface {
    // Obtenemos la lista de cámaras
    @GET("/cameras")
    fun getCameras(): Call<List<Camera>>

    // Eliminamos todas las cámaras
    @DELETE("/cameras")
    fun deleteCameras(): Call<Void>

    // Eliminamos una cámara en concreto
    @DELETE("/cameras/{id}")
    fun deleteCamera(@Path("id") id: Int): Call<Void>

    // Actualizar una cámara en concreto
    @PUT("/cameras/{id}")
    fun updateCamera(@Path("id") id: Int, @Body camera: Camera): Call<Camera>

    // Añadimos una nueva cámara
    @POST("/cameras")
    fun addCamera(@Body camera: Camera): Call<Camera>

    // Iniciamos una cámara concreta
    @POST("/cameras/{id}/start")
    fun startCamera(@Path("id") id: Int): Call<Void>

    // Detenemos una cámara concreta
    @POST("/cameras/{id}/stop")
    fun stopCamera(@Path("id") id: Int): Call<Void>

    // Activamos la alarma en la rpi
    @POST("/alarm")
    fun activateAlarm(): Call<Void>
}
