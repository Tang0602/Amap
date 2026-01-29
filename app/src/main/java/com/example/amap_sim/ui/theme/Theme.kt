package com.example.amap_sim.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.amap_sim.data.local.AppTheme

/**
 * 亮色主题
 */
private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant
)

/**
 * 暗色主题
 */
private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant
)

/**
 * 护眼模式主题（灰色主题）
 */
private val EyeCareColorScheme = lightColorScheme(
    primary = md_theme_eyecare_primary,
    onPrimary = md_theme_eyecare_onPrimary,
    primaryContainer = md_theme_eyecare_primaryContainer,
    onPrimaryContainer = md_theme_eyecare_onPrimaryContainer,
    secondary = md_theme_eyecare_secondary,
    onSecondary = md_theme_eyecare_onSecondary,
    secondaryContainer = md_theme_eyecare_secondaryContainer,
    onSecondaryContainer = md_theme_eyecare_onSecondaryContainer,
    tertiary = md_theme_eyecare_tertiary,
    onTertiary = md_theme_eyecare_onTertiary,
    tertiaryContainer = md_theme_eyecare_tertiaryContainer,
    onTertiaryContainer = md_theme_eyecare_onTertiaryContainer,
    error = md_theme_eyecare_error,
    onError = md_theme_eyecare_onError,
    errorContainer = md_theme_eyecare_errorContainer,
    onErrorContainer = md_theme_eyecare_onErrorContainer,
    background = md_theme_eyecare_background,
    onBackground = md_theme_eyecare_onBackground,
    surface = md_theme_eyecare_surface,
    onSurface = md_theme_eyecare_onSurface,
    surfaceVariant = md_theme_eyecare_surfaceVariant,
    onSurfaceVariant = md_theme_eyecare_onSurfaceVariant,
    outline = md_theme_eyecare_outline,
    outlineVariant = md_theme_eyecare_outlineVariant
)

/**
 * 仿高德地图主题
 *
 * @param appTheme 应用主题（明亮模式、夜间模式、护眼模式）
 * @param dynamicColor 是否使用动态颜色（Android 12+）
 */
@Composable
fun AmapSimTheme(
    appTheme: AppTheme = AppTheme.BRIGHT,
    dynamicColor: Boolean = false, // 默认关闭，保持高德风格
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            when (appTheme) {
                AppTheme.NIGHT -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }
        else -> when (appTheme) {
            AppTheme.BRIGHT -> LightColorScheme
            AppTheme.NIGHT -> DarkColorScheme
            AppTheme.EYE_CARE -> EyeCareColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏颜色
            window.statusBarColor = Color.Transparent.toArgb()
            // 设置状态栏图标颜色
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = appTheme != AppTheme.NIGHT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
