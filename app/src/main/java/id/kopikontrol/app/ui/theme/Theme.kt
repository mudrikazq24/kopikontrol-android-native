package id.kopikontrol.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val Coffee = Color(0xFF4B2E22)
val Caramel = Color(0xFFB56729)
val Cream = Color(0xFFFAF8F3)
val Paper = Color(0xFFFFFDF8)
val Forest = Color(0xFF263F41)
val Ink = Color(0xFF402A20)
val Muted = Color(0xFF75665C)
val Line = Color(0xFFDED5C9)
val Success = Color(0xFF2E6B52)
val Danger = Color(0xFFC8102E)

private val KopiColors = lightColorScheme(
    primary = Coffee,
    onPrimary = Color.White,
    secondary = Caramel,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF4EEE8),
    onSurfaceVariant = Muted,
    outline = Line,
    error = Danger,
)

private val KopiTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.7.sp),
)

@Composable
fun KopiKontrolTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Cream.toArgb()
            window.navigationBarColor = Forest.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(colorScheme = KopiColors, typography = KopiTypography, shapes = Shapes(), content = content)
}
