package com.example.nfccardtaptopayv101.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.nfccardtaptopayv101.R

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D5AFE),
    secondary = Color(0xFF00C853),
    background = Color(0xFFFDFDFD),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF536DFE),
    secondary = Color(0xFF00E676),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

// 1. Define font family with your new Consent font(s)
val Quicksand = FontFamily(
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_bold, FontWeight.Bold)// <-- changed here from Inter to Consent, also updated font file and weight accordingly
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Quicksand,   // <-- changed here: was Inter
        fontWeight = FontWeight.Bold, // adjust weight if needed, was Black before
        fontSize = 48.sp,
        lineHeight = 54.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Quicksand,   // <-- changed here
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Quicksand,   // <-- changed here
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Quicksand,   // <-- changed here
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
)


@Composable
fun NFCCardTapToPayV101Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
