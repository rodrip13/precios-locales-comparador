package com.rodrip.precioslocales.comparador

import android.app.Application
import androidx.room.Room
import com.cloudinary.android.MediaManager
import com.rodrip.precioslocales.comparador.data.local.AppDatabase
import com.rodrip.precioslocales.comparador.data.local.CityGroupPreferences
import com.rodrip.precioslocales.comparador.data.local.SyncPreferences
import com.rodrip.precioslocales.comparador.data.local.ThemePreferences
import com.rodrip.precioslocales.comparador.data.remote.AnonymousAuthManager
import com.rodrip.precioslocales.comparador.data.remote.RemoteProductDataSource
import com.rodrip.precioslocales.comparador.data.remote.RemoteStoreDataSource
import com.rodrip.precioslocales.comparador.data.repository.MainRepository
import com.rodrip.precioslocales.comparador.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainApplication : Application() {

    private val appScope = CoroutineScope(Dispatchers.IO)

    private val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }

    val remoteDataSource by lazy { RemoteProductDataSource(this) }
    val remoteStoreDataSource by lazy { RemoteStoreDataSource() }

    val cityGroupPreferences by lazy { CityGroupPreferences(this) }

    val repository by lazy {
        MainRepository(
            context = this,
            storeDao = database.localComercialDao(),
            productDao = database.productoDao(),
            priceDao = database.registroPrecioDao(),
            remoteDataSource = remoteDataSource,
            remoteStoreDataSource = remoteStoreDataSource,
            cityGroupPreferences = cityGroupPreferences
        )
    }

    val themePreferences by lazy { ThemePreferences(this) }

    val syncPreferences by lazy { SyncPreferences(this) }

    val syncManager by lazy {
        SyncManager(
            context = this,
            productoDao = database.productoDao(),
            remoteDataSource = remoteDataSource,
            syncPreferences = syncPreferences,
            storeDao = database.localComercialDao(),
            remoteStoreDataSource = remoteStoreDataSource,
            cityGroupPreferences = cityGroupPreferences
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializar Cloudinary
        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME
        )
        MediaManager.init(this, config)

        // Iniciar sesión anónima
        appScope.launch {
            try {
                val uid = AnonymousAuthManager.ensureSignedIn()
                syncPreferences.saveContributorId(uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
