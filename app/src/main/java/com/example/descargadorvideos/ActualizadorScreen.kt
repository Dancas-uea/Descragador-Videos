package com.example.descargadorvideos

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.descargadorvideos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

// ── Modelo de versión ─────────────────────────────────────────────────────────
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String
)

// ── Función para consultar GitHub ─────────────────────────────────────────────
suspend fun consultarVersion(): VersionInfo? = withContext(Dispatchers.IO) {
    try {
        val url  = "https://raw.githubusercontent.com/Dancas-uea/Descragador-Videos/main/release/version.json"
        val json = URL(url).readText()
        val obj  = JSONObject(json)
        VersionInfo(
            versionCode = obj.getInt("versionCode"),
            versionName = obj.getString("versionName"),
            apkUrl      = obj.getString("apkUrl")
        )
    } catch (_: Exception) {
        null
    }
}

// ── Composable principal de actualización ─────────────────────────────────────
@Composable
fun CheckActualizacion() {
    val ctx             = LocalContext.current
    var mostrarDialogo  by remember { mutableStateOf(false) }
    var versionInfo     by remember { mutableStateOf<VersionInfo?>(null) }
    var descargando     by remember { mutableStateOf(false) }
    var progreso        by remember { mutableStateOf(0f) }
    var descargaId      by remember { mutableStateOf(-1L) }

    // ── Al iniciar, consulta si hay nueva versión ─────────────────────────────
    LaunchedEffect(Unit) {
        val info = consultarVersion() ?: return@LaunchedEffect
        val versionActual = ctx.packageManager
            .getPackageInfo(ctx.packageName, 0).versionCode
        if (info.versionCode > versionActual) {
            versionInfo    = info
            mostrarDialogo = true
        }
    }

    // ── Monitorea progreso de descarga ────────────────────────────────────────
    LaunchedEffect(descargaId) {
        if (descargaId == -1L) return@LaunchedEffect
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        while (descargando) {
            val query  = DownloadManager.Query().setFilterById(descargaId)
            val cursor = dm.query(query)
            if (cursor.moveToFirst()) {
                val total     = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val descargado = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val estado    = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (total > 0) progreso = descargado.toFloat() / total.toFloat()
                if (estado == DownloadManager.STATUS_SUCCESSFUL) {
                    progreso    = 1f
                    descargando = false
                } else if (estado == DownloadManager.STATUS_FAILED) {
                    descargando = false
                }
            }
            cursor.close()
            delay(500)
        }
    }

    // ── Diálogo de actualización ──────────────────────────────────────────────
    if (mostrarDialogo && versionInfo != null) {
        Dialog(
            onDismissRequest = { if (!descargando) mostrarDialogo = false },
            properties       = DialogProperties(dismissOnBackPress = !descargando, dismissOnClickOutside = !descargando)
        ) {
            Card(
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Icono
                    AsyncImage(
                        model              = "https://img.icons8.com/color/96/software-installer.png",
                        contentDescription = null,
                        modifier           = Modifier.size(64.dp)
                    )

                    // Título
                    Text(
                        "¡Nueva versión disponible!",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center
                    )

                    // Versión
                    Text(
                        "v${versionInfo!!.versionName}",
                        fontSize  = 14.sp,
                        color     = Accent,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "Descarga la última versión para obtener mejoras y correcciones.",
                        fontSize  = 13.sp,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    // ── Barra de progreso ─────────────────────────────────────
                    if (descargando) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            ) {
                                LinearProgressIndicator(
                                    progress   = { progreso },
                                    modifier   = Modifier.fillMaxSize(),
                                    color      = Accent,
                                    trackColor = BorderLight
                                )
                            }
                            Text(
                                "Descargando... ${(progreso * 100).toInt()}%",
                                fontSize = 12.sp,
                                color    = TextSecondary
                            )
                        }
                    }

                    // ── Botones ───────────────────────────────────────────────
                    if (!descargando) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Botón Ahora no
                            OutlinedButton(
                                onClick  = { mostrarDialogo = false },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextSecondary
                                )
                            ) {
                                Text("Ahora no", fontSize = 13.sp)
                            }

                            // Botón Actualizar
                            Button(
                                onClick = {
                                    descargando = true
                                    progreso    = 0f
                                    descargaId  = iniciarDescargaApk(ctx, versionInfo!!.apkUrl)
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text("Actualizar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Instala el APK cuando termina la descarga ─────────────────────────────
    if (progreso == 1f && !descargando) {
        LaunchedEffect(Unit) {
            delay(500)
            instalarApk(ctx)
            mostrarDialogo = false
        }
    }
}

// ── Inicia descarga con DownloadManager ───────────────────────────────────────
fun iniciarDescargaApk(ctx: Context, apkUrl: String): Long {
    val archivo = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
    if (archivo.exists()) archivo.delete()

    val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
        setTitle("VIP-Downloader - Actualizando")
        setDescription("Descargando nueva versión...")
        setDestinationUri(Uri.fromFile(archivo))
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
    }

    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return dm.enqueue(request)
}

// ── Abre el instalador del APK ────────────────────────────────────────────────
fun instalarApk(ctx: Context) {
    val archivo = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
    if (!archivo.exists()) return

    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", archivo)
    } else {
        Uri.fromFile(archivo)
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}