package com.example.descargadorvideos

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.descargadorvideos.ui.theme.*
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private val procesando    = mutableStateOf(false)
    private val progreso      = mutableStateOf(0f)
    private val mensajeEstado = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar yt-dlp una sola vez al arrancar
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(application)
                // Actualizar al canal estable (nueva firma en 0.17.+)
                YoutubeDL.getInstance().updateYoutubeDL(
                    application,
                    YoutubeDL.UpdateChannel.STABLE
                )
            } catch (e: Exception) {
                // Si falla el update no es crítico, continúa con la versión instalada
            }
        }

        setContent {
            DescargadorVideosTheme {
                AppShell(
                    descargando   = procesando.value,
                    progreso      = progreso.value,
                    mensajeEstado = mensajeEstado.value,
                    onDescargar   = { url -> iniciarDescarga(url) }
                )
            }
        }
    }

    private fun iniciarDescarga(url: String) {
        val urlLimpia = url.trim()
        if (urlLimpia.isEmpty()) {
            Toast.makeText(this, "⚠️ Pega un enlace primero", Toast.LENGTH_SHORT).show()
            return
        }
        if (procesando.value) return

        procesando.value    = true
        progreso.value      = 0f
        mensajeEstado.value = "Extrayendo info del video..."

        lifecycleScope.launch {
            val resultado = descargarConYtDlp(urlLimpia, this@MainActivity) { p, msg ->
                progreso.value      = p
                mensajeEstado.value = msg
            }
            procesando.value    = false
            mensajeEstado.value = resultado.second
            Toast.makeText(this@MainActivity, resultado.second, Toast.LENGTH_LONG).show()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MOTOR: yt-dlp nativo corriendo en el dispositivo
// ─────────────────────────────────────────────────────────────────────────────

suspend fun descargarConYtDlp(
    videoUrl: String,
    context: Context,
    onProgreso: (Float, String) -> Unit
): Pair<Boolean, String> = withContext(Dispatchers.IO) {

    try {
        // Carpeta de descarga temporal dentro de la app
        val carpetaDescarga = File(
            context.getExternalFilesDir(null),
            "VDownloader"
        ).also { it.mkdirs() }

        val nombreArchivo = "video_${System.currentTimeMillis()}.mp4"
        val archivoFinal  = File(carpetaDescarga, nombreArchivo)

        // ── Configurar request de yt-dlp ─────────────────────────────────────
        val request = YoutubeDLRequest(videoUrl).apply {
            addOption("-f", "bestvideo[ext=mp4][vcodec^=avc]+bestaudio[ext=m4a]/best[ext=mp4]/best")
            addOption("-o", archivoFinal.absolutePath)
            addOption("--merge-output-format", "mp4")
            addOption("--no-playlist")
            addOption("--extractor-args", "tiktok:webpage_download=true")
        }

        // ── Ejecutar descarga con callback de progreso ────────────────────────
        YoutubeDL.getInstance().execute(request) { p, _, line ->
            val porcentaje = p / 100f
            val msg = when {
                line.contains("Downloading") -> "Descargando... ${p.toInt()}%"
                line.contains("Merging")     -> "Fusionando video y audio..."
                line.contains("already")     -> "Ya descargado"
                else                         -> "Procesando... ${p.toInt()}%"
            }
            kotlinx.coroutines.runBlocking {
                withContext(Dispatchers.Main) { onProgreso(porcentaje, msg) }
            }
        }

        if (!archivoFinal.exists() || archivoFinal.length() == 0L) {
            return@withContext Pair(false, "❌ El archivo no se generó correctamente")
        }

        // ── Mover a /Downloads públicos con MediaStore ────────────────────────
        withContext(Dispatchers.Main) { onProgreso(0.95f, "Guardando en Descargas...") }

        val nombrePublico = "VDownloader_${System.currentTimeMillis()}.mp4"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, nombrePublico)
                put(MediaStore.Downloads.MIME_TYPE,    "video/mp4")
                put(MediaStore.Downloads.IS_PENDING,   1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv
            ) ?: return@withContext Pair(false, "❌ No se pudo crear archivo en Descargas")

            context.contentResolver.openOutputStream(uri)?.use { out ->
                archivoFinal.inputStream().use { inp -> inp.copyTo(out) }
            }

            cv.clear()
            cv.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, cv, null, null)
        } else {
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            dir.mkdirs()
            archivoFinal.copyTo(File(dir, nombrePublico), overwrite = true)
        }

        archivoFinal.delete()

        return@withContext Pair(true, "✅ Guardado en Descargas: $nombrePublico")

    } catch (e: com.yausername.youtubedl_android.YoutubeDLException) {
        val msg = e.message ?: "Error desconocido"
        return@withContext when {
            "private"     in msg.lowercase() -> Pair(false, "❌ Video privado o eliminado")
            "login"       in msg.lowercase() -> Pair(false, "❌ Este video requiere inicio de sesión")
            "unavailable" in msg.lowercase() -> Pair(false, "❌ Video no disponible en tu región")
            else -> Pair(false, "❌ Error: $msg")
        }
    } catch (e: Exception) {
        return@withContext Pair(false, "❌ Error inesperado: ${e.localizedMessage}")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI
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
            ModalDrawerSheet(
                drawerContainerColor = DrawerBg,
                drawerShape          = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                modifier             = Modifier.width(280.dp)
            ) {
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
                        Text("Motor: yt-dlp nativo", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                DrawerItem("🏠", "Inicio")          { scope.launch { drawerState.close() } }
                DrawerItem("📖", "Cómo descargar")  { scope.launch { drawerState.close() } }
                DrawerItem("⚙️", "Configuraciones") { scope.launch { drawerState.close() } }
                DrawerItem("🎧", "Soporte") {
                    ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:soporte@vdownloader.app")))
                    scope.launch { drawerState.close() }
                }

                Spacer(Modifier.weight(1f))
                Divider(color = DrawerBorder)

                Text(
                    "Plataformas",
                    color    = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("♪ TikTok", "▶ YT", "◈ IG", "𝕏", "f FB").forEach { label ->
                        Text(
                            label,
                            color    = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DrawerSurface)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "VDownloader",
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 17.sp,
                            color         = TextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier            = Modifier.padding(4.dp)
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

@Composable
fun DrawerItem(emoji: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

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
        // ── Chips plataformas ─────────────────────────────────────────────────
        Text("Plataformas soportadas", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("♪" to "TikTok", "▶" to "YouTube", "◈" to "Instagram", "𝕏" to "X", "f" to "FB").forEach { (icon, name) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center
                    ) { Text(icon, fontSize = 20.sp) }
                    Text(name, fontSize = 9.sp, color = TextSecondary)
                }
            }
        }

        // ── Card descarga ─────────────────────────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Pega el enlace del video", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

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
                        cursorColor          = Accent
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
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Descargando...", color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("⬇  Descargar", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }

        // ── Progreso ──────────────────────────────────────────────────────────
        if (descargando || mensajeEstado.isNotEmpty()) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = when {
                        mensajeEstado.startsWith("✅") -> Color(0xFFF0FDF4)
                        mensajeEstado.startsWith("❌") -> Color(0xFFFFF1F2)
                        else -> SurfaceCard
                    }
                ),
                elevation = CardDefaults.cardElevation(0.dp)
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
                        if (progreso > 0f) {
                            LinearProgressIndicator(
                                progress   = { progreso },
                                modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(10.dp)),
                                color      = Accent,
                                trackColor = BorderLight
                            )
                            Text("${(progreso * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
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

        // ── Info card ─────────────────────────────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = AccentLight),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("💡 Cómo usar", style = MaterialTheme.typography.titleMedium, color = Accent)
                Text("1. Copia el enlace desde TikTok, YouTube, Instagram, X o Facebook.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("2. Pégalo arriba y toca Descargar.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("3. El video se guarda en tu carpeta Descargas.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
fun PreviewApp() {
    DescargadorVideosTheme {
        AppShell(descargando = false, progreso = 0f, mensajeEstado = "", onDescargar = {})
    }
}