package com.misterdownloadmanager.presentation.ui.component

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.presentation.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DownloadSpeedometerDashboard(
    totalSpeedBytesPerSec: Long,
    activeDownloadsCount: Int,
    queuedDownloadsCount: Int,
    completedDownloadsCount: Int,
    totalDownloadedBytes: Long,
    modifier: Modifier = Modifier
) {
    // Dynamic Speed Unit and Value Formatting
    val (speedFormatted, unit) = remember(totalSpeedBytesPerSec) {
        when {
            totalSpeedBytesPerSec >= 1024L * 1024L * 1024L -> {
                String.format("%.2f", totalSpeedBytesPerSec.toDouble() / (1024.0 * 1024.0 * 1024.0)) to "GB/s"
            }
            totalSpeedBytesPerSec >= 1024L * 1024L -> {
                String.format("%.2f", totalSpeedBytesPerSec.toDouble() / (1024.0 * 1024.0)) to "MB/s"
            }
            totalSpeedBytesPerSec >= 1024L -> {
                String.format("%.1f", totalSpeedBytesPerSec.toDouble() / 1024.0) to "KB/s"
            }
            else -> {
                "$totalSpeedBytesPerSec" to "B/s"
            }
        }
    }

    // Adaptive Maximum Scale in bytes/sec
    val maxScaleBytes = remember(totalSpeedBytesPerSec) {
        val currentMb = totalSpeedBytesPerSec.toDouble() / (1024.0 * 1024.0)
        when {
            currentMb > 50.0 -> (currentMb * 1.3 * 1024.0 * 1024.0).toLong()
            currentMb > 10.0 -> 50L * 1024L * 1024L
            currentMb > 2.0 -> 10L * 1024L * 1024L
            currentMb > 0.5 -> 2L * 1024L * 1024L
            else -> 1024L * 1024L
        }
    }

    val targetFraction = remember(totalSpeedBytesPerSec, maxScaleBytes) {
        if (maxScaleBytes > 0 && totalSpeedBytesPerSec > 0) {
            (totalSpeedBytesPerSec.toFloat() / maxScaleBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val animatedFraction = remember { Animatable(0f) }
    LaunchedEffect(targetFraction) {
        animatedFraction.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val surfaceBg = MaterialTheme.colorScheme.surface
    val surfaceBorder = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = if (MaterialTheme.colorScheme.background.red < 0.2f) DarkAccentCyan else LightAccentCyan
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceBg, RoundedCornerShape(16.dp))
            .border(1.dp, surfaceBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Precision Semicircular Speedometer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val centerX = canvasWidth / 2f
                val centerY = canvasHeight * 0.88f
                val arcRadius = (canvasWidth.coerceAtMost(canvasHeight * 2f)) * 0.44f

                val startAngle = 180f
                val sweepAngle = 180f
                val trackStrokeWidth = 14f

                // Inactive track arc
                drawArc(
                    color = surfaceBorder.copy(alpha = 0.7f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                    size = Size(arcRadius * 2f, arcRadius * 2f),
                    style = Stroke(width = trackStrokeWidth, cap = StrokeCap.Round)
                )

                // Active speed gradient arc
                val currentSweep = (sweepAngle * animatedFraction.value).coerceIn(0f, sweepAngle)
                if (currentSweep > 0f) {
                    val gradient = Brush.sweepGradient(
                        colors = listOf(primaryColor, accentColor),
                        center = Offset(centerX, centerY)
                    )
                    drawArc(
                        brush = gradient,
                        startAngle = startAngle,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                        size = Size(arcRadius * 2f, arcRadius * 2f),
                        style = Stroke(width = trackStrokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Precision Tick Marks
                val totalTicks = 20
                for (i in 0..totalTicks) {
                    val tickFraction = i.toFloat() / totalTicks
                    val tickAngleDeg = startAngle + (sweepAngle * tickFraction)
                    val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())

                    val isMajor = i % 5 == 0
                    val tickLength = if (isMajor) 14f else 7f
                    val innerR = arcRadius - trackStrokeWidth / 2f - 12f
                    val outerR = innerR - tickLength

                    val startX = (centerX + innerR * cos(tickAngleRad)).toFloat()
                    val startY = (centerY + innerR * sin(tickAngleRad)).toFloat()
                    val endX = (centerX + outerR * cos(tickAngleRad)).toFloat()
                    val endY = (centerY + outerR * sin(tickAngleRad)).toFloat()

                    val tickColor = if (tickFraction <= animatedFraction.value) {
                        accentColor.copy(alpha = 0.9f)
                    } else {
                        surfaceBorder.copy(alpha = 0.6f)
                    }

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isMajor) 3f else 1.5f,
                        cap = StrokeCap.Round
                    )
                }

                // Needle indicator
                val needleAngleDeg = startAngle + (sweepAngle * animatedFraction.value)
                val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
                val needleLength = arcRadius - 28f

                val needleEndX = (centerX + needleLength * cos(needleAngleRad)).toFloat()
                val needleEndY = (centerY + needleLength * sin(needleAngleRad)).toFloat()

                drawLine(
                    color = accentColor,
                    start = Offset(centerX, centerY),
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                // Needle pivot center pin
                drawCircle(
                    color = textPrimary,
                    radius = 9f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 5f,
                    center = Offset(centerX, centerY)
                )
            }

            // Digital Speed Readout in Center of Dial
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = speedFormatted,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Precision Metric Pills / Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                title = stringResource(R.string.filter_active),
                value = "$activeDownloadsCount",
                modifier = Modifier.weight(1f),
                highlightColor = StateDownloading
            )
            MetricTile(
                title = stringResource(R.string.filter_queued),
                value = "$queuedDownloadsCount",
                modifier = Modifier.weight(1f),
                highlightColor = StateWaiting
            )
            MetricTile(
                title = stringResource(R.string.filter_completed),
                value = "$completedDownloadsCount",
                modifier = Modifier.weight(1f),
                highlightColor = StateDone
            )
            val totalFormatted = remember(totalDownloadedBytes) {
                formatByteSize(totalDownloadedBytes)
            }
            MetricTile(
                title = stringResource(R.string.dashboard_total_data_label),
                value = totalFormatted,
                modifier = Modifier.weight(1f),
                highlightColor = DarkAccentCyan
            )
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(highlightColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .border(1.dp, highlightColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = highlightColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

fun formatByteSize(bytes: Long): String {
    val mb = bytes.toDouble() / 1_000_000.0
    return String.format(java.util.Locale.US, "%.2f MB", mb)
}
