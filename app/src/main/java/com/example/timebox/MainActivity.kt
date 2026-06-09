package com.example.timebox

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val duration: Int
)

private const val MinBlockMinutes = 5
private const val MinuteHeightDp = 2

@Composable
fun TimeboxApp() {
    val context = LocalContext.current
    val store = remember { TimeboxStore(context) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var blocks by remember { mutableStateOf(store.load(LocalDate.now())) }
    var editingBlock by remember { mutableStateOf<TimeBlock?>(null) }

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

        Timeline(
            date = date,
            blocks = blocks,
            onBlocksChange = { blocks = it },
            onEdit = { editingBlock = it },
            modifier = Modifier.weight(1f)
        )
    }

    editingBlock?.let { block ->
        EditBlockDialog(
            block = block,
            onSave = { title ->
                blocks = blocks.map { if (it.id == block.id) it.copy(title = title.ifBlank { "새 블록" }) else it }
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
                        onBlocksChange = onBlocksChange,
                        onEdit = onEdit
                    )
                }

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
    onBlocksChange: (List<TimeBlock>) -> Unit,
    onEdit: (TimeBlock) -> Unit
) {
    val density = LocalDensity.current
    val latestBlocks by rememberUpdatedState(blocks)
    val latestOnBlocksChange by rememberUpdatedState(onBlocksChange)

    Box(
        modifier = Modifier
            .offset(x = 10.dp, y = (block.start * MinuteHeightDp).dp)
            .fillMaxWidth()
            .height((block.duration * MinuteHeightDp).dp)
            .padding(end = 20.dp)
            .background(Color(0xFFE7F1EB), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF2F7D57), RoundedCornerShape(8.dp))
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 12.dp, top = 10.dp),
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

        ResizeHandle(
            block = block,
            blocks = blocks,
            onBlocksChange = onBlocksChange,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ResizeHandle(
    block: TimeBlock,
    blocks: List<TimeBlock>,
    onBlocksChange: (List<TimeBlock>) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val latestBlocks by rememberUpdatedState(blocks)
    val latestOnBlocksChange by rememberUpdatedState(onBlocksChange)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .pointerInput(block.id) {
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
                        val maxDuration = baseBounds.maxEnd - baseStart
                        val nextDuration = (baseDuration + delta).coerceIn(MinBlockMinutes, maxDuration)
                        latestOnBlocksChange(baseBlocks.map { if (it.id == block.id) it.copy(duration = nextDuration) else it })
                    }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(Modifier.size(width = 34.dp, height = 8.dp)) {
            drawRoundRect(
                color = Color(0x772F7D57),
                topLeft = Offset(0f, 2f),
                size = androidx.compose.ui.geometry.Size(size.width, 4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
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
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(block.id) { mutableStateOf(block.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("블록 수정") },
        text = {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(fontSize = 18.sp, color = Color(0xFF232323)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFD9D6CC), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        },
        confirmButton = {
            Button(onClick = { onSave(title) }) {
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

fun snapMinutes(value: Int): Int = (value / 5f).roundToInt() * 5

fun formatTime(minutes: Int): String {
    val clamped = minutes.coerceIn(0, 1439)
    return "%02d:%02d".format(clamped / 60, clamped % 60)
}

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
            return if (date == LocalDate.now()) defaultBlocks() else emptyList()
        }
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                TimeBlock(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    start = item.getInt("start"),
                    duration = item.getInt("duration")
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
            })
        }
        prefs.edit().putString(date.toString(), array.toString()).apply()
    }

    private fun defaultBlocks(): List<TimeBlock> = listOf(
        TimeBlock(title = "아침 정리", start = 8 * 60, duration = 30),
        TimeBlock(title = "집중 작업", start = 9 * 60, duration = 120),
        TimeBlock(title = "점심", start = 12 * 60 + 30, duration = 60),
        TimeBlock(title = "리뷰와 정리", start = 16 * 60 + 30, duration = 60)
    )
}
