package com.example.descargadorvideos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.descargadorvideos.ui.theme.Accent
import com.example.descargadorvideos.ui.theme.BorderLight
import com.example.descargadorvideos.ui.theme.DescargadorVideosTheme
import com.example.descargadorvideos.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DescargadorVideosTheme {
                SplashScreen {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {

    // ── Animación de entrada del logo ─────────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // ── Barra de progreso animada de 0 → 1 en 2.5s ───────────────────────────
    var progreso by remember { mutableStateOf(0f) }
    val progresoAnimado by animateFloatAsState(
        targetValue   = progreso,
        animationSpec = tween(durationMillis = 2500, easing = LinearEasing),
        label         = "progreso"
    )

    LaunchedEffect(Unit) {
        visible  = true
        progreso = 1f
        delay(2800)
        onFinish()
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {

        // ── Contenido central ─────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {

            // ── Logo ──────────────────────────────────────────────────────────
            Image(
                painter            = painterResource(id = R.drawable.icon_do_round),
                contentDescription = "VIP-Downloader logo",
                modifier           = Modifier
                    .scale(scale)
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(Modifier.height(28.dp))

            // ── Nombre ────────────────────────────────────────────────────────
            Text(
                text          = "VIP-Downloader",
                color         = Color(0xFF0D0F14),
                fontSize      = 30.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text      = "TikTok · YouTube · Instagram · X · Facebook",
                color     = TextSecondary,
                fontSize  = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // ── Barra de progreso ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BorderLight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progresoAnimado)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Accent)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text     = "Cargando ${(progresoAnimado * 100).toInt()}%",
                color    = TextSecondary,
                fontSize = 11.sp
            )
        }

        // ── Versión abajo del todo ────────────────────────────────────────────
        Text(
            text     = "v1.0.0",
            color    = TextSecondary.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}