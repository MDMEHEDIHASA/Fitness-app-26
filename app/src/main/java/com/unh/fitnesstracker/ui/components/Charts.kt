package com.unh.fitnesstracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class ChartPoint(val label: String, val value: Double)

@Composable
fun BarChart(
    points: List<ChartPoint>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    goal: Double? = null,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val goalColor = MaterialTheme.colorScheme.error
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (points.isEmpty() || points.all { it.value == 0.0 }) {
            Text("No data yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Box
        }
        val max = (points.maxOf { it.value }.coerceAtLeast(goal ?: 0.0)).coerceAtLeast(1.0)
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(points.size) {
                    detectTapGestures { offset ->
                        val w = size.width / points.size
                        val index = (offset.x / w).toInt().coerceIn(0, points.lastIndex)
                        onSelect(index)
                    }
                },
        ) {
            val slot = size.width / points.size
            val barWidth = slot * 0.55f
            points.forEachIndexed { i, point ->
                val h = (point.value / max).toFloat() * size.height
                val x = slot * i + (slot - barWidth) / 2
                drawRoundRect(
                    color = if (i == selectedIndex) selectedColor else barColor,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(8f, 8f),
                )
            }
            goal?.let {
                val y = size.height - (it / max).toFloat() * size.height
                drawLine(goalColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
            }
            drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        }
    }
}

@Composable
fun LineChart(
    points: List<ChartPoint>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    goal: Double? = null,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val goalColor = MaterialTheme.colorScheme.error
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (points.size < 2 || points.all { it.value == 0.0 }) {
            Text("No data yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Box
        }
        val max = (points.maxOf { it.value }.coerceAtLeast(goal ?: 0.0)).coerceAtLeast(1.0)
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(points.size) {
                    detectTapGestures { offset ->
                        val step = size.width / (points.lastIndex).coerceAtLeast(1)
                        val index = (offset.x / step).roundToInt().coerceIn(0, points.lastIndex)
                        onSelect(index)
                    }
                },
        ) {
            fun x(i: Int) = size.width * i / points.lastIndex.toFloat()
            fun y(v: Double) = size.height - (v / max).toFloat() * size.height
            for (step in 0..2) {
                val gy = size.height * step / 2f
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            }
            goal?.let {
                val gy = y(it)
                drawLine(goalColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 3f)
            }
            val path = Path()
            points.forEachIndexed { i, point ->
                val p = Offset(x(i), y(point.value))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
            selectedIndex?.let { i ->
                if (i in points.indices) {
                    drawCircle(selectedColor, radius = 10f, center = Offset(x(i), y(points[i].value)))
                }
            }
        }
    }
}
