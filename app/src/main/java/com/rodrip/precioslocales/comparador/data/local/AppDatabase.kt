package com.rodrip.precioslocales.comparador.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rodrip.precioslocales.comparador.data.local.dao.LocalComercialDao
import com.rodrip.precioslocales.comparador.data.local.dao.ProductoDao
import com.rodrip.precioslocales.comparador.data.local.dao.RegistroPrecioDao
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.entity.RegistroPrecio

@Database(
    entities = [LocalComercial::class, Producto::class, RegistroPrecio::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localComercialDao(): LocalComercialDao
    abstract fun productoDao(): ProductoDao
    abstract fun registroPrecioDao(): RegistroPrecioDao

    companion object {
        const val DATABASE_NAME = "compara_precios_db"
    }
}