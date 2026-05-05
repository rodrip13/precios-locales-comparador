package com.rodrip.precioslocales.comparador

import android.app.Application
import androidx.room.Room
import com.rodrip.precioslocales.comparador.data.local.AppDatabase
import com.rodrip.precioslocales.comparador.data.repository.MainRepository

class MainApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository by lazy {
        MainRepository(
            database.localComercialDao(),
            database.productoDao(),
            database.registroPrecioDao()
        )
    }
}