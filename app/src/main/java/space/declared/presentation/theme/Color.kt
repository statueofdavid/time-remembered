package space.declared.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

// Define your app's specific colors here.
// These are just example colors; you can customize them.
val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)
val Red400 = Color(0xFFCF6679)

// This is your Wear OS Material color palette.
// It maps your specific colors to the Material theme's color roles.
val wearColorPalette: Colors = Colors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200,
    secondaryVariant = Teal200, // Often same as secondary for Wear
    error = Red400,
    background = Color.Black,   // Common for Wear OS watch faces
    surface = Color(0xFF202124), // A dark grey, common for surfaces on dark background
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onError = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray // For less prominent text/icons on surfaces
)