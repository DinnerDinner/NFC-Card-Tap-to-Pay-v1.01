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

// -------------- COLORS ------------------

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D5AFE),         // Indigo A700
    secondary = Color(0xFF00C853),       // Green A700
    background = Color(0xFFFDFDFD),       // Off-white
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF536DFE),         // Indigo 400
    secondary = Color(0xFF00E676),       // Green A400
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

// -------------- TYPOGRAPHY ------------------

// Replace with your fonts if needed
//private val Inter = FontFamily(
//    Font(R.font.inter_regular, FontWeight.Normal),
//    Font(R.font.inter_bold, FontWeight.Bold)
//)
//
//private val AppTypography = Typography(
//    displayLarge = TextStyle(
//        fontFamily = Inter,
//        fontWeight = FontWeight.Bold,
//        fontSize = 48.sp,
//        lineHeight = 54.sp
//    ),
//    headlineMedium = TextStyle(
//        fontFamily = Inter,
//        fontWeight = FontWeight.SemiBold,
//        fontSize = 24.sp
//    ),
//    bodyLarge = TextStyle(
//        fontFamily = Inter,
//        fontWeight = FontWeight.Normal,
//        fontSize = 16.sp
//    ),
//    labelLarge = TextStyle(
//        fontFamily = Inter,
//        fontWeight = FontWeight.Medium,
//        fontSize = 14.sp
//    )
//)

// -------------- THEME WRAPPER ------------------

@Composable
fun NFCCardTapToPayV101Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
