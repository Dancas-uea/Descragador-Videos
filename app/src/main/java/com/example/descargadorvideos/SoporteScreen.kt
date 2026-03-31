package com.example.descargadorvideos

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.descargadorvideos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoporteScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Soporte",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint               = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SurfaceCard
                )
            )
        },
        containerColor = OffWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Ayuda ─────────────────────────────────────────────────────────
            SeccionTitulo(
                icono  = "https://img.icons8.com/color/48/help.png",
                titulo = "Ayuda"
            )

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    SoporteItem(
                        icono    = "https://img.icons8.com/color/48/filled-sent.png",
                        titulo   = "Reportar un problema",
                        subtitulo = "soporte@vdownloader.app",
                        onClick  = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_SENDTO, "mailto:soporte@vdownloader.app".toUri())
                            )
                        }
                    )
                    HorizontalDivider(color = BorderLight, modifier = Modifier.padding(horizontal = 16.dp))
                    SoporteItem(
                        icono    = "https://img.icons8.com/color/48/star.png",
                        titulo   = "Calificar la app",
                        subtitulo = "¡Tu opinión nos ayuda mucho!",
                        onClick  = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=com.example.descargadorvideos".toUri())
                            )
                        }
                    )
                }
            }

            // ── Legal ─────────────────────────────────────────────────────────
            SeccionTitulo(
                icono  = "https://img.icons8.com/color/48/contract.png",
                titulo = "Legal"
            )

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    SoporteItem(
                        icono    = "https://img.icons8.com/color/48/privacy.png",
                        titulo   = "Política de privacidad",
                        subtitulo = "Cómo manejamos tus datos",
                        onClick  = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://vdownloader.app/privacidad".toUri())
                            )
                        }
                    )
                    HorizontalDivider(color = BorderLight, modifier = Modifier.padding(horizontal = 16.dp))
                    SoporteItem(
                        icono    = "https://img.icons8.com/color/48/terms-and-conditions.png",
                        titulo   = "Términos de uso",
                        subtitulo = "Condiciones del servicio",
                        onClick  = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://vdownloader.app/terminos".toUri())
                            )
                        }
                    )
                }
            }

            // ── Redes sociales ────────────────────────────────────────────────
            SeccionTitulo(
                icono  = "https://img.icons8.com/color/48/share.png",
                titulo = "Síguenos"
            )

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    SoporteItem(
                        icono    = "https://img.icons8.com/color/48/instagram-new.png",
                        titulo   = "Instagram",
                        subtitulo = "@vipdownloader",
                        onClick  = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://instagram.com/vipdownloader".toUri())
                            )
                        }
                    )
                    HorizontalDivider(color = BorderLight, modifier = Modifier.padding(horizontal = 16.dp))
                    SoporteItem(
                        icono    = "https://img.icons8.com/color/48/twitterx.png",
                        titulo   = "X (Twitter)",
                        subtitulo = "@vipdownloader",
                        onClick  = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://x.com/vipdownloader".toUri())
                            )
                        }
                    )
                }
            }

            // ── Versión ───────────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = AccentLight),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = "https://img.icons8.com/color/48/info.png",
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Versión de la app", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Text("v1.0.2", color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SoporteItem(icono: String, titulo: String, subtitulo: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AsyncImage(
            model = icono,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitulo, color = TextSecondary, fontSize = 12.sp)
        }
        AsyncImage(
            model = "https://img.icons8.com/ios-filled/96/ffffff/chevron-right.png",
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}