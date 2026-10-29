package com.unh.fitnesstracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Simple heart-rate line. Reused on S18 now and on S19 in Sprint 5.
 * [samples] is bpm in time order. Empty until Health Connect / the watch arrives (Sprint 9–10).
 */
@Composable
fun HeartRateChart(
    samples: List<Int>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (samples.size < 2) {
            Text("No heart-rate data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Box
        }
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val min = (samples.minOrNull() ?: return@Canvas).toFloat()
            val max = (samples.maxOrNull() ?: return@Canvas).toFloat().coerceAtLeast(min + 1f)
            val range = max - min
            fun x(i: Int) = size.width * i / (samples.lastIndex).toFloat()
            fun y(bpm: Int) = size.height - (bpm - min) / range * size.height

            for (step in 0..2) {
                val gy = size.height * step / 2f
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            }
            val path = Path()
            samples.forEachIndexed { i, bpm ->
                val point = Offset(x(i), y(bpm))
                if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
    }
}
