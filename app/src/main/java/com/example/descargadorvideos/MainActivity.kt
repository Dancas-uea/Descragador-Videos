package com.example.descargadorvideos

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.tooling.preview.Preview
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

// ── Plataformas soportadas ────────────────────────────────────────────────────
data class Plataforma(val nombre: String, val color: Color, val emoji: String, val dominio: String)

val PLATAFORMAS = listOf(
    Plataforma("TikTok",    ColorTikTok,    "♪", "tiktok.com"),
    Plataforma("YouTube",   ColorYouTube,   "▶", "youtube.com"),
    Plataforma("Instagram", ColorInstagram, "◈", "instagram.com"),
    Plataforma("Facebook",  ColorFacebook,  "f", "facebook.com"),
    Plataforma("X / Twitter", ColorTwitterX, "𝕏", "twitter.com"),
)

class MainActivity : ComponentActivity() {

    private val descargando  = mutableStateOf(false)
    private val progreso     = mutableStateOf(0f)
    private val mensajeEstado = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DescargadorVideosTheme {
                AppShell (
                    descargando   = descargando.value,
                    progreso      = progreso.value,
                    mensajeEstado = mensajeEstado.value,
                    onDescargar   = { url -> iniciarDescarga(url)}
                )
            }
        }
    }

    private fun iniciarDescarga(url: String) {
        if (descargando.value) return
        descargando.value  = true
        progreso.value     = 0f
        mensajeEstado.value = "Conectando..."

        lifecycleScope.launch {
            val resultado = descargarConCobalt(url, this@MainActivity) { p, msg ->
                progreso.value      = p
                mensajeEstado.value = msg
            }
            descargando.value   = false
            mensajeEstado.value = resultado.second
            Toast.makeText(this@MainActivity, resultado.second, Toast.LENGTH_LONG).show()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHELL: TopBar + Drawer
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    descargando: Boolean,
    progreso: Float,
    mensajeEstado: String,
    onDescargar: (String) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    val ctx         = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(onClose = { scope.launch { drawerState.close() } }, ctx = ctx)
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "VDownloader",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 17.sp,
                            color      = TextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            // Hamburger icon
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp).height(2.dp)
                                            .clip(CircleShape)
                                            .background(TextPrimary)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SurfaceCard
                    )
                )
            },
            containerColor = OffWhite
        ) { padding ->
            HomeScreen(
                modifier      = Modifier.padding(padding),
                descargando   = descargando,
                progreso      = progreso,
                mensajeEstado = mensajeEstado,
                onDescargar   = onDescargar
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DRAWER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppDrawer(onClose: () -> Unit, ctx: Context) {
    ModalDrawerSheet(
        drawerContainerColor = DrawerBg,
        drawerShape          = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
        modifier             = Modifier.width(280.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DrawerSurface)
                .padding(24.dp)
                .padding(top = 28.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↓", fontSize = 26.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text("VDownloader", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("v2.03 Pro", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        }

        Divider(color = DrawerBorder, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))

        // Items del menú
        DrawerItem(emoji = "🏠", label = "Inicio",           onClick = onClose)
        DrawerItem(emoji = "📖", label = "Cómo descargar",   onClick = { onClose() })
        DrawerItem(emoji = "⚙️", label = "Configuraciones",  onClick = { onClose() })
        DrawerItem(emoji = "🎧", label = "Servicio al cliente", onClick = {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:soporte@vdownloader.app"))
            ctx.startActivity(intent)
            onClose()
        })

        Spacer(Modifier.weight(1f))
        Divider(color = DrawerBorder, thickness = 1.dp)

        // Redes sociales
        Text(
            "Síguenos",
            color    = Color.White.copy(alpha = 0.35f),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SocialChip("TikTok",   ColorTikTok,    "♪", "https://tiktok.com",    ctx)
            SocialChip("IG",       ColorInstagram, "◈", "https://instagram.com", ctx)
            SocialChip("X",        Color.White,    "𝕏", "https://x.com",         ctx)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun DrawerItem(emoji: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SocialChip(label: String, color: Color, icon: String, url: String, ctx: Context) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DrawerSurface)
            .border(1.dp, DrawerBorder, RoundedCornerShape(8.dp))
            .clickable {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 16.sp, color = color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    modifier: Modifier,
    descargando: Boolean,
    progreso: Float,
    mensajeEstado: String,
    onDescargar: (String) -> Unit
) {
    val contexto = LocalContext.current
    var urlVideo by remember { mutableStateOf("") }
    val scroll   = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Chips de plataformas ──────────────────────────────────────────────
        Text(
            "Plataformas soportadas",
            style    = MaterialTheme.typography.labelMedium,
            color    = TextSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PLATAFORMAS.forEach { p ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(p.emoji, fontSize = 16.sp)
                }
            }
        }

        // ── Card principal de descarga ────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Text(
                    "Pega el enlace del video",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value         = urlVideo,
                    onValueChange = { urlVideo = it },
                    placeholder   = { Text("https://...", color = TextHint) },
                    modifier      = Modifier.fillMaxWidth(),
                    enabled       = !descargando,
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Accent,
                        unfocusedBorderColor = BorderLight,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = Accent,
                        disabledBorderColor  = BorderLight,
                        disabledTextColor    = TextSecondary
                    )
                )

                Button(
                    onClick = {
                        if (urlVideo.isNotEmpty()) onDescargar(urlVideo)
                        else Toast.makeText(contexto, "⚠️ Pega un enlace primero", Toast.LENGTH_SHORT).show()
                    },
                    modifier  = Modifier.fillMaxWidth().height(50.dp),
                    enabled   = !descargando,
                    shape     = RoundedCornerShape(12.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor         = Accent,
                        disabledContainerColor = Accent.copy(alpha = 0.4f)
                    )
                ) {
                    if (descargando) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Descargando...", color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("⬇  Descargar", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }

        // ── Progreso / resultado ──────────────────────────────────────────────
        if (descargando || mensajeEstado.isNotEmpty()) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = if (mensajeEstado.startsWith("✅")) Color(0xFFF0FDF4)
                    else if (mensajeEstado.startsWith("❌")) Color(0xFFFFF1F2)
                    else SurfaceCard
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                border    = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (mensajeEstado.startsWith("✅")) Color(0xFFBBF7D0)
                    else if (mensajeEstado.startsWith("❌")) Color(0xFFFFCDD2)
                    else BorderLight
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        mensajeEstado,
                        color = when {
                            mensajeEstado.startsWith("✅") -> Success
                            mensajeEstado.startsWith("❌") -> Error
                            else -> TextPrimary
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (descargando) {
                        if (progreso >= 0f) {
                            LinearProgressIndicator(
                                progress  = { progreso },
                                modifier  = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(10.dp)),
                                color     = Accent,
                                trackColor = BorderLight
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(10.dp)),
                                color      = Accent,
                                trackColor = BorderLight
                            )
                        }
                    }
                }
            }
        }

        // ── Card de info ──────────────────────────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = AccentLight),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("💡 Cómo usar", style = MaterialTheme.typography.titleMedium, color = Accent)
                Text("1. Copia el enlace del video desde TikTok, YouTube, Instagram, X o Facebook.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("2. Pégalo en el campo de arriba.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("3. Toca Descargar. El video se guardará en tu carpeta Descargas.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LÓGICA DE DESCARGA CON COBALT
// ─────────────────────────────────────────────────────────────────────────────

suspend fun descargarConCobalt(
    videoUrl: String,
    context: Context,
    onProgreso: (Float, String) -> Unit
): Pair<Boolean, String> = withContext(Dispatchers.IO) {

    val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(900, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val jsonBody = JSONObject().apply {
        put("url",                videoUrl.trim())
        put("videoQuality",       "1080")
        put("downloadMode",       "auto")
        put("filenameStyle",      "basic")
        put("youtubeVideoCodec",  "h264")
        put("youtubeVideoContainer", "mp4")
        put("alwaysProxy",        true)
    }.toString()

    val cobaltReq = Request.Builder()
        .url("$COBALT_API/")
        .post(jsonBody.toRequestBody("application/json".toMediaType()))
        .header("Accept",       "application/json")
        .header("Content-Type", "application/json")
        .build()

    val directUrl: String
    val filename: String

    try {
        val cobaltResp = cliente.newCall(cobaltReq).execute()
        val bodyStr    = cobaltResp.body?.string() ?: return@withContext Pair(false, "❌ Cobalt no respondió")
        val json       = JSONObject(bodyStr)
        val status     = json.optString("status", "error")

        when (status) {
            "tunnel", "redirect" -> {
                directUrl = json.getString("url")
                filename  = json.optString("filename", "vdownloader_${System.currentTimeMillis()}.mp4")
            }
            "picker" -> {
                val picker = json.optJSONArray("picker")
                if (picker != null && picker.length() > 0) {
                    directUrl = picker.getJSONObject(0).getString("url")
                    filename  = "vdownloader_${System.currentTimeMillis()}.mp4"
                } else return@withContext Pair(false, "❌ No se encontró video")
            }
            "error" -> {
                val code = json.optJSONObject("error")?.optString("code") ?: "desconocido"
                return@withContext Pair(false, "❌ Cobalt: $code")
            }
            else -> return@withContext Pair(false, "❌ Respuesta inesperada: $status")
        }
    } catch (e: Exception) {
        return@withContext Pair(false, "❌ Error al contactar servidor: ${e.localizedMessage}")
    }

    withContext(Dispatchers.Main) { onProgreso(0f, "Iniciando descarga...") }

    val videoReq = Request.Builder()
        .url(directUrl)
        .header("User-Agent", "VDownloader-Android/2.03")
        .build()

    try {
        val videoResp  = cliente.newCall(videoReq).execute()
        if (!videoResp.isSuccessful)
            return@withContext Pair(false, "❌ Error CDN: HTTP ${videoResp.code}")

        val body       = videoResp.body ?: return@withContext Pair(false, "❌ Respuesta vacía")
        val totalBytes = videoResp.header("Content-Length")?.toLongOrNull() ?: -1L

        val outputStream: OutputStream
        val uriData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE,    "video/mp4")
                put(MediaStore.Downloads.IS_PENDING,   1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                ?: return@withContext Pair(false, "❌ No se pudo crear el archivo")
            outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Pair(false, "❌ No se pudo escribir el archivo")
            Pair(uri, cv)
        } else {
            val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            outputStream = java.io.File(dir, filename).outputStream()
            null
        }

        val buffer   = ByteArray(65536)
        var leidos   = 0L
        var chunk    : Int
        val input    = body.byteStream()

        try {
            while (input.read(buffer).also { chunk = it } != -1) {
                outputStream.write(buffer, 0, chunk)
                leidos += chunk
                if (totalBytes > 0) {
                    val pct = leidos.toFloat() / totalBytes.toFloat()
                    withContext(Dispatchers.Main) { onProgreso(pct, "Descargando...") }
                }
            }
        } finally {
            outputStream.close()
            input.close()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uriData != null) {
            val (uri, cv) = uriData
            cv.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, cv, null, null)
        }

        return@withContext Pair(true, "✅ Guardado en Descargas")

    } catch (e: Exception) {
        return@withContext Pair(false, "❌ Error descargando")
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
fun PreviewPantallaFormal() {
    DescargadorVideosTheme {
        AppShell(
            descargando = true,
            progreso = 0.65f,
            mensajeEstado = "Descargando: 65%",
            onDescargar = { url -> }
        )
    }
}
