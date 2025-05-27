package space.declared.presentation

import android.graphics.Paint
import android.os.Bundle
import android.util.Log // For logging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// import androidx.compose.runtime.withFrameNanos // Temporarily unused for simpler rotation test
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.delay // Still needed for both effects
import java.time.LocalTime
import java.time.format.DateTimeFormatter
// import java.time.temporal.ChronoUnit // Not used in this simplified version
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

import space.declared.presentation.theme.TimeRememberedTheme
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    TimeRememberedTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            RotatingSpiralWithHour()
        }
    }
}

private val hourFormatter = DateTimeFormatter.ofPattern("H")
private const val TAG = "SpiralDebug" // For Logcat

/**
 * Calculates the rotation angle for the spiral's tip to point at the given hour
 * on a standard clock face.
 *
 * @param hour24 The hour in 24-hour format (0-23).
 * @return The angle in radians.
 */
fun calculateAngleForHour(hour24: Int): Float {
    val displayHour = when {
        hour24 == 0 || hour24 == 12 -> 12 // Midnight (0h) and Noon (12h) are '12' on the clock
        hour24 > 12 -> hour24 % 12
        else -> hour24
    }

    val angle = (displayHour * PI.toFloat() / 6f) - (PI.toFloat() / 2f)

    Log.d(TAG, "calculateAngleForHour: hour24=$hour24, displayHour=$displayHour, angleRad=$angle")
    return angle
}

@Composable
fun RotatingSpiralWithHour() {
    val spiralColor = MaterialTheme.colors.primary
    val textColor = MaterialTheme.colors.onBackground

    var currentRotationAngle by remember {
        mutableFloatStateOf(calculateAngleForHour(LocalTime.now().hour))
    }

    var currentHourText by remember {
        mutableStateOf(LocalTime.now().format(hourFormatter))
    }

    var lastRotatedHour by remember {
        mutableIntStateOf(LocalTime.now().hour)
    }

    // --- Simplified Rotation Logic ---
    LaunchedEffect(Unit) {
        Log.d(TAG, "Hourly update effect started. Initial hour: ${LocalTime.now().hour}")

        while (true) {
            val now = LocalTime.now()
            val currentHour = now.hour

            if (currentHour != lastRotatedHour) {
                Log.d(TAG, "Rotating to new hour: $currentHour")
                currentRotationAngle = calculateAngleForHour(currentHour)
                lastRotatedHour = currentHour
            } else {
                val updatedText = now.format(hourFormatter)
                if (updatedText != currentHourText) {
                    currentHourText = updatedText
                }
            }

            val nextHour = now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
            var delayMillis = ChronoUnit.MILLIS.between(now, nextHour)

            if (delayMillis <= 0) {
                delayMillis = 1000L
            }
            Log.d(TAG, "Next hourly check/update in $delayMillis ms (target: $nextHour)")
            delay(delayMillis)
        }
    }

    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 80f
            textAlign = Paint.Align.CENTER
        }
    }
    textPaint.color = textColor.toArgb()

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Log.d(TAG, "Canvas recomposing/redrawing. Rotation: $currentRotationAngle") // Can be very spammy
        val path = Path()
        val canvasCenter = this.center
        val maxRadius = size.minDimension / 2 * 0.8f
        val coils = 5f
        val angleStep = 0.1f
        var pathAngle = 0f
        var currentRadius = 0f

        path.moveTo(canvasCenter.x, canvasCenter.y)

        while (currentRadius < maxRadius) {
            currentRadius = (pathAngle / (2 * PI.toFloat())) * (maxRadius / coils)
            if (currentRadius > maxRadius) currentRadius = maxRadius
            val effectiveAngle = pathAngle + currentRotationAngle // Rotation applied here
            val x = canvasCenter.x + currentRadius * cos(effectiveAngle)
            val y = canvasCenter.y + currentRadius * sin(effectiveAngle)
            path.lineTo(x, y)
            pathAngle += angleStep
        }

        drawPath(path = path, color = spiralColor, style = Stroke(width = 4f))

        val tipTextRadius = maxRadius - textPaint.textSize * 0.08f
        val tipTextX = canvasCenter.x + tipTextRadius * cos(currentRotationAngle)
        val tipTextY = canvasCenter.y + tipTextRadius * sin(currentRotationAngle)

        val centeredTextTipY = tipTextY + textPaint.textSize / 3f

        drawContext.canvas.nativeCanvas.drawText(
            currentHourText,
            tipTextX,
            centeredTextTipY,
            textPaint
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}