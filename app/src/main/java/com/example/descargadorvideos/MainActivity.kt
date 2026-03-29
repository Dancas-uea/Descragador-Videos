package com.example.descargadorvideos

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.descargadorvideos.ui.theme.DescargadorVideosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.OutputStream
import java.util.concurrent.TimeUnit

// ── CAMBIA ESTA URL por la de tu instancia Railway ────────────────────────────
const val COBALT_API = "https://cobalt-api-production-3a13.up.railway.app"

class MainActivity : ComponentActivity() {

    private val descargando = mutableStateOf(false)
    private val progreso = mutableStateOf(0f)
    private val mensajeEstado = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DescargadorVideosTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    PantallaPrincipal(
                        descargando = descargando.value,
                        progreso = progreso.value,
                        mensajeEstado = mensajeEstado.value,
                        onDescargar = { url -> iniciarDescarga(url) }
                    )
                }
            }
        }
    }

    private fun iniciarDescarga(url: String) {
        if (descargando.value) return
        descargando.value = true
        progreso.value = 0f
        mensajeEstado.value = "Consultando Cobalt..."

        lifecycleScope.launch {
            val resultado = descargarConCobalt(url, this@MainActivity) { p, msg ->
                progreso.value = p
                mensajeEstado.value = msg
            }
            descargando.value = false
            mensajeEstado.value = resultado.second
            Toast.makeText(this@MainActivity, resultado.second, Toast.LENGTH_LONG).show()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Llamar a Cobalt API → obtener URL directa del video
// 2. Descargar esa URL → guardar en /Downloads
// ─────────────────────────────────────────────────────────────────────────────

suspend fun descargarConCobalt(
    videoUrl: String,
    context: Context,
    onProgreso: (Float, String) -> Unit
): Pair<Boolean, String> = withContext(Dispatchers.IO) {

    val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // ── PASO 1: POST a Cobalt para obtener el link directo ────────────────────
    val jsonBody = JSONObject().apply {
        put("url", videoUrl.trim())
        put("videoQuality", "1080")
        put("downloadMode", "auto")        // auto = video+audio combinado
        put("filenameStyle", "basic")
        put("youtubeVideoCodec", "h264")   // mp4 compatible con Android
        put("youtubeVideoContainer", "mp4")
    }.toString()

    val cobaltRequest = Request.Builder()
        .url("$COBALT_API/")
        .post(jsonBody.toRequestBody("application/json".toMediaType()))
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .build()

    val directUrl: String
    val filename: String

    try {
        val cobaltResponse = cliente.newCall(cobaltRequest).execute()
        val responseBody = cobaltResponse.body?.string()
            ?: return@withContext Pair(false, "❌ Cobalt no respondió")

        val json = JSONObject(responseBody)
        val status = json.optString("status", "error")

        when (status) {
            "tunnel", "redirect" -> {
                directUrl = json.getString("url")
                filename = json.optString("filename", "asmoroot_${System.currentTimeMillis()}.mp4")
            }
            "error" -> {
                val errorCode = json.optJSONObject("error")?.optString("code") ?: "desconocido"
                return@withContext Pair(false, "❌ Cobalt error: $errorCode")
            }
            "picker" -> {
                // Para Instagram/TikTok con múltiples medios, tomar el primero
                val picker = json.optJSONArray("picker")
                if (picker != null && picker.length() > 0) {
                    directUrl = picker.getJSONObject(0).getString("url")
                    filename = "asmoroot_${System.currentTimeMillis()}.mp4"
                } else {
                    return@withContext Pair(false, "❌ No se encontró video en la respuesta")
                }
            }
            else -> {
                return@withContext Pair(false, "❌ Respuesta inesperada de Cobalt: $status")
            }
        }
    } catch (e: Exception) {
        return@withContext Pair(false, "❌ Error al contactar Cobalt: ${e.localizedMessage}")
    }

    withContext(Dispatchers.Main) {
        onProgreso(0f, "Descargando video...")
    }

    // ── PASO 2: Descargar el video desde la URL que devolvió Cobalt ───────────
    val videoRequest = Request.Builder()
        .url(directUrl)
        .header("User-Agent", "AsmoRoot-Android/3.6")
        .build()

    try {
        val videoResponse = cliente.newCall(videoRequest).execute()

        if (!videoResponse.isSuccessful) {
            return@withContext Pair(false, "❌ Error al descargar: HTTP ${videoResponse.code}")
        }

        val body = videoResponse.body
            ?: return@withContext Pair(false, "❌ Respuesta vacía del CDN")

        val tamanoTotal = videoResponse.header("Content-Length")?.toLongOrNull() ?: -1L

        // Guardar con MediaStore (Android 10+) o directo (Android 9-)
        val outputStream: OutputStream
        val uriMediaStore = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valores = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores
            ) ?: return@withContext Pair(false, "❌ No se pudo crear archivo en Descargas")

            outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Pair(false, "❌ No se pudo abrir archivo para escritura")

            Pair(uri, valores)
        } else {
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            dir.mkdirs()
            outputStream = java.io.File(dir, filename).outputStream()
            null
        }

        // Stream con progreso
        val buffer = ByteArray(65536)
        var bytesLeidos = 0L
        var chunk: Int
        val input = body.byteStream()

        try {
            while (input.read(buffer).also { chunk = it } != -1) {
                outputStream.write(buffer, 0, chunk)
                bytesLeidos += chunk

                if (tamanoTotal > 0) {
                    val pct = bytesLeidos.toFloat() / tamanoTotal.toFloat()
                    val mb = bytesLeidos / (1024 * 1024)
                    val mbT = tamanoTotal / (1024 * 1024)
                    withContext(Dispatchers.Main) { onProgreso(pct, "$mb MB / $mbT MB") }
                } else {
                    val mb = bytesLeidos / (1024 * 1024)
                    withContext(Dispatchers.Main) { onProgreso(-1f, "$mb MB descargados...") }
                }
            }
        } finally {
            outputStream.close()
            input.close()
        }

        // Publicar en MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uriMediaStore != null) {
            val (uri, valores) = uriMediaStore
            valores.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, valores, null, null)
        }

        return@withContext Pair(true, "✅ Guardado: $filename")

    } catch (e: Exception) {
        return@withContext Pair(false, "❌ Error descargando: ${e.localizedMessage}")
    }
}

// ── UI ────────────────────────────────────────────────────────────────────────

@Composable
fun PantallaPrincipal(
    descargando: Boolean,
    progreso: Float,
    mensajeEstado: String,
    onDescargar: (String) -> Unit
) {
    val contexto = LocalContext.current
    var urlVideo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ASMOROOT v3.6", color = Color(0xFF00FFCC), style = MaterialTheme.typography.headlineMedium)
        Text("Video Downloader", color = Color(0xFF00FFCC).copy(alpha = 0.5f), style = MaterialTheme.typography.labelLarge)

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = urlVideo,
            onValueChange = { urlVideo = it },
            label = { Text("TikTok / YouTube / Instagram", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !descargando,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FFCC),
                unfocusedBorderColor = Color.DarkGray,
                disabledTextColor = Color.Gray,
                disabledBorderColor = Color.DarkGray,
                cursorColor = Color(0xFF00FFCC)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (urlVideo.isNotEmpty()) onDescargar(urlVideo)
                else Toast.makeText(contexto, "⚠️ Pega un link primero", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !descargando,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00FFCC),
                disabledContainerColor = Color(0xFF00FFCC).copy(alpha = 0.35f)
            )
        ) {
            if (descargando) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("DESCARGANDO...", color = Color.Black)
            } else {
                Text("DESCARGAR AHORA", color = Color.Black)
            }
        }

        if (descargando) {
            Spacer(modifier = Modifier.height(20.dp))
            if (progreso >= 0f) {
                LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF00FFCC), trackColor = Color(0xFF1A1A1A))
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF00FFCC), trackColor = Color(0xFF1A1A1A))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(mensajeEstado, color = Color(0xFF00FFCC).copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        }

        if (!descargando && mensajeEstado.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                mensajeEstado,
                color = if (mensajeEstado.startsWith("✅")) Color(0xFF00FFCC) else Color(0xFFFF5555),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}