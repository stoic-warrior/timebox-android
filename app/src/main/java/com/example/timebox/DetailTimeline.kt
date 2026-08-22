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

@Composable
fun DetailTimeline(
    date: LocalDate,
    kind: BlockKind,
    blocks: List<TimeBlock>,
    onBlocksChange: (List<TimeBlock>) -> Unit,
    onEdit: (TimeBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val scrollState = rememberScrollState()
    val minuteHeight = MinuteHeightDp.dp
    val dayHeight = minuteHeight * DayMinutes
    val sorted = blocks.ofKind(kind).sortedBy { it.start }
    val density = LocalDensity.current
    var liftedBlockId by remember { mutableStateOf<String?>(null) }
    var activeBlockId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(date) {
        val targetMinute = if (date == LocalDate.now()) {
            (LocalTime.now().hour * 60 + LocalTime.now().minute - 90).coerceAtLeast(0)
        } else {
            6 * 60
        }
        scrollState.scrollTo(with(density) { (targetMinute * MinuteHeightDp).dp.roundToPx() })
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, theme.line, RoundedCornerShape(8.dp))
            .background(theme.cardBrush(), RoundedCornerShape(8.dp))
            .verticalScroll(scrollState)
    ) {
        Row(Modifier.height(dayHeight)) {
            HourColumn(minuteHeight)
            Box(
                Modifier
                    .weight(1f)
                    .height(dayHeight)
                    .background(theme.card)
            ) {
                TimelineGrid()

                if (date == LocalDate.now()) {
                    val now = LocalTime.now()
                    val nowMinute = now.hour * 60 + now.minute
                    Box(
                        Modifier
                            .offset(y = (nowMinute * MinuteHeightDp).dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(theme.now)
                    )
                }

                EmptySpaceGestures(
                    kind = kind,
                    blocks = blocks,
                    onBlocksChange = onBlocksChange,
                    modifier = Modifier.matchParentSize()
                )

                sorted.forEach { block ->
                    TimeBlockView(
                        block = block,
                        blocks = blocks,
                        isActive = activeBlockId == block.id,
                        isLifted = liftedBlockId == block.id || activeBlockId == block.id,
                        onBlocksChange = onBlocksChange,
                        onEdit = onEdit,
                        onActivate = {
                            activeBlockId = block.id
                            liftedBlockId = block.id
                        },
                        onLiftChange = { lifted -> liftedBlockId = if (lifted) block.id else null }
                    )
                }

                blocks.firstOrNull { it.id == activeBlockId }?.let { activeBlock ->
                    FloatingTimeBadge(
                        block = activeBlock,
                        color = theme.color(activeBlock.color).stroke,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = ((activeBlock.start * MinuteHeightDp) - 34).dp)
                            .zIndex(50f)
                    )
                }

                Box(
                    Modifier
                        .matchParentSize()
                        .zIndex(if (activeBlockId == null) -1f else 20f)
                        .pointerInput(activeBlockId) {
                            if (activeBlockId != null) {
                                detectTapGestures { activeBlockId = null; liftedBlockId = null }
                            }
                        }
                )

            }
        }
    }
}

@Composable
fun HourColumn(minuteHeight: Dp) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier
            .width(60.dp)
            .height(minuteHeight * DayMinutes)
            .background(theme.hourBg)
    ) {
        repeat(24) { hour ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(minuteHeight * 60)
                    .border(0.5.dp, theme.rule),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = "%02d:00".format(hour),
                    color = theme.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp, end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TimelineGrid() {
    val theme = LocalAppTheme.current
    val density = LocalDensity.current
    Canvas(Modifier.fillMaxSize()) {
        val line = theme.grid
        for (y in 30..DayMinutes step 30) {
            val scaledY = with(density) { (y * MinuteHeightDp).dp.toPx() }
            drawLine(line, Offset(0f, scaledY), Offset(size.width, scaledY), strokeWidth = 1f)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeBlockView(
    block: TimeBlock,
    blocks: List<TimeBlock>,
    isActive: Boolean,
    isLifted: Boolean,
    onBlocksChange: (List<TimeBlock>) -> Unit,
    onEdit: (TimeBlock) -> Unit,
    onActivate: () -> Unit,
    onLiftChange: (Boolean) -> Unit
) {
    val theme = LocalAppTheme.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val latestBlocks by rememberUpdatedState(blocks)
    val latestOnBlocksChange by rememberUpdatedState(onBlocksChange)
    val blockColor = theme.color(block.color)

    Box(
        modifier = Modifier
            .offset(x = 10.dp, y = (block.start * MinuteHeightDp).dp)
            .zIndex(if (isActive) 30f else if (isLifted) 10f else 1f)
            .graphicsLayer {
                scaleX = if (isLifted) 1.015f else 1f
                scaleY = if (isLifted) 1.015f else 1f
                shadowElevation = if (isLifted) 18f else 0f
                translationY = if (isLifted) -6f else 0f
                shape = RoundedCornerShape(8.dp)
                clip = false
            }
            .fillMaxWidth()
            .height((block.duration * MinuteHeightDp).dp)
            .padding(end = 20.dp)
            .background(blockColor.fillBrush(), RoundedCornerShape(8.dp))
            .border(1.dp, blockColor.stroke, RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {},
                onDoubleClick = { onEdit(block) }
            )
            .pointerInput(block.id) {
                var baseBlocks = emptyList<TimeBlock>()
                var baseStart = 0
                var baseDuration = 0
                var baseBounds = Bounds(0, DayMinutes)
                var accumulated = 0f

                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        baseBlocks = latestBlocks
                        val base = baseBlocks.firstOrNull { it.id == block.id } ?: block
                        baseStart = base.start
                        baseDuration = base.duration
                        baseBounds = neighborBounds(baseBlocks.ofKind(block.kind), block.id, baseStart)
                        accumulated = 0f
                        onActivate()
                        context.vibrateShort()
                    },
                    onDragEnd = {},
                    onDragCancel = {},
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulated += with(density) { dragAmount.y.toDp().value } / MinuteHeightDp
                        val delta = snapMinutes(accumulated.roundToInt())
                        val maxStart = baseBounds.maxEnd - baseDuration
                        val nextStart = if (maxStart < baseBounds.minStart) {
                            baseStart
                        } else {
                            (baseStart + delta).coerceIn(baseBounds.minStart, maxStart)
                        }
                        latestOnBlocksChange(baseBlocks.map { if (it.id == block.id) it.copy(start = nextStart) else it })
                    }
                )
            }
            .then(
                if (isActive) {
                    Modifier.pointerInput(block.id, isActive) {
                        var baseBlocks = emptyList<TimeBlock>()
                        var baseStart = 0
                        var baseDuration = 0
                        var baseBounds = Bounds(0, DayMinutes)
                        var accumulated = 0f

                        detectDragGestures(
                            onDragStart = {
                                baseBlocks = latestBlocks
                                val base = baseBlocks.firstOrNull { it.id == block.id } ?: block
                                baseStart = base.start
                                baseDuration = base.duration
                                baseBounds = neighborBounds(baseBlocks.ofKind(block.kind), block.id, baseStart)
                                accumulated = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulated += with(density) { dragAmount.y.toDp().value } / MinuteHeightDp
                                val delta = snapMinutes(accumulated.roundToInt())
                                val maxStart = baseBounds.maxEnd - baseDuration
                                val nextStart = if (maxStart < baseBounds.minStart) {
                                    baseStart
                                } else {
                                    (baseStart + delta).coerceIn(baseBounds.minStart, maxStart)
                                }
                                latestOnBlocksChange(baseBlocks.map { if (it.id == block.id) it.copy(start = nextStart) else it })
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = if (isActive) 58.dp else 12.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = block.title,
                color = theme.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${formatTime(block.start)} - ${formatTime(block.start + block.duration)}",
                color = theme.ink.copy(alpha = 0.72f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        if (isActive) {
            ResizeHandle(
                block = block,
                blocks = blocks,
                isActive = isActive,
                isLifted = isLifted,
                onBlocksChange = onBlocksChange,
                onLiftChange = onLiftChange,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun FloatingTimeBadge(block: TimeBlock, color: Color, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .background(theme.ink, RoundedCornerShape(999.dp))
            .border(1.dp, color, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "${formatTime(block.start)} - ${formatTime(block.start + block.duration)}",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun ResizeHandle(
    block: TimeBlock,
    blocks: List<TimeBlock>,
    isActive: Boolean,
    isLifted: Boolean,
    onBlocksChange: (List<TimeBlock>) -> Unit,
    onLiftChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val latestBlocks by rememberUpdatedState(blocks)
    val latestOnBlocksChange by rememberUpdatedState(onBlocksChange)

    Box(
        modifier = modifier
            .width(48.dp)
            .fillMaxSize()
            .pointerInput(block.id, isActive) {
                var baseBlocks = emptyList<TimeBlock>()
                var baseStart = 0
                var baseDuration = 0
                var baseBounds = Bounds(0, DayMinutes)
                var accumulated = 0f

                detectDragGestures(
                    onDragStart = {
                        baseBlocks = latestBlocks
                        val base = baseBlocks.firstOrNull { it.id == block.id } ?: block
                        baseStart = base.start
                        baseDuration = base.duration
                        baseBounds = neighborBounds(baseBlocks.ofKind(block.kind), block.id, baseStart)
                        accumulated = 0f
                        onLiftChange(true)
                    },
                    onDragEnd = { onLiftChange(false) },
                    onDragCancel = { onLiftChange(false) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulated += with(density) { dragAmount.y.toDp().value } / MinuteHeightDp
                        val delta = snapMinutes(accumulated.roundToInt())
                        val maxDuration = baseBounds.maxEnd - baseStart
                        val nextDuration = (baseDuration + delta).coerceIn(MinBlockMinutes, maxDuration)
                        latestOnBlocksChange(baseBlocks.map { if (it.id == block.id) it.copy(duration = nextDuration) else it })
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .height(44.dp)
                .width(24.dp)
                .background(
                    theme.ink.copy(alpha = 0.93f),
                    RoundedCornerShape(999.dp)
                )
                .border(
                    1.dp,
                    if (isLifted) Color.White else Color(0x99FFFFFF),
                    RoundedCornerShape(999.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "↕",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptySpaceGestures(
    kind: BlockKind,
    blocks: List<TimeBlock>,
    onBlocksChange: (List<TimeBlock>) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    Box(
        modifier.pointerInput(blocks) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    val minute = snapMinutes((with(density) { offset.y.toDp().value } / MinuteHeightDp).roundToInt()).coerceIn(0, DayMinutes - MinBlockMinutes)
                    val gap = gapForMinute(blocks.ofKind(kind), minute)
                    if (gap != null && gap.maxEnd - gap.minStart >= MinBlockMinutes) {
                        val start = minute.coerceIn(gap.minStart, gap.maxEnd - MinBlockMinutes)
                        val duration = min(30, gap.maxEnd - start)
                        onBlocksChange(blocks + TimeBlock(title = "새 블록", start = start, duration = duration, kind = kind))
                        context.vibrateShort()
                    }
                },
                onDrag = { change, _ -> change.consume() }
            )
        }
    )
}
