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
            RotatingSpiralWithMinute()
        }
    }
}

private val minuteFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val hourFormatter = DateTimeFormatter.ofPattern("H")
private const val TAG = "SpiralDebug"

// History display parameters
private const val MAX_HISTORY_DEPTH = 512 // Current + 'x' previous hours
private const val PATH_ANGLE_STEP_BACK_PER_ITEM = PI.toFloat() / 24f // Angular distance on spiral's path between hours
private const val TEXT_INSET_FACTOR = 0.8f // Factor for how much to inset text from exact radius

private val FONT_SIZES_HISTORY = listOf(32f, 26f, 20f, 14f) // Descending font sizes
private const val CURRENT_TIME_FONT_SIZE = 50f // Font size for the current time (index 0)
private const val HISTORY_START_FONT_SIZE = 24f // Font size for the current time (index 0)
private const val HISTORY_END_FONT_SIZE = 1f   // Minimum font size for the oldest visible items

private const val CURRENT_TIME_ALPHA = 2.0f       // Current time is fully opaque
private const val HISTORY_START_ALPHA = 0.85f     // Alpha for the *first* historical item (index 1)
private const val HISTORY_END_ALPHA = 0.005f       // Minimum alpha for the oldest items (almost transparent)

/**
 * Calculates the rotation angle for the spiral's tip to point at the given minute
 * on a standard clock face.
 *
 * @param minute The minute of the hour (0-59).
 * @return The angle in radians.
 */
fun calculateAngleForMinute(minute: Int): Float {
    val angle = (minute * PI.toFloat() / 30f) - (PI.toFloat() / 2f)
    return angle
}

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
fun RotatingSpiralWithMinute() {
    val spiralColor = MaterialTheme.colors.primary.copy(alpha = 0.1f) // Keep semi-transparent spiral
    val currentItemTextColor = MaterialTheme.colors.onBackground
    val previousItemTextColor = MaterialTheme.colors.primary.copy(alpha = 0.7f)

    val coils = 3f
    val spiralOwnAngleAtEnd = remember(coils) { coils * 2f * PI.toFloat() }
    val maxDisplayableHistoryItems = remember(spiralOwnAngleAtEnd, PATH_ANGLE_STEP_BACK_PER_ITEM) {
        (spiralOwnAngleAtEnd / PATH_ANGLE_STEP_BACK_PER_ITEM).toInt().coerceAtLeast(1)
    }

    // --- State variables ---
    var spiralBaseRotationForDrawing by remember {
        val initialTargetDialAngle = calculateAngleForMinute(LocalTime.now().minute)
        mutableFloatStateOf(initialTargetDialAngle - spiralOwnAngleAtEnd)
    }
    var currentTargetDialAngle by remember {
        mutableFloatStateOf(calculateAngleForMinute(LocalTime.now().minute))
    }
    var timeTextHistory by remember { // Renamed from hourTextHistory
        mutableStateOf(
            List(MAX_HISTORY_DEPTH) { index ->
                LocalTime.now().minusMinutes(index.toLong()).format(minuteFormatter) // Use new formatter and minusMinutes
            }
        )
    }
    var lastProcessedMinute by remember { // Renamed from lastProcessedHour
        mutableIntStateOf(LocalTime.now().minute)
    }

    // --- LaunchedEffect for MINUTE updates ---
    LaunchedEffect(Unit) {
        Log.d(TAG, "Minute update effect started. Initial minute: ${LocalTime.now().minute}")
        // Initialize states correctly for the very first run
        val initialTime = LocalTime.now()
        lastProcessedMinute = initialTime.minute
        val initialHistory = List(MAX_HISTORY_DEPTH) { index ->
            initialTime.minusMinutes(index.toLong()).format(minuteFormatter)
        }
        if (timeTextHistory != initialHistory) timeTextHistory = initialHistory

        val initialDialAngle = calculateAngleForMinute(initialTime.minute)
        if (currentTargetDialAngle != initialDialAngle) currentTargetDialAngle = initialDialAngle

        val initialBaseRotation = initialDialAngle - spiralOwnAngleAtEnd
        if (spiralBaseRotationForDrawing != initialBaseRotation) spiralBaseRotationForDrawing = initialBaseRotation


        while (true) {
            val now = LocalTime.now()
            val currentActualMinute = now.minute

            if (currentActualMinute != lastProcessedMinute) {
                Log.d(TAG, "Minute changed from $lastProcessedMinute to $currentActualMinute.")
                val newTimeText = now.format(minuteFormatter)

                val newHistory = MutableList(MAX_HISTORY_DEPTH) { "" }
                for (i in MAX_HISTORY_DEPTH - 1 downTo 1) {
                    newHistory[i] = timeTextHistory[i - 1]
                }
                newHistory[0] = newTimeText
                timeTextHistory = newHistory.toList() // Ensure immutable list for state

                currentTargetDialAngle = calculateAngleForMinute(currentActualMinute)
                spiralBaseRotationForDrawing = currentTargetDialAngle - spiralOwnAngleAtEnd
                lastProcessedMinute = currentActualMinute
                Log.d(TAG, "History: $timeTextHistory, TargetDial: $currentTargetDialAngle")
            } else {
                // Ensure current text in history is accurate if app resumes mid-minute
                val expectedCurrentText = now.format(minuteFormatter)
                if (timeTextHistory.isNotEmpty() && timeTextHistory[0] != expectedCurrentText) {
                    timeTextHistory = timeTextHistory.toMutableList().also { it[0] = expectedCurrentText }.toList()
                }
            }

            // Calculate delay until the start of the next minute
            val nextMinuteTime = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
            var delayMillis = ChronoUnit.MILLIS.between(now, nextMinuteTime)

            if (delayMillis <= 0) { // If we are somehow past the next minute mark
                delayMillis = 60000L - (now.second * 1000L + now.nano / 1_000_000L) // ms until next minute from now
                if (delayMillis <=0) delayMillis = 1000L // Safety: check every second if calculation is off
            }
            // Ensure a minimum positive delay if very close to reduce rapid checks
            if (delayMillis < 500L) delayMillis = 500L


            Log.d(TAG, "Next minute check/update in $delayMillis ms (target: $nextMinuteTime)")
            delay(delayMillis)
        }
    }

    val basePreviousItemTextColor = MaterialTheme.colors.primary // Opaque base for history

    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        Log.d(TAG, "Canvas recomposing/redrawing. Rotation: $spiralBaseRotationForDrawing")
        val path = Path()
        val canvasCenter = this.center
        val maxRadius = size.minDimension / 2 * 0.8f
        val angleStep = 0.1f
        var pAngle = 0f
        var cRadius = 0f

        // 1. Draw the spiral path first
        path.moveTo(canvasCenter.x, canvasCenter.y)
        while (cRadius < maxRadius) {
            cRadius = (pAngle / (2 * PI.toFloat())) * (maxRadius / coils)
            if (cRadius > maxRadius) cRadius = maxRadius
            val effectiveAngle = pAngle + spiralBaseRotationForDrawing
            val x = canvasCenter.x + cRadius * cos(effectiveAngle)
            val y = canvasCenter.y + cRadius * sin(effectiveAngle)
            path.lineTo(x, y)
            pAngle += angleStep
        }
        drawPath(path = path, color = spiralColor, style = Stroke(width = 4f))

        Log.d(TAG, "CANVAS: timeTextHistory = $timeTextHistory")

        // 2. Draw HISTORICAL items first (index 1 up to MAX_HISTORY_DEPTH - 1)
        timeTextHistory.forEachIndexed { index, timeStr ->
            if (index == 0) return@forEachIndexed // Skip current time in this loop, draw it later

            Log.d(TAG, "CANVAS LOOP (History): Index $index, Text '$timeStr'")
            if (timeStr.isBlank()) { /* ... */ return@forEachIndexed }

            val currentPathAngleOnSpiral = spiralOwnAngleAtEnd - (index * PATH_ANGLE_STEP_BACK_PER_ITEM)
            if (currentPathAngleOnSpiral < 0.01f) { /* ... */ return@forEachIndexed } // No need for && index > 0 here

            // Font Size for history
            val historyItemCountForTaper = (maxDisplayableHistoryItems - 1).coerceAtLeast(1)
            val progression = ((index - 1).toFloat() / historyItemCountForTaper.toFloat()).coerceIn(0f, 1f)
            val calculatedSize = HISTORY_START_FONT_SIZE - (progression * (HISTORY_START_FONT_SIZE - HISTORY_END_FONT_SIZE))
            val currentItemFontSize = calculatedSize.coerceAtLeast(HISTORY_END_FONT_SIZE)
            textPaint.textSize = currentItemFontSize

            // Alpha for history
            val calculatedAlpha = HISTORY_START_ALPHA - (progression * (HISTORY_START_ALPHA - HISTORY_END_ALPHA))
            val currentItemAlpha = calculatedAlpha.coerceAtLeast(HISTORY_END_ALPHA)

            val colorWithAlpha = basePreviousItemTextColor.copy(alpha = currentItemAlpha)
            textPaint.color = colorWithAlpha.toArgb()

            val radiusToUse = (currentPathAngleOnSpiral / (2 * PI.toFloat())) * (maxRadius / coils)
            if (radiusToUse < currentItemFontSize / 2f) { /* ... */ return@forEachIndexed }

            val screenAngleToUse = currentPathAngleOnSpiral + spiralBaseRotationForDrawing

            val textX = canvasCenter.x + radiusToUse * cos(screenAngleToUse)
            val textY = canvasCenter.y + radiusToUse * sin(screenAngleToUse)
            val centeredTextY = textY + textPaint.textSize / 3f

            Log.d(TAG, "Drawing History: '$timeStr' (idx $index) R:${String.format("%.1f",radiusToUse)} SA:${String.format("%.2f",screenAngleToUse)} Font:${textPaint.textSize} Alpha:${String.format("%.2f",currentItemAlpha)}")
            drawContext.canvas.nativeCanvas.drawText(timeStr, textX, centeredTextY, textPaint)
        }

        // 3. Draw CURRENT time last (index 0) to ensure it's on top
        if (timeTextHistory.isNotEmpty()) {
            val currentTimeStr = timeTextHistory[0]
            if (!currentTimeStr.isBlank()) {
                Log.d(TAG, "CANVAS (Current Time): Text '$currentTimeStr'")

                textPaint.textSize = CURRENT_TIME_FONT_SIZE
                val colorWithAlpha = currentItemTextColor.copy(alpha = CURRENT_TIME_ALPHA) // currentItemTextColor is base
                textPaint.color = colorWithAlpha.toArgb()

                val radiusToUse = maxRadius - (CURRENT_TIME_FONT_SIZE * TEXT_INSET_FACTOR / 2f)
                // screenAngleToUse for current time is currentTargetDialAngle
                val screenAngleToUse = currentTargetDialAngle

                val textX = canvasCenter.x + radiusToUse * cos(screenAngleToUse)
                val textY = canvasCenter.y + radiusToUse * sin(screenAngleToUse)
                val centeredTextY = textY + textPaint.textSize / 3f

                Log.d(TAG, "Drawing Current: '$currentTimeStr' (idx 0) R:${String.format("%.1f",radiusToUse)} SA:${String.format("%.2f",screenAngleToUse)} Font:${textPaint.textSize} Alpha:${String.format("%.2f",CURRENT_TIME_ALPHA)}")
                drawContext.canvas.nativeCanvas.drawText(currentTimeStr, textX, centeredTextY, textPaint)
            }
        }
        Log.d(TAG, "CANVAS: Finished drawing all text.")
    }
}

@Composable
fun RotatingSpiralWithHour() {
    val spiralColor = MaterialTheme.colors.primary
    val currentHourTextColor = MaterialTheme.colors.onBackground
    val previousHourTextColor = MaterialTheme.colors.primary.copy(alpha = 0.7f)

    val coils = 5f
    val spiralOwnAngleAtEnd = remember(coils) { coils * 2f * PI.toFloat() }

    var spiralBaseRotationForDrawing by remember {
        val initialTargetDialAngle = calculateAngleForHour(LocalTime.now().hour)
        mutableFloatStateOf(initialTargetDialAngle - spiralOwnAngleAtEnd)
    }
    var currentTargetDialAngle by remember { // For the leading tip (current hour)
        mutableFloatStateOf(calculateAngleForHour(LocalTime.now().hour))
    }
    var hourTextHistory by remember {
        mutableStateOf(
            List(MAX_HISTORY_DEPTH) { index ->
                LocalTime.now().minusHours(index.toLong()).format(hourFormatter)
            }
        )
    }
    var lastProcessedHour by remember {
        mutableIntStateOf(LocalTime.now().hour)
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Hourly update effect started. Initial hour: ${LocalTime.now().hour}")

        while (true) {
            val now = LocalTime.now()
            val currentHour = now.hour

            if (currentHour != lastProcessedHour) {
                Log.d(TAG, "Rotating to new hour: $currentHour")
                val newHour = now.format(hourFormatter)

                val newHistory = MutableList(MAX_HISTORY_DEPTH) { "" }
                for (i in MAX_HISTORY_DEPTH - 1 downTo 1) {
                    newHistory[i] = hourTextHistory[i - 1]
                }
                newHistory[0] = newHour
                hourTextHistory = newHistory.toList()

                currentTargetDialAngle = calculateAngleForHour(currentHour)
                spiralBaseRotationForDrawing = currentTargetDialAngle - spiralOwnAngleAtEnd
                lastProcessedHour = currentHour
                Log.d(TAG, "History: $hourTextHistory, TargetDial: $currentTargetDialAngle")
            } else {
                val expectedCurrentText = now.format(hourFormatter)
                if (hourTextHistory.isNotEmpty() && hourTextHistory[0] != expectedCurrentText) {
                    hourTextHistory = hourTextHistory.toMutableList().also {
                        it[0] = expectedCurrentText
                    }.toList()
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
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        Log.d(TAG, "Canvas recomposing/redrawing. Rotation: $spiralBaseRotationForDrawing")
        val path = Path()
        val canvasCenter = this.center
        val maxRadius = size.minDimension / 2 * 0.8f
        val angleStep = 0.1f
        var pathAngle = 0f
        var currentRadius = 0f

        path.moveTo(canvasCenter.x, canvasCenter.y)

        while (currentRadius < maxRadius) {
            currentRadius = (pathAngle / (2 * PI.toFloat())) * (maxRadius / coils)
            if (currentRadius > maxRadius) currentRadius = maxRadius
            val effectiveAngle = pathAngle + spiralBaseRotationForDrawing
            val x = canvasCenter.x + currentRadius * cos(effectiveAngle)
            val y = canvasCenter.y + currentRadius * sin(effectiveAngle)
            path.lineTo(x, y)
            pathAngle += angleStep
        }

        drawPath(path = path, color = spiralColor.copy(alpha = 0.2f), style = Stroke(width = 12f))

        hourTextHistory.forEachIndexed { index, hourStr ->
            if (hourStr.isBlank()) return@forEachIndexed // Skip if no text for this slot

            val currentPathAngleOnSpiral =
                spiralOwnAngleAtEnd - (index * PATH_ANGLE_STEP_BACK_PER_ITEM)

            if (currentPathAngleOnSpiral < 0.01f && index > 0) return@forEachIndexed // Stop if we're at/before spiral origin for past items

            val radiusToUse = if (index == 0) { // Current hour at/near maxRadius
                maxRadius - (FONT_SIZES_HISTORY[0] * TEXT_INSET_FACTOR / 2f) // Inset from edge
            } else {
                (currentPathAngleOnSpiral / (2 * PI.toFloat())) * (maxRadius / coils)
            }

            val currentItemFontSize = FONT_SIZES_HISTORY.getOrElse(index) { FONT_SIZES_HISTORY.last() }
            textPaint.textSize = currentItemFontSize // Set font size for current item
            textPaint.color = if (index == 0) currentHourTextColor.toArgb() else previousHourTextColor.toArgb()

            if (radiusToUse < currentItemFontSize / 2f && index > 0) return@forEachIndexed // Too small to draw past items

            val screenAngleToUse = if (index == 0) { // Current hour uses direct target dial angle
                currentTargetDialAngle
            } else {
                currentPathAngleOnSpiral + spiralBaseRotationForDrawing
            }

            val textX = canvasCenter.x + radiusToUse * cos(screenAngleToUse)
            val textY = canvasCenter.y + radiusToUse * sin(screenAngleToUse)

            val centeredTextTipY = textY + textPaint.textSize / 3f

            Log.d(TAG, "Attempting to draw single text: '$currentHourTextColor' at ($textX, $centeredTextTipY) with radius ${radiusToUse}, angle $currentPathAngleOnSpiral")

            drawContext.canvas.nativeCanvas.drawText(
                hourStr, textX, centeredTextTipY, textPaint
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}