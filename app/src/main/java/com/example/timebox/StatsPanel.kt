package com.example.timebox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 실제 탭 한눈 모드의 왼쪽 패널. 하루가 어느 색으로 갔는지만 보여준다. */
@Composable
fun OverviewStatsPanel(blocks: List<TimeBlock>, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val totals = theme.blocks
        .map { color -> color to blocks.filter { it.color == color.key }.sumOf { it.duration } }
        .filter { it.second > 0 }

    Column(
        modifier = modifier.padding(start = 12.dp, end = 10.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (totals.isEmpty()) {
            Text(
                text = "기록 없음",
                color = theme.hint,
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
    val theme = LocalAppTheme.current
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
            color = theme.ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatDuration(minutes),
            color = theme.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
