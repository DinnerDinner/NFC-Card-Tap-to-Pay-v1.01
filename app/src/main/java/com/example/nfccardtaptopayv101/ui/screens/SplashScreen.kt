//package com.example.nfccardtaptopayv101.ui.screens
//
//import androidx.compose.animation.core.FastOutSlowInEasing
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.nfccardtaptopayv101.R
//import kotlinx.coroutines.delay
//
//@Composable
//fun SplashScreen(content: @Composable () -> Unit) {
//    var animationPhase by remember { mutableStateOf(0) }
//
//    // Animation states
//    val logoAlpha by animateFloatAsState(
//        targetValue = when (animationPhase) {
//            0 -> 0f
//            1 -> 1f
//            2 -> 0f
//            else -> 0f
//        },
//        animationSpec = tween(
//            durationMillis = when (animationPhase) {
//                0 -> 800  // Fade in
//                1 -> 0    // Stay visible
//                2 -> 600  // Fade out
//                else -> 0
//            }
//        ),
//        label = "logoAlpha"
//    )
//
//    val logoScale by animateFloatAsState(
//        targetValue = when (animationPhase) {
//            0 -> 0.3f
//            1 -> 1f
//            2 -> 1.2f
//            else -> 0.3f
//        },
//        animationSpec = tween(
//            durationMillis = when (animationPhase) {
//                0 -> 800
//                1 -> 0
//                2 -> 600
//                else -> 0
//            },
//            easing = FastOutSlowInEasing
//        ),
//        label = "logoScale"
//    )
//
//    val textAlpha by animateFloatAsState(
//        targetValue = when (animationPhase) {
//            0 -> 0f
//            1 -> 1f
//            2 -> 0f
//            else -> 0f
//        },
//        animationSpec = tween(
//            durationMillis = when (animationPhase) {
//                0 -> 1000  // Slower fade in for text
//                1 -> 0
//                2 -> 500   // Faster fade out
//                else -> 0
//            },
//            delayMillis = when (animationPhase) {
//                0 -> 400   // Delay text appearance
//                else -> 0
//            }
//        ),
//        label = "textAlpha"
//    )
//
//    val backgroundBrightness by animateFloatAsState(
//        targetValue = when (animationPhase) {
//            0 -> 1f
//            1 -> 1f
//            2 -> 0f  // Fade to black
//            else -> 0f
//        },
//        animationSpec = tween(
//            durationMillis = when (animationPhase) {
//                2 -> 600
//                else -> 0
//            }
//        ),
//        label = "backgroundBrightness"
//    )
//
//    var showSplash by remember { mutableStateOf(true) }
//
//    // Animation timing
//    LaunchedEffect(Unit) {
//        animationPhase = 1  // Start fade in
//        delay(2000)         // Hold for 2 seconds
//        animationPhase = 2  // Start fade out
//        delay(800)          // Wait for fade out to complete
//        showSplash = false  // Switch to main content
//    }
//
//    if (showSplash) {
//
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black) // Solid black base
//                .background(
//                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
//                        colors = listOf(
//                            Color(0xFF2D1B1B).copy(alpha = backgroundBrightness),
//                            Color(0xFF1A0F0F).copy(alpha = backgroundBrightness)
//                        )
//                    )
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                // Animated logo
//                Image(
//                    painter = painterResource(id = R.drawable.deadhandstudio),
//                    contentDescription = "DeadHand Studios Logo",
//                    modifier = Modifier
//                        .size(200.dp)
//                        .padding(bottom = 32.dp)
//                        .graphicsLayer(
//                            alpha = logoAlpha,
//                            scaleX = logoScale,
//                            scaleY = logoScale,
//                            rotationZ = (logoScale - 1f) * 10f // Slight rotation effect
//                        )
//                )
//
//                // Animated text
//                Text(
//                    text = "DeadHandStudios",
//                    fontSize = 24.sp,
//                    fontWeight = FontWeight.Light,
//                    color = Color(0xFFE5D5C8),
//                    textAlign = TextAlign.Center,
//                    letterSpacing = 2.sp,
//                    modifier = Modifier
//                        .graphicsLayer(
//                            alpha = textAlpha,
//                            translationY = (1f - textAlpha) * 20f // Slide up effect
//                        )
//                )
//            }
//
//        }
//    }
//}
