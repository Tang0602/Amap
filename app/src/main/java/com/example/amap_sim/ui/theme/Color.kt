package com.example.amap_sim.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 仿高德地图配色方案
 */

// 主题色 - 高德蓝
val AmapBlue = Color(0xFF3385FF)
val AmapBlueDark = Color(0xFF0066CC)
val AmapBlueLight = Color(0xFF66A3FF)

// 辅助色
val AmapGreen = Color(0xFF00CC66)      // 路线绿
val AmapOrange = Color(0xFFFF8C00)     // 警示橙
val AmapRed = Color(0xFFFF4D4D)        // 错误红
val AmapYellow = Color(0xFFFFCC00)     // 注意黄

// 中性色
val Gray50 = Color(0xFFFAFAFA)
val Gray100 = Color(0xFFF5F5F5)
val Gray200 = Color(0xFFEEEEEE)
val Gray300 = Color(0xFFE0E0E0)
val Gray400 = Color(0xFFBDBDBD)
val Gray500 = Color(0xFF9E9E9E)
val Gray600 = Color(0xFF757575)
val Gray700 = Color(0xFF616161)
val Gray800 = Color(0xFF424242)
val Gray900 = Color(0xFF212121)

// 背景色
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF121212)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1E1E1E)

// 文本色
val TextPrimaryLight = Color(0xFF212121)
val TextSecondaryLight = Color(0xFF757575)
val TextTertiaryLight = Color(0xFF9E9E9E)
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFFB3B3B3)
val TextTertiaryDark = Color(0xFF808080)

// 地图相关颜色
val MapRouteColor = Color(0xFF3385FF)        // 路线颜色
val MapRouteWalkColor = Color(0xFF00CC66)    // 步行路线
val MapRouteBikeColor = Color(0xFF9933FF)    // 骑行路线
val MapMarkerStart = Color(0xFF00CC66)       // 起点标记
val MapMarkerEnd = Color(0xFFFF4D4D)         // 终点标记
val MapMarkerPoi = Color(0xFF3385FF)         // POI 标记
val MapLocationCircle = Color(0x333385FF)    // 位置圈（半透明）

// Material 3 配色
val md_theme_light_primary = AmapBlue
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFD6E3FF)
val md_theme_light_onPrimaryContainer = Color(0xFF001B3E)
val md_theme_light_secondary = Color(0xFF565E71)
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = Color(0xFFDAE2F9)
val md_theme_light_onSecondaryContainer = Color(0xFF131B2C)
val md_theme_light_tertiary = Color(0xFF705574)
val md_theme_light_onTertiary = Color.White
val md_theme_light_tertiaryContainer = Color(0xFFFAD8FD)
val md_theme_light_onTertiaryContainer = Color(0xFF28132E)
val md_theme_light_error = AmapRed
val md_theme_light_onError = Color.White
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = BackgroundLight
val md_theme_light_onBackground = Gray900
val md_theme_light_surface = SurfaceLight
val md_theme_light_onSurface = Gray900
val md_theme_light_surfaceVariant = Color(0xFFE1E2EC)
val md_theme_light_onSurfaceVariant = Color(0xFF44474F)
val md_theme_light_outline = Color(0xFF74777F)
val md_theme_light_outlineVariant = Color(0xFFC4C6D0)

val md_theme_dark_primary = AmapBlueLight
val md_theme_dark_onPrimary = Color(0xFF002F64)
val md_theme_dark_primaryContainer = Color(0xFF00458D)
val md_theme_dark_onPrimaryContainer = Color(0xFFD6E3FF)
val md_theme_dark_secondary = Color(0xFFBEC6DC)
val md_theme_dark_onSecondary = Color(0xFF283041)
val md_theme_dark_secondaryContainer = Color(0xFF3E4759)
val md_theme_dark_onSecondaryContainer = Color(0xFFDAE2F9)
val md_theme_dark_tertiary = Color(0xFFDDBCE0)
val md_theme_dark_onTertiary = Color(0xFF3F2844)
val md_theme_dark_tertiaryContainer = Color(0xFF573E5C)
val md_theme_dark_onTertiaryContainer = Color(0xFFFAD8FD)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = BackgroundDark
val md_theme_dark_onBackground = Color(0xFFE3E2E6)
val md_theme_dark_surface = SurfaceDark
val md_theme_dark_onSurface = Color(0xFFE3E2E6)
val md_theme_dark_surfaceVariant = Color(0xFF44474F)
val md_theme_dark_onSurfaceVariant = Color(0xFFC4C6D0)
val md_theme_dark_outline = Color(0xFF8E9099)
val md_theme_dark_outlineVariant = Color(0xFF44474F)

// 护眼模式配色（灰色主题 - 更深的灰色）
val md_theme_eyecare_primary = Color(0xFF5B6370)
val md_theme_eyecare_onPrimary = Color.White
val md_theme_eyecare_primaryContainer = Color(0xFFB8BCC4)
val md_theme_eyecare_onPrimaryContainer = Color(0xFF1C1F26)
val md_theme_eyecare_secondary = Color(0xFF5B6370)
val md_theme_eyecare_onSecondary = Color.White
val md_theme_eyecare_secondaryContainer = Color(0xFFCBCED6)
val md_theme_eyecare_onSecondaryContainer = Color(0xFF2A2E36)
val md_theme_eyecare_tertiary = Color(0xFF7D8694)
val md_theme_eyecare_onTertiary = Color.White
val md_theme_eyecare_tertiaryContainer = Color(0xFFCBCED6)
val md_theme_eyecare_onTertiaryContainer = Color(0xFF2A2E36)
val md_theme_eyecare_error = Color(0xFFD64545)
val md_theme_eyecare_onError = Color.White
val md_theme_eyecare_errorContainer = Color(0xFFE8C5C5)
val md_theme_eyecare_onErrorContainer = Color(0xFF5C1F1F)
val md_theme_eyecare_background = Color(0xFFDCDFE4)
val md_theme_eyecare_onBackground = Color(0xFF1C1F26)
val md_theme_eyecare_surface = Color(0xFFE8EAEE)
val md_theme_eyecare_onSurface = Color(0xFF1C1F26)
val md_theme_eyecare_surfaceVariant = Color(0xFFCBCED6)
val md_theme_eyecare_onSurfaceVariant = Color(0xFF3A3E46)
val md_theme_eyecare_outline = Color(0xFF7D8694)
val md_theme_eyecare_outlineVariant = Color(0xFFB8BCC4)
