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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.descargadorvideos.ui.theme.*
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
                PantallaConMenu(
                    descargando = descargando.value,
                    progreso = progreso.value,
                    mensajeEstado = mensajeEstado.value,
                    onDescargar = { url -> iniciarDescarga(url) }
                )
            }
        }
    }

    private fun iniciarDescarga(url: String) {
        if (descargando.value) return
        descargando.value = true
        progreso.value = 0f
        mensajeEstado.value = "Conectando..."

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConMenu(
    descargando: Boolean,
    progreso: Float,
    mensajeEstado: String,
    onDescargar: (String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Gray900,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Spacer(Modifier.height(48.dp))
                Text("V-Downloader", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge, color = Color.White)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = GlassBorder)
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                    label = { Text("Inicio", color = Color.White) },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = GlassBorder),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(label = { Text("Ajustes", color = Color.White) }, selected = false, onClick = { }, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("V-DOWNLOADER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Gray900, Gray800, Gray900)))
                    .padding(padding)
            ) {
                PantallaPrincipalContent(descargando, progreso, mensajeEstado, onDescargar)
            }
        }
    }
}

@Composable
fun PantallaPrincipalContent(
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
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Tarjeta Glassmorphism
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Premium Downloader", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text("V2.03 - Profesional", color = Color.Gray, style = MaterialTheme.typography.labelLarge)

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = urlVideo,
                    onValueChange = { urlVideo = it },
                    placeholder = { Text("Pega el enlace aquí", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !descargando,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = AccentBlue
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (urlVideo.isNotEmpty()) onDescargar(urlVideo)
                        else Toast.makeText(contexto, "⚠️ Inserte un enlace", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !descargando,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    if (descargando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Descargar", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }

        if (descargando || mensajeEstado.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(mensajeEstado, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    if (descargando) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { if (progreso >= 0) progreso else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(10.dp)),
                            color = AccentBlue,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

suspend fun descargarConCobalt(
    videoUrl: String,
    context: Context,
    onProgreso: (Float, String) -> Unit
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val cliente = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
    val jsonBody = JSONObject().apply {
        put("url", videoUrl.trim())
        put("videoQuality", "1080")
        put("downloadMode", "auto")
        put("youtubeVideoCodec", "h264")
    }.toString()

    val cobaltRequest = Request.Builder()
        .url("$COBALT_API/")
        .post(jsonBody.toRequestBody("application/json".toMediaType()))
        .header("Accept", "application/json")
        .build()

    try {
        val cobaltResponse = cliente.newCall(cobaltRequest).execute()
        val responseBody = cobaltResponse.body?.string() ?: ""
        val json = JSONObject(responseBody)
        val directUrl = json.optString("url")
        val filename = json.optString("filename", "video_${System.currentTimeMillis()}.mp4")

        if (directUrl.isEmpty()) return@withContext Pair(false, "❌ Enlace no válido")

        withContext(Dispatchers.Main) { onProgreso(0f, "Descargando...") }

        val videoRequest = Request.Builder().url(directUrl).build()
        val videoResponse = cliente.newCall(videoRequest).execute()
        val body = videoResponse.body ?: return@withContext Pair(false, "❌ Error descarga")
        val tamanoTotal = videoResponse.header("Content-Length")?.toLongOrNull() ?: -1L

        val outputStream: OutputStream
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valores = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)!!
            outputStream = context.contentResolver.openOutputStream(uri)!!
            
            val buffer = ByteArray(65536)
            var bytesLeidos = 0L
            var chunk: Int
            val input = body.byteStream()
            while (input.read(buffer).also { chunk = it } != -1) {
                outputStream.write(buffer, 0, chunk)
                bytesLeidos += chunk
                if (tamanoTotal > 0) {
                    val pct = bytesLeidos.toFloat() / tamanoTotal.toFloat()
                    withContext(Dispatchers.Main) { onProgreso(pct, "Descargando: ${(pct * 100).toInt()}%") }
                }
            }
            outputStream.close()
            valores.clear()
            valores.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, valores, null, null)
        }
        return@withContext Pair(true, "✅ Guardado en Descargas")
    } catch (e: Exception) {
        return@withContext Pair(false, "❌ Error")
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
fun PreviewPantallaFormal() {
    DescargadorVideosTheme {
        PantallaConMenu(
            descargando = true, 
            progreso = 0.65f, 
            mensajeEstado = "Descargando: 65%",
            onDescargar = {}
        )
    }
}
