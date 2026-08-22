package com.example.timebox

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import java.util.UUID
import kotlin.math.roundToInt

/** 타임라인 한 줄이 계획인지 실제 기록인지. */
enum class BlockKind { PLAN, ACTUAL }

data class TimeBlock(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val start: Int,
    val duration: Int,
    val color: String = "green",
    val kind: BlockKind = BlockKind.PLAN,
    /** 이 실제 기록이 어느 계획에서 나왔는지. 계획 블록이면 항상 null. */
    val planId: String? = null
)

/**
 * 계획 탭 왼쪽의 브레인덤프 항목.
 * 타임라인과는 아무 관계가 없다. 블록으로 옮기는 경로도, 서로를 참조하는 필드도 없다.
 */
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean = false
)

const val MinBlockMinutes = 5
const val MinuteHeightDp = 2
const val OverviewHourHeightDp = 42
const val DayMinutes = 1440

/** 겹침 판정은 항상 같은 레인 안에서만 한다. 계획과 실제는 서로 겹쳐도 된다. */
fun List<TimeBlock>.ofKind(kind: BlockKind): List<TimeBlock> = filter { it.kind == kind }

data class Bounds(val minStart: Int, val maxEnd: Int)

fun neighborBounds(blocks: List<TimeBlock>, id: String, start: Int): Bounds {
    val others = blocks
        .filterNot { it.id == id }
        .map { it.start to it.start + it.duration }
        .sortedBy { it.first }
    val previous = others.lastOrNull { it.second <= start }
    val next = others.firstOrNull { it.first >= start }
    return Bounds(previous?.second ?: 0, next?.first ?: DayMinutes)
}

fun gapForMinute(blocks: List<TimeBlock>, minute: Int): Bounds? {
    val sorted = blocks.map { it.start to it.start + it.duration }.sortedBy { it.first }
    var minStart = 0
    var maxEnd = DayMinutes

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
    val clamped = minutes.coerceIn(0, DayMinutes - 1)
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
