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

@Composable
fun RotatingSpiralWithHour() {
    val spiralColor = MaterialTheme.colors.primary
    val textColor = MaterialTheme.colors.onBackground

    var currentRotationAngle by remember { mutableStateOf(0f) }
    var currentHourText by remember { mutableStateOf(LocalTime.now().format(hourFormatter)) }

    // --- Simplified Rotation Logic ---
    LaunchedEffect(Unit) {
        Log.d(TAG, "Simplified rotation effect started.")
        var frameCount = 0
        while (true) {
            delay(16) // Aim for roughly 60 FPS, ~16ms per frame
            currentRotationAngle = (currentRotationAngle + 0.01f) % (2f * PI.toFloat())
            // Optional: Log every few frames to avoid spamming Logcat too much
            if (frameCount % 60 == 0) { // Log once per second if 60fps
                Log.d(TAG, "Simplified currentRotationAngle: $currentRotationAngle")
            }
            frameCount++
        }
    }
    // --- End of Simplified Rotation Logic ---

    /*
    // Original withFrameNanos rotation logic (commented out for test)
    LaunchedEffect(Unit) {
        Log.d(TAG, "withFrameNanos rotation effect started.")
        val rotationStartTime = withFrameNanos { it }
        Log.d(TAG, "rotationStartTime: $rotationStartTime")
        var frameCount = 0
        while (true) {
            withFrameNanos { frameTimeNanos ->
                val elapsedTimeNanos = frameTimeNanos - rotationStartTime
                val rotationSpeedFactor = 10_000_000_000f
                currentRotationAngle = (elapsedTimeNanos.toFloat() / rotationSpeedFactor) * (2f * PI.toFloat())
                currentRotationAngle %= (2f * PI.toFloat())
                if (frameCount % 60 == 0) { // Log once per second
                    Log.d(TAG, "withFrameNanos elapsedTime: $elapsedTimeNanos, angle: $currentRotationAngle")
                }
                frameCount++
            }
        }
    }
    */


    // LaunchedEffect to update the hour text every quarter hour (same as before)
    LaunchedEffect(Unit) {
        Log.d(TAG, "Quarter-hour text update effect started.")
        while (true) {
            val now = LocalTime.now()
            val newHourText = now.format(hourFormatter)
            if (newHourText != currentHourText) {
                Log.d(TAG, "Updating hour text from $currentHourText to $newHourText")
                currentHourText = newHourText
            }


            val currentMinute = now.minute
            val currentSecond = now.second
            val currentNano = now.nano

            val minutesToNextQuarterHour: Int = when {
                currentMinute < 15 -> 15 - currentMinute
                currentMinute < 30 -> 30 - currentMinute
                currentMinute < 45 -> 45 - currentMinute
                else -> (60 - currentMinute)
            }

            var delayMillis = minutesToNextQuarterHour * 60L * 1000L
            delayMillis -= currentSecond * 1000L
            delayMillis -= currentNano / 1_000_000L

            if (delayMillis <= 0) {
                if (currentMinute % 15 == 0 && currentSecond == 0 && currentNano == 0) {
                    delayMillis = 15 * 60 * 1000L
                } else {
                    val minutesPastLastQuarter = currentMinute % 15
                    val minutesToWaitFor = 15 - minutesPastLastQuarter
                    var nextDelayMillis = minutesToWaitFor * 60L * 1000L
                    nextDelayMillis -= currentSecond * 1000L // Re-subtract current seconds for the adjusted wait
                    nextDelayMillis -= currentNano / 1_000_000L // Re-subtract current nanos
                    if (nextDelayMillis <= 0) { // If still non-positive, means we are at the very end of a 15-min block
                        nextDelayMillis = 15 * 60 * 1000L // wait for the next full 15 min slot
                    }
                    delayMillis = nextDelayMillis
                }
            }
            Log.d(TAG, "Text update: Next delayMillis: $delayMillis for $minutesToNextQuarterHour min (approx)")
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

        val textX = canvasCenter.x
        val textY = canvasCenter.y + textPaint.textSize / 3f
        drawContext.canvas.nativeCanvas.drawText(currentHourText, textX, textY, textPaint)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}