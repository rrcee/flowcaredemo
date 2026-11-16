package com.flowcare

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowcare.resources.Res
import com.flowcare.resources.jakarta
import org.jetbrains.compose.resources.Font

/** Shared by Patient, Care Team and HQ. All text roles follow Android font scaling. */
internal fun flowCareColors(dark: Boolean = false): ColorScheme = if (dark) {
    darkColorScheme(
        primary = Color(0xFF9FD8EE), onPrimary = Color(0xFF003646),
        primaryContainer = Color(0xFF204D60), onPrimaryContainer = Color(0xFFD8F2FD),
        secondary = Color(0xFF97DDCB), onSecondary = Color(0xFF00382E),
        secondaryContainer = Color(0xFF214D46), onSecondaryContainer = Color(0xFFD5F5EC),
        background = Color(0xFF101E26), onBackground = Color(0xFFE4F0F4),
        surface = Color(0xFF1A2B34), onSurface = Color(0xFFE4F0F4),
        surfaceVariant = Color(0xFF263E49), onSurfaceVariant = Color(0xFFBDD0D9),
        outline = Color(0xFF8CA5B0), outlineVariant = Color(0xFF46616E),
        surfaceTint = Color(0xFF9FD8EE),
    )
} else {
    lightColorScheme(
        primary = Color(0xFF115B85), onPrimary = Color.White,
        primaryContainer = Color(0xFFCBE9F8), onPrimaryContainer = Color(0xFF103D57),
        secondary = Color(0xFF22667F), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE2F1F4), onSecondaryContainer = Color(0xFF164559),
        background = Color(0xFFF9F3E7), onBackground = Color(0xFF142F3E),
        surface = Color(0xFFFFFEFB), onSurface = Color(0xFF142F3E),
        surfaceVariant = Color(0xFFE6F0F5), onSurfaceVariant = Color(0xFF49616E),
        outline = Color(0xFF637D89), outlineVariant = Color(0xFFBCD0D8),
        surfaceTint = Color(0xFF115B85),
    )
}

@Composable
internal fun flowCareTypography(): Typography {
    val font = FontFamily(Font(Res.font.jakarta))
    val base = Typography()
    return Typography(
        headlineLarge = base.headlineLarge.copy(fontFamily = font, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = font, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = font, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = font, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = font, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = font, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = font, fontSize = 18.sp, lineHeight = 28.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = font, fontSize = 17.sp, lineHeight = 26.sp),
        bodySmall = base.bodySmall.copy(fontFamily = font, fontSize = 16.sp, lineHeight = 24.sp),
        labelLarge = base.labelLarge.copy(fontFamily = font, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = font, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = font, fontSize = 14.sp, lineHeight = 20.sp),
    )
}

internal val FlowCareShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
internal fun FlowCareTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = flowCareColors(dark), typography = flowCareTypography(), shapes = FlowCareShapes, content = content)
}

