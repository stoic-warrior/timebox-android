package com.example.timebox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime

private val HourColumnWidth = 60.dp
private val HourLabelWidth = 42.dp

/**
 * 비교 탭은 읽기 전용이다. 상세와 한눈은 편집 여부가 아니라 배율의 차이고,
 * 한눈에서 어긋난 게 눈에 띄면 상세로 확대해서 몇 분인지 확인하는 흐름이다.
 */
@Composable
fun CompareDetail(date: LocalDate, blocks: List<TimeBlock>, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val minuteHeight = MinuteHeightDp.dp
    val dayHeight = minuteHeight * DayMinutes

    LaunchedEffect(date) {
        val targetMinute = if (date == LocalDate.now()) {
            (LocalTime.now().hour * 60 + LocalTime.now().minute - 90).coerceAtLeast(0)
        } else {
            6 * 60
        }
        scrollState.scrollTo(with(density) { (targetMinute * MinuteHeightDp).dp.roundToPx() })
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, theme.line, RoundedCornerShape(8.dp))
            .background(theme.cardBrush(), RoundedCornerShape(8.dp))
    ) {
        LaneHeader(leadingWidth = HourColumnWidth)

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Row(Modifier.height(dayHeight)) {
                HourColumn(minuteHeight)
                Box(
                    Modifier
                        .weight(1f)
                        .height(dayHeight)
                ) {
                    TimelineGrid()

                    if (date == LocalDate.now()) {
                        val now = LocalTime.now()
                        Box(
                            Modifier
                                .offset(y = ((now.hour * 60 + now.minute) * MinuteHeightDp).dp)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(theme.now)
                        )
                    }

                    Row(Modifier.fillMaxSize()) {
                        CompareLane(
                            lane = blocks.ofKind(BlockKind.PLAN),
                            all = blocks,
                            modifier = Modifier.weight(1f)
                        )
                        CompareLane(
                            lane = blocks.ofKind(BlockKind.ACTUAL),
                            all = blocks,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompareOverview(date: LocalDate, blocks: List<TimeBlock>, modifier: Modifier = Modifier) {
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, theme.line, RoundedCornerShape(8.dp))
            .background(theme.cardBrush(), RoundedCornerShape(8.dp))
            .padding(top = 8.dp, bottom = 8.dp, end = 10.dp)
    ) {
        LaneHeader(leadingWidth = HourLabelWidth)

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(overviewScroll)
        ) {
            Row(Modifier.height(dayHeight)) {
                OverviewHourLabels()
                OverviewGraph(
                    blocks = blocks.ofKind(BlockKind.PLAN),
                    showNowLine = date == LocalDate.now(),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(theme.line)
                )
                OverviewGraph(
                    blocks = blocks.ofKind(BlockKind.ACTUAL),
                    showNowLine = date == LocalDate.now(),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

/** 시간축은 왼쪽에 하나만 둔다. 두 열은 그 축을 공유한다. */
@Composable
private fun LaneHeader(leadingWidth: androidx.compose.ui.unit.Dp) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(leadingWidth))
        listOf("계획", "실제").forEach { label ->
            Text(
                text = label,
                color = theme.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompareLane(lane: List<TimeBlock>, all: List<TimeBlock>, modifier: Modifier) {
    val theme = LocalAppTheme.current
    Box(modifier.fillMaxHeight()) {
        lane.sortedBy { it.start }.forEach { block ->
            StaticBlock(block = block, delta = startDelta(block, all))
        }
    }
}

/** 계획과 연결된 실제 블록이 몇 분 밀려서 시작했는지. 연결이 없거나 같으면 null. */
private fun startDelta(block: TimeBlock, all: List<TimeBlock>): Int? {
    val planId = block.planId ?: return null
    val plan = all.firstOrNull { it.id == planId } ?: return null
    val delta = block.start - plan.start
    return if (delta == 0) null else delta
}

@Composable
private fun StaticBlock(block: TimeBlock, delta: Int?) {
    val theme = LocalAppTheme.current
    val color = theme.color(block.color)

    Box(
        modifier = Modifier
            .offset(y = (block.start * MinuteHeightDp).dp)
            .fillMaxWidth()
            .height((block.duration * MinuteHeightDp).dp)
            .padding(horizontal = 4.dp)
            .background(color.fillBrush(), RoundedCornerShape(8.dp))
            .border(1.dp, color.stroke, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = block.title,
            color = theme.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp)
        )

        if (delta != null && block.duration >= 25) {
            Text(
                text = if (delta > 0) "+${delta}분" else "${delta}분",
                color = theme.ink.copy(alpha = 0.60f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 4.dp)
            )
        }
    }
}
