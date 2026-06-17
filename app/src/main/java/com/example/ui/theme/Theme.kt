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
  darkTheme: Boolean = false, // Always launch in Light Mode by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always use the customized premium LightColorScheme
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
