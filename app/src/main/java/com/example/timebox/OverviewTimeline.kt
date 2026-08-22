package com.example.timebox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.min
import kotlin.math.roundToInt

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

@Composable
fun OverviewTimeline(
    date: LocalDate,
    blocks: List<TimeBlock>,
    modifier: Modifier = Modifier,
    leftPanel: (@Composable (Modifier) -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    val overviewScroll = rememberScrollState()
    val density = LocalDensity.current
    val dayHeight = (OverviewHourHeightDp * 24).dp

    LaunchedEffect(date) {
        val targetHour = if (date == LocalDate.now()) {
            (LocalTime.now().hour - 2).coerceAtLeast(0)
        } else {
            6
        }
        overviewScroll.scrollTo(with(density) { (OverviewHourHeightDp * targetHour).dp.roundToPx() })
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, theme.line, RoundedCornerShape(8.dp))
            .background(theme.cardBrush(), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp)
    ) {
        leftPanel?.invoke(
            Modifier
                .fillMaxWidth(0.42f)
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(end = 10.dp)
                .verticalScroll(overviewScroll)
        ) {
            Row(Modifier.height(dayHeight)) {
                OverviewHourLabels()
                OverviewGraph(
                    blocks = blocks,
                    showNowLine = date == LocalDate.now(),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }
        }
    }
}

/** 24행 압축 그래프 한 장. 비교 탭에서는 이 그래프를 계획/실제 두 장 나란히 그린다. */
@Composable
fun OverviewGraph(
    blocks: List<TimeBlock>,
    showNowLine: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Canvas(modifier) {
        drawOverviewGrid(theme)
        blocks.forEach { block -> drawOverviewBlock(block, theme) }
        if (showNowLine) {
            val now = LocalTime.now()
            val minute = now.hour * 60 + now.minute
            val y = size.height * minute / DayMinutes.toFloat()
            drawLine(
                color = theme.now,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun OverviewHourLabels() {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier
            .width(42.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.End
    ) {
        repeat(24) { hour ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OverviewHourHeightDp.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = if (hour == 0) "12" else if (hour <= 12) "$hour" else "${hour - 12}",
                    color = theme.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                )
            }
        }
    }
}

fun DrawScope.drawOverviewGrid(theme: AppTheme) {
    val minor = theme.overviewMinor
    val major = theme.overviewMajor
    val rowHeight = size.height / 24f

    for (hour in 0..24) {
        val y = rowHeight * hour
        drawLine(
            color = if (hour % 3 == 0) major else minor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = if (hour % 3 == 0) 1.4f else 1f
        )
    }

    for (slot in 1 until 12) {
        val x = size.width * slot / 12f
        drawLine(
            color = minor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
    }
}

fun DrawScope.drawOverviewBlock(block: TimeBlock, theme: AppTheme) {
    val color = theme.color(block.color)
    val rowHeight = size.height / 24f
    val blockHeight = rowHeight.coerceAtLeast(12f)
    var cursor = block.start.coerceIn(0, DayMinutes)
    val end = (block.start + block.duration).coerceIn(0, DayMinutes)
    var remainingLabel = block.title

    while (cursor < end) {
        val hour = cursor / 60
        val minuteInHour = cursor % 60
        val segmentEnd = minOf(end, (hour + 1) * 60)
        val segmentMinutes = segmentEnd - cursor

        val left = size.width * minuteInHour / 60f
        val top = rowHeight * hour
        val width = (size.width * segmentMinutes / 60f).coerceAtLeast(3f)

        drawRect(
            brush = color.segmentBrush(startY = top, endY = top + blockHeight),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(width, blockHeight)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(width, blockHeight),
            style = Stroke(width = 1.5f)
        )

        if (remainingLabel.isNotBlank() && width >= 28f) {
            remainingLabel = drawOverviewBlockLabel(
                remainingText = remainingLabel,
                left = left,
                top = top,
                width = width,
                height = blockHeight
            )
        }

        cursor = segmentEnd
    }
}

fun DrawScope.drawOverviewBlockLabel(
    remainingText: String,
    left: Float,
    top: Float,
    width: Float,
    height: Float
): String {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
        textSize = 14.sp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val horizontalPadding = 4f
    val availableWidth = width - horizontalPadding * 2
    if (availableWidth <= 0f) return remainingText

    val visibleCount = paint.breakText(remainingText, true, availableWidth, null)
    if (visibleCount <= 0) return remainingText

    val visibleText = remainingText.take(visibleCount)
    val textY = top + height / 2f - (paint.descent() + paint.ascent()) / 2f
    val canvas = drawContext.canvas.nativeCanvas
    val save = canvas.save()
    canvas.clipRect(left + horizontalPadding, top, left + width - horizontalPadding, top + height)
    canvas.drawText(visibleText, left + horizontalPadding, textY, paint)
    canvas.restoreToCount(save)
    return remainingText.drop(visibleCount)
}
