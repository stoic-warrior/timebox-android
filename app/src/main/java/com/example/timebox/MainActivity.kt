package com.example.timebox

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TimeboxApp()
            }
        }
    }
}

data class TimeBlock(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val start: Int,
    val duration: Int,
    val color: String = "green"
)

private const val MinBlockMinutes = 5
private const val MinuteHeightDp = 2
private const val OverviewHourHeightDp = 42

data class BlockColor(
    val key: String,
    val label: String,
    val fill: Color,
    val stroke: Color
)

val BlockColors = listOf(
    BlockColor("green", "초록", Color(0xFFE7F1EB), Color(0xFF2F7D57)),
    BlockColor("blue", "파랑", Color(0xFFE5EDF8), Color(0xFF315F99)),
    BlockColor("yellow", "노랑", Color(0xFFF8EFD8), Color(0xFFA16E12)),
    BlockColor("violet", "보라", Color(0xFFEEE9F7), Color(0xFF6B529F)),
    BlockColor("red", "빨강", Color(0xFFF7E8E5), Color(0xFFA9463A))
)

@Composable
fun TimeboxApp() {
    val context = LocalContext.current
    val store = remember { TimeboxStore(context) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var blocks by remember { mutableStateOf(store.load(LocalDate.now())) }
    var editingBlock by remember { mutableStateOf<TimeBlock?>(null) }
    var overviewMode by remember { mutableStateOf(false) }

    LaunchedEffect(blocks, date) {
        store.save(date, blocks)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F6F2))
            .padding(10.dp)
    ) {
        DateBar(
            date = date,
            onPrevious = {
                date = date.minusDays(1)
                blocks = store.load(date)
                editingBlock = null
            },
            onNext = {
                date = date.plusDays(1)
                blocks = store.load(date)
                editingBlock = null
            }
        )

        Spacer(Modifier.height(8.dp))

        ViewToggle(
            overviewMode = overviewMode,
            onOverviewModeChange = { overviewMode = it }
        )

        Spacer(Modifier.height(8.dp))

        if (overviewMode) {
            OverviewTimeline(
                date = date,
                blocks = blocks,
                modifier = Modifier.weight(1f)
            )
        } else {
            Timeline(
                date = date,
                blocks = blocks,
                onBlocksChange = { blocks = it },
                onEdit = { editingBlock = it },
                modifier = Modifier.weight(1f)
            )
        }
    }

    editingBlock?.let { block ->
        EditBlockDialog(
            block = block,
            onSave = { title, color ->
                blocks = blocks.map { if (it.id == block.id) it.copy(title = title.ifBlank { "새 블록" }, color = color) else it }
                editingBlock = null
            },
            onDelete = {
                blocks = blocks.filterNot { it.id == block.id }
                editingBlock = null
            },
            onDismiss = { editingBlock = null }
        )
    }
}

@Composable
fun DateBar(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit) {
    val today = LocalDate.now()
    val label = date.format(DateTimeFormatter.ofPattern("M월 d일"))
    val sub = if (date == today) "오늘" else date.toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
            Text("‹", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF232323))
            Text(sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF696A70))
        }
        IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
            Text("›", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ViewToggle(overviewMode: Boolean, onOverviewModeChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFD9D6CC), RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToggleButton(
            text = "상세",
            selected = !overviewMode,
            onClick = { onOverviewModeChange(false) },
            modifier = Modifier.weight(1f)
        )
        ToggleButton(
            text = "한눈",
            selected = overviewMode,
            onClick = { onOverviewModeChange(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ToggleButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (selected) Color(0xFF232323) else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF696A70),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OverviewTimeline(date: LocalDate, blocks: List<TimeBlock>, modifier: Modifier = Modifier) {
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
            .border(1.dp, Color(0xFFD9D6CC), RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp)
    ) {
        OverviewStatsPanel(
            blocks = blocks,
            modifier = Modifier
                .width(132.dp)
                .fillMaxSize()
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawOverviewGrid()
                        blocks.forEach { block ->
                            drawOverviewBlock(block)
                        }
                        if (date == LocalDate.now()) {
                            val now = LocalTime.now()
                            val minute = now.hour * 60 + now.minute
                            val y = size.height * minute / 1440f
                            drawLine(
                                color = Color(0xFFA9463A),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewStatsPanel(blocks: List<TimeBlock>, modifier: Modifier = Modifier) {
    val totals = BlockColors
        .map { color -> color to blocks.filter { it.color == color.key }.sumOf { it.duration } }
        .filter { it.second > 0 }

    Column(
        modifier = modifier.padding(start = 12.dp, end = 10.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (totals.isEmpty()) {
            Text(
                text = "기록 없음",
                color = Color(0xFF8A8780),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            totals.forEach { (color, minutes) ->
                ColorStatRow(color = color, minutes = minutes)
            }
        }
    }
}

@Composable
fun ColorStatRow(color: BlockColor, minutes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(color.stroke, RoundedCornerShape(999.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = color.label,
            color = Color(0xFF232323),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatDuration(minutes),
            color = Color(0xFF4F4D49),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun OverviewHourLabels() {
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
                    color = Color(0xFF696A70),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun Timeline(
    date: LocalDate,
    blocks: List<TimeBlock>,
    onBlocksChange: (List<TimeBlock>) -> Unit,
    onEdit: (TimeBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val minuteHeight = MinuteHeightDp.dp
    val dayHeight = minuteHeight * 1440
    val sorted = blocks.sortedBy { it.start }
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
            .border(1.dp, Color(0xFFD9D6CC), RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .verticalScroll(scrollState)
    ) {
        Row(Modifier.height(dayHeight)) {
            HourColumn(minuteHeight)
            Box(
                Modifier
                    .weight(1f)
                    .height(dayHeight)
                    .background(Color.White)
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
                            .background(Color(0xFFA9463A))
                    )
                }

                EmptySpaceGestures(
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
                        color = blockColor(activeBlock.color).stroke,
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
fun HourColumn(minuteHeight: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .height(minuteHeight * 1440)
            .background(Color(0xFFFBFAF6))
    ) {
        repeat(24) { hour ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(minuteHeight * 60)
                    .border(0.5.dp, Color(0xFFEBE8DF)),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = "%02d:00".format(hour),
                    color = Color(0xFF696A70),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp, end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TimelineGrid() {
    val density = LocalDensity.current
    Canvas(Modifier.fillMaxSize()) {
        val line = Color(0xFFF0EDE5)
        for (y in 30..1440 step 30) {
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
    val density = LocalDensity.current
    val context = LocalContext.current
    val latestBlocks by rememberUpdatedState(blocks)
    val latestOnBlocksChange by rememberUpdatedState(onBlocksChange)
    val blockColor = blockColor(block.color)

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
            .background(blockColor.fill, RoundedCornerShape(8.dp))
            .border(1.dp, blockColor.stroke, RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {},
                onDoubleClick = { onEdit(block) }
            )
            .pointerInput(block.id) {
                var baseBlocks = emptyList<TimeBlock>()
                var baseStart = 0
                var baseDuration = 0
                var baseBounds = Bounds(0, 1440)
                var accumulated = 0f

                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        baseBlocks = latestBlocks
                        val base = baseBlocks.firstOrNull { it.id == block.id } ?: block
                        baseStart = base.start
                        baseDuration = base.duration
                        baseBounds = neighborBounds(baseBlocks, block.id, baseStart)
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
                        var baseBounds = Bounds(0, 1440)
                        var accumulated = 0f

                        detectDragGestures(
                            onDragStart = {
                                baseBlocks = latestBlocks
                                val base = baseBlocks.firstOrNull { it.id == block.id } ?: block
                                baseStart = base.start
                                baseDuration = base.duration
                                baseBounds = neighborBounds(baseBlocks, block.id, baseStart)
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
                color = Color(0xFF232323),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${formatTime(block.start)} - ${formatTime(block.start + block.duration)}",
                color = Color(0xB8232323),
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
    Box(
        modifier = modifier
            .background(Color(0xFF232323), RoundedCornerShape(999.dp))
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
                var baseBounds = Bounds(0, 1440)
                var accumulated = 0f

                detectDragGestures(
                    onDragStart = {
                        baseBlocks = latestBlocks
                        val base = baseBlocks.firstOrNull { it.id == block.id } ?: block
                        baseStart = base.start
                        baseDuration = base.duration
                        baseBounds = neighborBounds(baseBlocks, block.id, baseStart)
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
                    Color(0xEE232323),
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
                    val minute = snapMinutes((with(density) { offset.y.toDp().value } / MinuteHeightDp).roundToInt()).coerceIn(0, 1440 - MinBlockMinutes)
                    val gap = gapForMinute(blocks, minute)
                    if (gap != null && gap.maxEnd - gap.minStart >= MinBlockMinutes) {
                        val start = minute.coerceIn(gap.minStart, gap.maxEnd - MinBlockMinutes)
                        val duration = min(30, gap.maxEnd - start)
                        onBlocksChange(blocks + TimeBlock(title = "새 블록", start = start, duration = duration))
                        context.vibrateShort()
                    }
                },
                onDrag = { change, _ -> change.consume() }
            )
        }
    )
}

@Composable
fun EditBlockDialog(
    block: TimeBlock,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(block.id) { mutableStateOf(block.title) }
    var selectedColor by remember(block.id) { mutableStateOf(block.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("블록 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(fontSize = 18.sp, color = Color(0xFF232323)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD9D6CC), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BlockColors.forEach { option ->
                        val selected = selectedColor == option.key
                        Box(
                            modifier = Modifier
                                .size(if (selected) 34.dp else 30.dp)
                                .background(option.fill, RoundedCornerShape(999.dp))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) option.stroke else Color(0xFFD9D6CC),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable { selectedColor = option.key }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, selectedColor) }) {
                Text("저장")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("삭제", color = Color(0xFFA9463A))
                }
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}

data class Bounds(val minStart: Int, val maxEnd: Int)

fun neighborBounds(blocks: List<TimeBlock>, id: String, start: Int): Bounds {
    val others = blocks
        .filterNot { it.id == id }
        .map { it.start to it.start + it.duration }
        .sortedBy { it.first }
    val previous = others.lastOrNull { it.second <= start }
    val next = others.firstOrNull { it.first >= start }
    return Bounds(previous?.second ?: 0, next?.first ?: 1440)
}

fun gapForMinute(blocks: List<TimeBlock>, minute: Int): Bounds? {
    val sorted = blocks.map { it.start to it.start + it.duration }.sortedBy { it.first }
    var minStart = 0
    var maxEnd = 1440

    for ((start, end) in sorted) {
        if (minute in start until end) return null
        if (end <= minute) minStart = end
        if (start > minute) {
            maxEnd = start
            break
        }
    }

    return Bounds(minStart, maxEnd)
}

fun blockColor(key: String): BlockColor = BlockColors.firstOrNull { it.key == key } ?: BlockColors.first()

fun DrawScope.drawOverviewGrid() {
    val minor = Color(0xFFF2F0EA)
    val major = Color(0xFFE3E0D8)
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

fun DrawScope.drawOverviewBlock(block: TimeBlock) {
    val color = blockColor(block.color).stroke
    val rowHeight = size.height / 24f
    val blockHeight = rowHeight.coerceAtLeast(12f)
    var cursor = block.start.coerceIn(0, 1440)
    val end = (block.start + block.duration).coerceIn(0, 1440)
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
            color = color,
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

fun snapMinutes(value: Int): Int = (value / 5f).roundToInt() * 5

fun formatTime(minutes: Int): String {
    val clamped = minutes.coerceIn(0, 1439)
    return "%02d:%02d".format(clamped / 60, clamped % 60)
}

fun formatDuration(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

fun Context.vibrateShort() {
    runCatching {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(12)
        }
    }
}

class TimeboxStore(context: Context) {
    private val prefs = context.getSharedPreferences("timeboxes", Context.MODE_PRIVATE)

    fun load(date: LocalDate): List<TimeBlock> {
        val raw = prefs.getString(date.toString(), null)
        if (raw == null) {
            return emptyList()
        }
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                TimeBlock(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    start = item.getInt("start"),
                    duration = item.getInt("duration"),
                    color = item.optString("color", "green")
                )
            }
        }.getOrElse { emptyList() }
    }

    fun save(date: LocalDate, blocks: List<TimeBlock>) {
        val array = JSONArray()
        blocks.forEach { block ->
            array.put(JSONObject().apply {
                put("id", block.id)
                put("title", block.title)
                put("start", block.start)
                put("duration", block.duration)
                put("color", block.color)
            })
        }
        prefs.edit().putString(date.toString(), array.toString()).apply()
    }

}
