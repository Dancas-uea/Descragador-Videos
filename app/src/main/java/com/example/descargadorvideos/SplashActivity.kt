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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.descargadorvideos.ui.theme.Accent
import com.example.descargadorvideos.ui.theme.DescargadorVideosTheme
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

    // FIX: estado visible para que la animación tenga desde dónde arrancar (0.4f → 1f)
    var visible by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // FIX: LaunchedEffect unificado — dispara la animación y luego navega
    LaunchedEffect(Unit) {
        visible = true
        delay(2800)
        onFinish()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val dotOffset by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "dot"
    )

    Box(
        modifier = Modifier

            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0544EA), Color(0xFF0D6EE7))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center ,
            modifier = Modifier.fillMaxWidth()
        ) {

            // ── Logo con animación de escala ───────────────────────────────────
            Image(
                painter            = painterResource(id = R.mipmap.icon_do),
                contentDescription = "VIP-Downloader logo",
                modifier           = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .scale(scale)          // ahora sí rebota desde 0.4f
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Nombre ────────────────────────────────────────────────────────
            Text(
                text          = "VIP-Downloader",
                color         = Color.White,
                fontSize      = 28.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text      = "TikTok · YouTube · Instagram · X · Facebook",
                color     = Color.White.copy(alpha = 0.45f),
                fontSize  = 12.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Dots loader ───────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    val alpha = ((dotOffset * 3 - i).let {
                        if (it < 0f) it + 3f else it
                    }.coerceIn(0f, 1f)).let { v ->
                        if (v < 0.5f) v * 2 else 2f - v * 2
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.3f + alpha * 0.7f))
                    )
                }
            }
        }
    }
}