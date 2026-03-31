package com.example.descargadorvideos


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.descargadorvideos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionesScreen(onBack: () -> Unit) {

    // ── Estados de configuración ──────────────────────────────────────────────
    var calidadSel   by remember { mutableStateOf("1080p") }
    var formatoSel   by remember { mutableStateOf("MP4") }
    var descargasSel by remember { mutableStateOf("2") }
    var temaOscuro   by remember { mutableStateOf(false) }
    var idiomaSel    by remember { mutableStateOf("Español") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Configuraciones",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Default.ArrowBack,
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

            // ── Sección Descargas ─────────────────────────────────────────────
            SeccionTitulo(
                icono = "https://img.icons8.com/color/48/downloading-updates.png",
                titulo = "Descargas"
            )

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Calidad de video
                    Text("Calidad de video", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("720p", "1080p", "4K").forEach { cal ->
                            val sel = calidadSel == cal
                            FilterChip(
                                selected = sel,
                                onClick  = { calidadSel = cal },
                                label    = { Text(cal, fontSize = 12.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor    = AccentLight,
                                    selectedLabelColor        = Accent,
                                    containerColor            = Surface,
                                    labelColor                = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled          = true,
                                    selected         = sel,
                                    selectedBorderColor = Accent,
                                    borderColor      = BorderLight
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BorderLight)

                    // Formato por defecto
                    Text("Formato por defecto", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MP4" to "Video", "MP3" to "Audio").forEach { (val_, label) ->
                            val sel = formatoSel == val_
                            FilterChip(
                                selected = sel,
                                onClick  = { formatoSel = val_ },
                                label    = { Text("$val_ · $label", fontSize = 12.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentLight,
                                    selectedLabelColor     = Accent,
                                    containerColor         = Surface,
                                    labelColor             = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = sel,
                                    selectedBorderColor = Accent,
                                    borderColor         = BorderLight
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BorderLight)

                    // Descargas simultáneas
                    Text("Descargas simultáneas", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1", "2", "3").forEach { n ->
                            val sel = descargasSel == n
                            FilterChip(
                                selected = sel,
                                onClick  = { descargasSel = n },
                                label    = { Text(n, fontSize = 12.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentLight,
                                    selectedLabelColor     = Accent,
                                    containerColor         = Surface,
                                    labelColor             = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = sel,
                                    selectedBorderColor = Accent,
                                    borderColor         = BorderLight
                                )
                            )
                        }
                    }
                }
            }

            // ── Sección Apariencia ────────────────────────────────────────────
            SeccionTitulo(
                icono  = "https://img.icons8.com/color/48/paint-palette.png",
                titulo = "Apariencia"
            )

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Tema oscuro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tema oscuro", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                            Text("Cambia el fondo de la app", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked         = temaOscuro,
                            onCheckedChange = { temaOscuro = it },
                            colors          = SwitchDefaults.colors(
                                checkedThumbColor  = Color.White,
                                checkedTrackColor  = Accent,
                                uncheckedTrackColor = BorderLight
                            )
                        )
                    }

                    HorizontalDivider(color = BorderLight)

                    // Idioma
                    Text("Idioma", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Español", "English", "Português").forEach { idioma ->
                            val sel = idiomaSel == idioma
                            FilterChip(
                                selected = sel,
                                onClick  = { idiomaSel = idioma },
                                label    = { Text(idioma, fontSize = 12.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentLight,
                                    selectedLabelColor     = Accent,
                                    containerColor         = Surface,
                                    labelColor             = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = sel,
                                    selectedBorderColor = Accent,
                                    borderColor         = BorderLight
                                )
                            )
                        }
                    }
                }
            }

            // ── Nota informativa ──────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = AccentLight),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "https://img.icons8.com/color/48/info.png",
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Algunas configuraciones se aplicarán en la próxima descarga.",
                        color    = Accent,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SeccionTitulo(icono: String, titulo: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = icono,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Text(
            titulo,
            fontWeight = FontWeight.Bold,
            fontSize   = 13.sp,
            color      = TextSecondary
        )
    }
}