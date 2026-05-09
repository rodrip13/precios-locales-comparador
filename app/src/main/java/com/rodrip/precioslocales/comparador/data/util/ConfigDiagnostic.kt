package com.rodrip.precioslocales.comparador.data.util

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rodrip.precioslocales.comparador.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Verifica que todos los servicios externos estén correctamente configurados.
 * Se ejecuta bajo demanda desde la pantalla de Ajustes.
 */
object ConfigDiagnostic {

    data class CheckResult(
        val name: String,
        val ok: Boolean,
        val detail: String
    )

    data class DiagnosticReport(val checks: List<CheckResult>) {
        val allOk: Boolean get() = checks.all { it.ok }
    }

    suspend fun run(): DiagnosticReport {
        val results = mutableListOf<CheckResult>()

        // ── 1. Cloudinary credentials ─────────────────────────────────────────
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
        val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET
        results += if (cloudName == "YOUR_CLOUD_NAME" || preset == "YOUR_UPLOAD_PRESET") {
            CheckResult(
                "Cloudinary credenciales",
                false,
                "Faltan valores en local.properties:\nCLOUDINARY_CLOUD_NAME y CLOUDINARY_UPLOAD_PRESET"
            )
        } else {
            CheckResult(
                "Cloudinary credenciales",
                true,
                "Cloud: $cloudName · Preset: $preset"
            )
        }

        // ── 2. Firebase Auth anónimo ──────────────────────────────────────────
        results += try {
            val auth = Firebase.auth
            val user = auth.currentUser ?: auth.signInAnonymously().await().user
            if (user != null) {
                CheckResult("Firebase Auth", true, "UID: ${user.uid.take(12)}…")
            } else {
                CheckResult("Firebase Auth", false, "No se pudo obtener usuario anónimo")
            }
        } catch (e: Exception) {
            CheckResult(
                "Firebase Auth",
                false,
                "Error: ${e.message?.take(80) ?: "desconocido"}\n¿google-services.json en app/?"
            )
        }

        // ── 3. Firestore conexión ─────────────────────────────────────────────
        results += try {
            Firebase.firestore
                .collection("products")
                .limit(1)
                .get()
                .await()
            CheckResult("Firestore conexión", true, "Lectura de prueba exitosa")
        } catch (e: Exception) {
            val hint = when {
                e.message?.contains("PERMISSION_DENIED") == true ->
                    "Reglas de seguridad bloqueando. ¿Copiaste firestore.rules en la consola?"
                e.message?.contains("UNAVAILABLE") == true ->
                    "Sin conexión a Internet"
                else -> e.message?.take(80) ?: "Error desconocido"
            }
            CheckResult("Firestore conexión", false, hint)
        }

        // ── 4. Cloudinary endpoint alcanzable ─────────────────────────────────
        results += if (cloudName != "YOUR_CLOUD_NAME") {
            try {
                withContext(Dispatchers.IO) {
                    val url = java.net.URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "HEAD"
                    conn.connectTimeout = 5000
                    conn.connect()
                    val code = conn.responseCode
                    conn.disconnect()
                    if (code in 200..499) {
                        CheckResult("Cloudinary endpoint", true, "Endpoint alcanzable (HTTP $code)")
                    } else {
                        CheckResult("Cloudinary endpoint", false, "Respuesta inesperada: HTTP $code")
                    }
                }
            } catch (e: Exception) {
                CheckResult("Cloudinary endpoint", false, "Sin conexión: ${e.message?.take(60)}")
            }
        } else {
            CheckResult("Cloudinary endpoint", false, "Credenciales no configuradas — omitido")
        }

        return DiagnosticReport(results)
    }
}



