package com.rodrip.precioslocales.comparador.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utilidad para comprimir y descargar imágenes.
 */
object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val MAX_DIMENSION = 400
    private const val WEBP_QUALITY = 60
    
    private val client by lazy { OkHttpClient() }

    fun compress(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            original ?: return null

            val scaled = scaleBitmap(original)
            val outFile = File(context.cacheDir, "product_${System.currentTimeMillis()}.webp")
            FileOutputStream(outFile).use { out ->
                @Suppress("DEPRECATION")
                scaled.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, out)
            }

            if (scaled != original) scaled.recycle()
            original.recycle()
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Error comprimiendo: ${e.message}")
            null
        }
    }

    suspend fun downloadAndSave(context: Context, url: String): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Intentando descargar: $url")
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Descarga fallida: ${response.code}")
                return@withContext null
            }
            
            val body = response.body ?: return@withContext null
            val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
            val file = File(imagesDir, "prod_${UUID.randomUUID()}.webp")
            
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "Imagen guardada en: ${file.absolutePath}")
            file.absolutePath // Retornamos la ruta absoluta, es más segura
        } catch (e: Exception) {
            Log.e(TAG, "Error en downloadAndSave: ${e.message}")
            null
        }
    }

    private fun scaleBitmap(original: Bitmap): Bitmap {
        val w = original.width
        val h = original.height
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return original
        val ratio = w.toFloat() / h.toFloat()
        val (newW, newH) = if (w > h) MAX_DIMENSION to (MAX_DIMENSION / ratio).toInt() else (MAX_DIMENSION * ratio).toInt() to MAX_DIMENSION
        return Bitmap.createScaledBitmap(original, newW, newH, true)
    }
}
