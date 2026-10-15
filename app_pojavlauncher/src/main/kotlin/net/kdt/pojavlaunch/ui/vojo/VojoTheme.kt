package net.kdt.pojavlaunch.ui.vojo

import android.app.Activity
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat

private val GlacierDark = darkColorScheme(
    primary = Color(0xFF7DD3FC), onPrimary = Color(0xFF082032),
    primaryContainer = Color(0xFF243C51), onPrimaryContainer = Color(0xFFEAF2FA),
    secondary = Color(0xFFA5B4FC), onSecondary = Color(0xFF182232),
    background = Color(0xFF0C121C), onBackground = Color(0xFFEAF2FA),
    surface = Color(0xFF131E2B), onSurface = Color(0xFFEAF2FA),
    surfaceVariant = Color(0xFF1C2A3B), onSurfaceVariant = Color(0xFFA0AEC0),
    surfaceContainer = Color(0xFF131E2B), surfaceContainerHigh = Color(0xFF1C2A3B),
    error = Color(0xFFF87171), onError = Color(0xFF330C0C), outline = Color(0xFF667C94))
private val GlacierLight = lightColorScheme(
    primary = Color(0xFF075985), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E9F5), onPrimaryContainer = Color(0xFF082032),
    secondary = Color(0xFF4338CA), onSecondary = Color.White,
    background = Color(0xFFF4F7FB), onBackground = Color(0xFF182232),
    surface = Color.White, onSurface = Color(0xFF182232),
    surfaceVariant = Color(0xFFE6EDF7), onSurfaceVariant = Color(0xFF526176),
    surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFE6EDF7),
    error = Color(0xFFB91C1C), outline = Color(0xFF62748A))

private val VojoTypography = Typography(
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 29.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp))

@Composable
fun VojoTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val colors = if (dark) GlacierDark else GlacierLight
    val activity = LocalContext.current as? Activity
    SideEffect {
        activity?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = (if (Build.VERSION.SDK_INT < 23) GlacierDark.background else colors.background).toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = (if (Build.VERSION.SDK_INT < 26) GlacierDark.surface else colors.surface).toArgb()
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = VojoTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp)), content = content)
}

@Composable
fun rememberReducedMotion(): Boolean {
    if (LocalInspectionMode.current) return true
    val resolver = LocalContext.current.contentResolver
    fun disabled() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduced by remember { mutableStateOf(disabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced = disabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}
