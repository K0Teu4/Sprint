package ru.sprint.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SprintTypography = Typography(
    displaySmall = TextStyle(FontFamily.Default, FontWeight.Bold, 30.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(FontFamily.Default, FontWeight.Bold, 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(FontFamily.Default, FontWeight.Bold, 23.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(FontFamily.Default, FontWeight.Bold, 19.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(FontFamily.Default, FontWeight.SemiBold, 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(FontFamily.Default, FontWeight.Normal, 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(FontFamily.Default, FontWeight.Normal, 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(FontFamily.Default, FontWeight.Normal, 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(FontFamily.Default, FontWeight.SemiBold, 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(FontFamily.Default, FontWeight.Medium, 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(FontFamily.Default, FontWeight.SemiBold, 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp)
)
