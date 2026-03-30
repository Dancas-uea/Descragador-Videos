package com.example.descargadorvideos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.4.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp, letterSpacing = 0.4.sp),
)