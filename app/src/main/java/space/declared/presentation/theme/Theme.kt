package space.declared.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun TimeRememberedTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = wearColorPalette, // wearColorPalette would be defined in Color.kt usually
        typography = Typography,   // Typography would be defined in Type.kt usually
        // For latest versions, shapes may also be here if customized
        content = content
    )
}