package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = OrangePrimary,
    secondary = OrangeSecondary,
    tertiary = DarkGreyMuted,
    background = DarkBg,
    surface = DarkCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = DarkGreyMuted
  )

private val LightColorScheme =
  lightColorScheme(
    primary = OrangePrimary,
    secondary = OrangeSecondary,
    tertiary = EditorialMuted,
    background = ScandinavianBg,
    surface = SoftCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = EditorialNearBlack,
    onSurface = EditorialNearBlack,
    surfaceVariant = SurfaceVariantBg,
    onSurfaceVariant = EditorialMuted,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors by default to preserve the premium Scandinavian College Orange design system
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
