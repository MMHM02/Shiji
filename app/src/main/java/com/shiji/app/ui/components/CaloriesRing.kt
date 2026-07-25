package com.shiji.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.shiji.app.ui.theme.Calories
import com.shiji.app.ui.theme.BrandGreen
import com.shiji.app.ui.theme.Error
import com.shiji.app.ui.theme.Warning

@Composable
fun CaloriesRing(
    intake: Float,
    target: Float,
    size: Dp = 240.dp,
    strokeWidth: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val ratio = (intake / target).coerceIn(0f, 1.2f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceAtMost(1f),
        animationSpec = tween(durationMillis = 800),
        label = "ring"
    )

    val ringColor = when {
        ratio >= 1f -> Error
        ratio >= 0.9f -> Warning
        else -> BrandGreen
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val sweepAngle = animatedRatio * 360f

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val outerSize = this.size.minDimension
            val stroke = strokeWidth.toPx()
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(outerSize - stroke, outerSize - stroke)

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text(
                text = "${intake.toInt().toLocaleString()}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.015).em
            )
            androidx.compose.material3.Text(
                text = "/ ${target.toInt().toLocaleString()} kcal",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            androidx.compose.material3.Text(
                text = "${(ratio * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ringColor
            )
        }
    }
}

private fun Int.toLocaleString(): String = buildString {
    val s = this@toLocaleString.toString()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) append(',')
        append(s[i])
    }
}
