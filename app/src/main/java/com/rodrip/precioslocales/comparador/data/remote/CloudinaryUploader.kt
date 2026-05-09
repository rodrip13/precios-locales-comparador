package com.rodrip.precioslocales.comparador.data.remote

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.rodrip.precioslocales.comparador.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Sube imágenes comprimidas a Cloudinary de forma no firmada (unsigned).
 * 
 * IMPORTANTE: La carpeta y las transformaciones deben configurarse 
 * directamente en el Upload Preset en la consola de Cloudinary para 
 * evitar errores de "api_key" requerida.
 */
object CloudinaryUploader {

    suspend fun upload(file: File, folder: String = "products"): String? = suspendCancellableCoroutine { continuation ->
        try {
            MediaManager.get().upload(file.absolutePath)
                .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                .option("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (continuation.isActive) continuation.resume(url)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        println("Cloudinary error: ${error.description}")
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            e.printStackTrace()
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
