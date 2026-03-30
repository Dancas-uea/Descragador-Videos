package com.example.descargadorvideos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.descargadorvideos.ui.theme.Accent
import com.example.descargadorvideos.ui.theme.AccentDark
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
    // Animaciones
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val dotOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "dot"
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(2800)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1D26), Color(0xFF0D0F14)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ── Logo placeholder ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Accent, AccentDark))),
                contentAlignment = Alignment.Center
            ) {
                // Icono de descarga SVG-like con Canvas
                Text("↓", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Bold)
                // REEMPLAZA el Text("↓"...) por tu Image(@drawable/ic_launcher) cuando tengas el icono
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "VDownloader",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                "TikTok · YouTube · Instagram · X · Facebook",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

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