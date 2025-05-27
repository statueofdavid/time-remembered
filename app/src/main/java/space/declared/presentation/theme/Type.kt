package space.declared.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Typography

// This is your Wear OS Material typography definition.
// You can customize font families, weights, and sizes for each text style.
val Typography = Typography(
    title1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    title2 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    title3 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // Adjusted from Bold for more hierarchy
        fontSize = 18.sp
    ),
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body2 = TextStyle( // Added for more flexibility
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold, // Buttons often use a bolder weight
        fontSize = 15.sp,
        letterSpacing = 0.5.sp // Added for button text clarity
    ),
    caption1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    caption2 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // Adjusted for slightly more emphasis
        fontSize = 12.sp
    ),
    caption3 = TextStyle( // Added for more granular control
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
    // You can also define other styles like display1, display2, display3 if needed
    // display1 = TextStyle( ... ),
    // display2 = TextStyle( ... ),
    // display3 = TextStyle( ... ),
)