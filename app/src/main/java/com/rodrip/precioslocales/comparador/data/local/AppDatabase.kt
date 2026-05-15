package com.rodrip.precioslocales.comparador.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rodrip.precioslocales.comparador.data.local.dao.LocalComercialDao
import com.rodrip.precioslocales.comparador.data.local.dao.ProductoDao
import com.rodrip.precioslocales.comparador.data.local.dao.RegistroPrecioDao
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.entity.RegistroPrecio

@Database(
    entities = [LocalComercial::class, Producto::class, RegistroPrecio::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localComercialDao(): LocalComercialDao
    abstract fun productoDao(): ProductoDao
    abstract fun registroPrecioDao(): RegistroPrecioDao

    companion object {
        const val DATABASE_NAME = "compara_precios_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN remotePhotoUrl TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stores ADD COLUMN department TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stores ADD COLUMN localidad TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stores ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE stores ADD COLUMN uploadedBy TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stores ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}