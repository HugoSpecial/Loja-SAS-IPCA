package ipca.project.lojasas.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- CONFIGURAÇÃO DO TEMA ESCURO ---
private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = White,

    secondary = GreenPrimary,
    onSecondary = White,

    tertiary = RedPrimary,

    background = DarkBackground, // O fundo geral continua o mais escuro (1C1B1F)
    onBackground = White,

    // --- A MUDANÇA ESTÁ AQUI ---
    surface = DarkSurface,       // Os cards agora usam o cinza mais claro (2C2C2C)
    onSurface = White,

    error = RedPrimary,
    onError = White
)

// --- CONFIGURAÇÃO DO TEMA CLARO ---
private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = White,

    secondary = GreenPrimary,
    onSecondary = White,

    tertiary = RedPrimary,

    background = MyBackColor,
    onBackground = Black,

    surface = White,
    onSurface = Black,

    error = RedPrimary,
    onError = White
)

@Composable
fun LojaSASTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // --- LÓGICA DA BARRA DE STATUS ---
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}