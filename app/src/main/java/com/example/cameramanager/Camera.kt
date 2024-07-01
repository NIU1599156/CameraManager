package com.example.cameramanager

import android.os.Parcel
import android.os.Parcelable

// Clase Camera que implementa una interfaz Parcelable para permitir poder pasarse como intent
// y así poder modificar realizar la edición de cámaras de forma cómoda
data class Camera(
    val id: Int,
    val name: String,
    val ip: String
) : Parcelable {

    // Constructor para crear un Parcel a través de una cámara
    constructor(parcel: Parcel) : this(
        parcel.readInt(), // Leemos el ID de la cámara
        parcel.readString() ?: "", // Leemos el nombre asegurando que no sea null
        parcel.readString() ?: "" // Leemos la IP desde asegurando que no sea null
    )

    // Método para escribir los datos de Camera en un Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id) // Escribir el ID en el Parcel
        parcel.writeString(name) // Escribir el nombre en el Parcel
        parcel.writeString(ip) // Escribir la IP en el Parcel
    }

    // Método que para describir el parcelable (no la usamos pero debemos implementarla)
    override fun describeContents(): Int {
        return 0
    }

    // Implementación adicional para la creación de un objeto Parcel
    companion object CREATOR : Parcelable.Creator<Camera> {
        // Creamos una instancia de Camera desde un Parcel
        override fun createFromParcel(parcel: Parcel): Camera {
            return Camera(parcel)
        }

        // Creación de un array de Camera
        override fun newArray(size: Int): Array<Camera?> {
            return arrayOfNulls(size) // Crea un array de cámaras con tamaño especificado
        }
    }
}
