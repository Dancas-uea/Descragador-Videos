package com.example.descargadorvideos

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import coil.compose.AsyncImage
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.descargadorvideos.ui.theme.*
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {

    private val procesando    = mutableStateOf(false)
    private val progreso      = mutableStateOf(0f)
    private val mensajeEstado = mutableStateOf("")

    // Controla si el splash sigue visible (true = sigue esperando)
    private var splashVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {

        // ── DEBE ser la primera línea, antes de super.onCreate ────────────────
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mantiene el splash hasta que la inicialización termine
        splashScreen.setKeepOnScreenCondition { splashVisible }

        // Inicializar yt-dlp y FFmpeg — cuando terminen, quita el splash
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(application)
                FFmpeg.getInstance().init(application)
                YoutubeDL.getInstance().updateYoutubeDL(
                    application,
                    YoutubeDL.UpdateChannel.STABLE
                )
            } catch (_: Exception) {
                // No crítico — la app puede funcionar igual
            } finally {
                // Quita el splash pase lo que pase
                splashVisible = false
            }
        }

        setContent {
            DescargadorVideosTheme {
                AppShell(
                    descargando   = procesando.value,
                    progreso      = progreso.value,
                    mensajeEstado = mensajeEstado.value,
                    onDescargar   = { url, formato -> iniciarDescarga(url, formato) }
                )
            }
        }
    }

    private fun iniciarDescarga(url: String, formato: String) {
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
            val resultado = descargarConYtDlp(urlLimpia, formato, this@MainActivity) { p, msg ->
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
    formato: String,
    context: Context,
    onProgreso: (Float, String) -> Unit
): Pair<Boolean, String> = withContext(Dispatchers.IO) {

    try {
        val carpetaDescarga = File(
            context.getExternalFilesDir(null),
            "Downloader"
        ).also { it.mkdirs() }

        val urlLower  = videoUrl.lowercase()
        val esYoutube = "youtube.com" in urlLower || "youtu.be" in urlLower
        val esTwitter = "twitter.com" in urlLower || "x.com" in urlLower || "t.co" in urlLower

        val prefijo       = "video_${System.currentTimeMillis()}"
        val rutaPlantilla = File(carpetaDescarga, "$prefijo.%(ext)s").absolutePath

        val esMp3 = formato == "MP3"

        val request = YoutubeDLRequest(videoUrl).apply {
            addOption("--no-playlist")

            if (esMp3) {
                addOption("-x")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", "0")
            } else {
                when {
                    esYoutube -> {
                        addOption("-f", "bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]/bestvideo+bestaudio/best")
                        addOption("--merge-output-format", "mp4")
                    }
                    esTwitter -> {
                        addOption("-f", "best[ext=mp4]/best")
                        addOption("--extractor-args", "twitter:api=syndication")
                    }
                    else -> {
                        addOption("-f", "best[ext=mp4]/best")
                        addOption("--extractor-args", "tiktok:webpage_download=true")
                    }
                }
            }

            addOption("-o", rutaPlantilla)
        }

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

        val archivoFinal = carpetaDescarga.listFiles()
            ?.filter { it.name.startsWith(prefijo) && it.length() > 0 }
            ?.maxByOrNull { it.lastModified() }

        if (archivoFinal == null) {
            return@withContext Pair(false, "❌ El archivo no se generó correctamente")
        }

        withContext(Dispatchers.Main) { onProgreso(0.95f, "Guardando en Descargas...") }

        val extFinal      = archivoFinal.extension.ifEmpty { if (esMp3) "mp3" else "mp4" }
        val mimeType      = if (esMp3) "audio/mpeg" else "video/mp4"
        val nombrePublico = "VIP-Downloader_${System.currentTimeMillis()}.$extFinal"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, nombrePublico)
                put(MediaStore.Downloads.MIME_TYPE,    mimeType)
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
            "private"      in msg.lowercase() -> Pair(false, "❌ Video privado o eliminado")
            "login"        in msg.lowercase() -> Pair(false, "❌ Este video requiere inicio de sesión")
            "unavailable"  in msg.lowercase() -> Pair(false, "❌ Video no disponible en tu región")
            "no video"     in msg.lowercase() -> Pair(false, "❌ Este tweet no contiene video o no es público")
            "syndication"  in msg.lowercase() -> Pair(false, "❌ Tweet no accesible — puede ser privado o sin video")
            "not all meta" in msg.lowercase() -> Pair(false, "❌ Tweet no accesible — puede ser privado o sin video")
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
    onDescargar: (String, String) -> Unit
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
                        .padding(20.dp)
                        .padding(top = 20.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(icon),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.icon_do_round),
                                contentDescription = "VIP-Downloader",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("VIP-Downloader", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Version v1.0.2", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                DrawerItem("https://img.icons8.com/color/48/home.png",          "Inicio")         { scope.launch { drawerState.close() } }
                DrawerItem("https://img.icons8.com/color/48/open-book.png",     "Cómo descargar") { scope.launch { drawerState.close() } }
                DrawerItem("https://img.icons8.com/color/48/settings.png",      "Configuraciones"){ scope.launch { drawerState.close() } }
                DrawerItem("https://img.icons8.com/color/48/headset.png",       "Soporte") {
                    ctx.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:soporte@vdownloader.app".toUri()))
                    scope.launch { drawerState.close() }
                }


                Spacer(Modifier.weight(1f))  // empuja el texto hacia abajo
                HorizontalDivider(color = DrawerBorder)
                Text(
                    "Desarrollado por Carlos Castillo",
                    color    = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )

            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "VIP-Downloader v1.0.2",
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
fun DrawerItem(iconUrl: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AsyncImage(
            model              = iconUrl,
            contentDescription = label,
            modifier           = Modifier.size(22.dp)
        )
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier,
    descargando: Boolean,
    progreso: Float,
    mensajeEstado: String,
    onDescargar: (String, String) -> Unit
) {
    val contexto  = LocalContext.current
    var urlVideo  by remember { mutableStateOf("") }
    var formatoSel by remember { mutableStateOf("MP4") }
    val scroll    = rememberScrollState()

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
            listOf(
                "https://img.icons8.com/color/48/tiktok.png"        to "TikTok",
                "https://img.icons8.com/color/48/youtube-play.png"  to "YouTube",
                "https://img.icons8.com/color/48/instagram-new.png" to "Instagram",
                "https://img.icons8.com/color/48/twitterx.png"      to "X",
                "https://img.icons8.com/color/48/facebook.png"      to "Facebook"
            )
                .forEach { (url, name) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = name,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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

                // ── Selector MP4 / MP3 ────────────────────────────────────────
                val iconos = mapOf(
                    "MP4" to "https://img.icons8.com/color/48/video.png",
                    "MP3" to "https://img.icons8.com/color/48/music.png"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("MP4" to "Video", "MP3" to "Audio").forEach { (valor, etiqueta) ->
                        val seleccionado = formatoSel == valor
                        OutlinedButton(
                            onClick  = { if (!descargando) formatoSel = valor },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(10.dp),
                            border   = BorderStroke(
                                width = if (seleccionado) 2.dp else 1.dp,
                                color = if (seleccionado) Accent else BorderLight
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (seleccionado) AccentLight else Color.Transparent
                            )
                        ) {
                            AsyncImage(
                                model = iconos[valor],
                                contentDescription = etiqueta,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                etiqueta,
                                color      = if (seleccionado) Accent else TextSecondary,
                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 13.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (urlVideo.isNotEmpty()) onDescargar(urlVideo, formatoSel)
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
                        val iconUrl = if (formatoSel == "MP3") "https://img.icons8.com/color/48/music.png"
                        else "https://img.icons8.com/color/48/video.png"
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Descargar $formatoSel", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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

                    // ── Icono + mensaje ───────────────────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val iconoUrl = when {
                            mensajeEstado.startsWith("✅") -> "https://img.icons8.com/color/48/ok--v1.png"
                            mensajeEstado.startsWith("❌") -> "https://img.icons8.com/color/48/cancel.png"
                            else -> null
                        }
                        if (iconoUrl != null) {
                            AsyncImage(
                                model = iconoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            // Quita el emoji ✅ o ❌ del inicio del texto
                            text  = mensajeEstado.removePrefix("✅").removePrefix("❌").trimStart(),
                            color = when {
                                mensajeEstado.startsWith("✅") -> Success
                                mensajeEstado.startsWith("❌") -> Error
                                else -> TextPrimary
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

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
        AppShell(descargando = false, progreso = 0f, mensajeEstado = "", onDescargar = { _, _ -> })
    }
}