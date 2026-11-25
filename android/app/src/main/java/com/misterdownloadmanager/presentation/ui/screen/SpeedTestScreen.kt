package com.misterdownloadmanager.presentation.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.domain.speedtest.SpeedTestManager
import com.misterdownloadmanager.domain.speedtest.SpeedTestPhase
import com.misterdownloadmanager.presentation.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedTestScreen(
    speedTestManager: SpeedTestManager,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val testState by speedTestManager.testState.collectAsState()

    val isRunning = testState.phase == SpeedTestPhase.PING ||
            testState.phase == SpeedTestPhase.DOWNLOAD ||
            testState.phase == SpeedTestPhase.UPLOAD

    // Needle scale: map 0 .. 150 Mbps to 0.0 .. 1.0 fraction
    val targetSpeed = when (testState.phase) {
        SpeedTestPhase.PING -> 0.0
        SpeedTestPhase.DOWNLOAD, SpeedTestPhase.UPLOAD -> testState.currentSpeedMbps
        SpeedTestPhase.COMPLETE -> testState.downloadSpeedMbps
        else -> 0.0
    }

    val speedFraction = (targetSpeed / 150.0).coerceIn(0.0, 1.0).toFloat()
    val animatedFraction by animateFloatAsState(
        targetValue = speedFraction,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "SpeedGaugeFraction"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status phase badge
        val phaseLabel = when (testState.phase) {
            SpeedTestPhase.IDLE -> stringResource(R.string.speed_test_status_idle)
            SpeedTestPhase.PING -> stringResource(R.string.speed_test_status_ping)
            SpeedTestPhase.DOWNLOAD -> stringResource(R.string.speed_test_status_download)
            SpeedTestPhase.UPLOAD -> stringResource(R.string.speed_test_status_upload)
            SpeedTestPhase.COMPLETE -> stringResource(R.string.speed_test_status_complete)
            SpeedTestPhase.ERROR -> testState.errorMessage ?: "Error"
        }

        val phaseColor = when (testState.phase) {
            SpeedTestPhase.IDLE -> MaterialTheme.colorScheme.primary
            SpeedTestPhase.PING -> DarkAccentLime
            SpeedTestPhase.DOWNLOAD -> StateDownloading
            SpeedTestPhase.UPLOAD -> DarkAccentCyan
            SpeedTestPhase.COMPLETE -> StateDownloading
            SpeedTestPhase.ERROR -> StateFailed
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(phaseColor.copy(alpha = 0.12f))
                .border(1.dp, phaseColor.copy(alpha = 0.35f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = phaseColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Precision Speed Gauge Dial
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val surfaceBorder = MaterialTheme.colorScheme.outline
                val primaryColor = MaterialTheme.colorScheme.primary
                val textPrimary = MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val centerX = canvasWidth / 2f
                        val centerY = canvasHeight * 0.88f
                        val arcRadius = (canvasWidth.coerceAtMost(canvasHeight * 1.8f)) * 0.44f

                        val startAngle = 180f
                        val sweepAngle = 180f
                        val trackStrokeWidth = 14f

                        // Inactive track
                        drawArc(
                            color = surfaceBorder.copy(alpha = 0.6f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                            size = Size(arcRadius * 2f, arcRadius * 2f),
                            style = Stroke(width = trackStrokeWidth, cap = StrokeCap.Round)
                        )

                        // Active gradient arc
                        val currentSweep = (sweepAngle * animatedFraction).coerceIn(0f, sweepAngle)
                        if (currentSweep > 0f) {
                            val gradient = Brush.sweepGradient(
                                colors = listOf(StateDownloading, DarkAccentCyan, LightPrimary),
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

                        // Precision tick marks
                        val totalTicks = 20
                        for (i in 0..totalTicks) {
                            val tickFraction = i.toFloat() / totalTicks
                            val tickAngleDeg = startAngle + (sweepAngle * tickFraction)
                            val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())

                            val isMajor = i % 5 == 0
                            val tickLength = if (isMajor) 14f else 7f
                            val innerR = arcRadius - trackStrokeWidth / 2f - 10f
                            val outerR = innerR - tickLength

                            val startX = (centerX + innerR * cos(tickAngleRad)).toFloat()
                            val startY = (centerY + innerR * sin(tickAngleRad)).toFloat()
                            val endX = (centerX + outerR * cos(tickAngleRad)).toFloat()
                            val endY = (centerY + outerR * sin(tickAngleRad)).toFloat()

                            val tickColor = if (tickFraction <= animatedFraction) {
                                StateDownloading.copy(alpha = 0.9f)
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
                        val needleAngleDeg = startAngle + (sweepAngle * animatedFraction)
                        val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
                        val needleLength = arcRadius - 26f

                        val needleEndX = (centerX + needleLength * cos(needleAngleRad)).toFloat()
                        val needleEndY = (centerY + needleLength * sin(needleAngleRad)).toFloat()

                        drawLine(
                            color = StateDownloading,
                            start = Offset(centerX, centerY),
                            end = Offset(needleEndX, needleEndY),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )

                        drawCircle(color = textPrimary, radius = 9f, center = Offset(centerX, centerY))
                        drawCircle(color = primaryColor, radius = 5f, center = Offset(centerX, centerY))
                    }

                    // Digital speed readout
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val displayedNumber = when (testState.phase) {
                            SpeedTestPhase.PING -> String.format("%.0f", testState.pingMs)
                            SpeedTestPhase.DOWNLOAD, SpeedTestPhase.UPLOAD -> String.format("%.1f", testState.currentSpeedMbps)
                            SpeedTestPhase.COMPLETE -> String.format("%.1f", testState.downloadSpeedMbps)
                            else -> "0.0"
                        }

                        val unitLabel = when (testState.phase) {
                            SpeedTestPhase.PING -> "ms"
                            else -> "Mbps"
                        }

                        Text(
                            text = displayedNumber,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = unitLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkAccentCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Linear test progress bar
                LinearProgressIndicator(
                    progress = { testState.currentProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = StateDownloading,
                    trackColor = surfaceBorder.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // New Speed Result Rectangle: MBps (Megabytes per second)
        // Positioned directly below the gauge and directly above the four information boxes
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StateDownloading.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Large, prominent numerical download speed in MBps
                Column {
                    val currentMb = when (testState.phase) {
                        SpeedTestPhase.DOWNLOAD -> testState.currentSpeedMBps
                        SpeedTestPhase.UPLOAD, SpeedTestPhase.COMPLETE -> testState.downloadSpeedMBps
                        else -> 0.0
                    }
                    val speedDisplay = if (currentMb > 0) String.format(java.util.Locale.US, "%.2f MBps", currentMb) else "-- MBps"

                    Text(
                        text = speedDisplay,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StateDownloading,
                        letterSpacing = (-0.3).sp
                    )
                }

                // Right Side: Short explanation of the measured download speed
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Actual Download Speed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Real data transfer rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 Key Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedMetricBox(
                icon = Icons.Default.NetworkPing,
                title = stringResource(R.string.speed_test_ping),
                value = if (testState.pingMs > 0) String.format("%.0f ms", testState.pingMs) else "--",
                color = StateWaiting,
                modifier = Modifier.weight(1f)
            )
            SpeedMetricBox(
                icon = Icons.Default.Timeline,
                title = stringResource(R.string.speed_test_jitter),
                value = if (testState.jitterMs > 0) String.format("%.1f ms", testState.jitterMs) else "--",
                color = DarkAccentLime,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedMetricBox(
                icon = Icons.Default.ArrowDownward,
                title = stringResource(R.string.speed_test_download),
                value = if (testState.downloadSpeedMbps > 0) String.format("%.1f Mbps", testState.downloadSpeedMbps) else "--",
                color = StateDownloading,
                modifier = Modifier.weight(1f)
            )
            SpeedMetricBox(
                icon = Icons.Default.ArrowUpward,
                title = stringResource(R.string.speed_test_upload),
                value = if (testState.uploadSpeedMbps > 0) String.format("%.1f Mbps", testState.uploadSpeedMbps) else "--",
                color = DarkAccentCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start / Cancel Button
        if (isRunning) {
            Button(
                onClick = { speedTestManager.stopTest() },
                colors = ButtonDefaults.buttonColors(containerColor = StateFailed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.speed_test_cancel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = { speedTestManager.startTest(coroutineScope) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (testState.phase == SpeedTestPhase.COMPLETE) {
                        stringResource(R.string.speed_test_retest)
                    } else {
                        stringResource(R.string.speed_test_start)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpeedMetricBox(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
