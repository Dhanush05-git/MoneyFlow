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

private val DarkColorScheme = darkColorScheme(
  primary = Emerald400,
  onPrimary = Slate950,
  primaryContainer = Emerald700,
  onPrimaryContainer = Emerald50,
  secondary = TealAccent,
  onSecondary = Slate950,
  tertiary = IndigoAccent,
  background = Slate950,
  onBackground = Color(0xFFF1F5F9),
  surface = Slate900,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = Slate800,
  onSurfaceVariant = Slate400,
  outline = Slate700,
  error = CoralRose
)

private val LightColorScheme = lightColorScheme(
  primary = Emerald600,
  onPrimary = Color.White,
  primaryContainer = Emerald100,
  onPrimaryContainer = Emerald700,
  secondary = TealAccent,
  onSecondary = Color.White,
  tertiary = IndigoAccent,
  background = Slate50,
  onBackground = Slate900,
  surface = Color.White,
  onSurface = Slate900,
  surfaceVariant = Slate100,
  onSurfaceVariant = Slate600,
  outline = Slate200,
  error = CoralRose
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent fintech brand identity
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

