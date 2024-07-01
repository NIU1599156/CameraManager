package com.example.cameramanager

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Instanciamos retrofit para gestionar las llamadas a la API
    private lateinit var retrofit: Retrofit
    // Instanciamos interfaz para definir las llamadas a la API
    lateinit var apiInterface: ApiInterface

    // Configuramos la dirección de la raspberry para las llamadas a la API
    fun setBaseUrl(baseUrl: String) {
        // Configuramos Retrofit para las llamadas a la API
        retrofit = Retrofit.Builder()
            .baseUrl("http://$baseUrl:5000/") // Ponemos la url de la rpi con retrofit
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        // Creamos la implementación de la API que hemos definido para que se pueda usar en el código
        apiInterface = retrofit.create(ApiInterface::class.java)
    }
}
