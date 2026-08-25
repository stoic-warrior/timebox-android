package com.example.timebox

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

enum class Tab { PLAN, ACTUAL, COMPARE }

enum class Zoom { DETAIL, OVERVIEW }

private fun Tab.kind(): BlockKind =
    if (this == Tab.ACTUAL) BlockKind.ACTUAL else BlockKind.PLAN

@Composable
fun TimeboxApp() {
    val context = LocalContext.current
    val store = remember { TimeboxStore(context) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var blocks by remember { mutableStateOf(store.loadBlocks(LocalDate.now())) }
    var todos by remember { mutableStateOf(store.loadTodos(LocalDate.now())) }
    var editingBlock by remember { mutableStateOf<TimeBlock?>(null) }
    var tab by remember { mutableStateOf(Tab.PLAN) }
    var zoom by remember { mutableStateOf(Zoom.DETAIL) }
    var theme by remember { mutableStateOf(themeForKey(store.loadThemeKey())) }
    var showSettings by remember { mutableStateOf(false) }

    val activity = context as? Activity
    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = theme.background.toArgb()
            window.navigationBarColor = theme.background.toArgb()
        }
    }

    LaunchedEffect(blocks, date) { store.saveBlocks(date, blocks) }
    LaunchedEffect(todos, date) { store.saveTodos(date, todos) }

    fun moveTo(next: LocalDate) {
        date = next
        blocks = store.loadBlocks(next)
        todos = store.loadTodos(next)
        editingBlock = null
    }

    CompositionLocalProvider(LocalAppTheme provides theme) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundBrush())
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(10.dp)
    ) {
        DateBar(
            date = date,
            onPrevious = { moveTo(date.minusDays(1)) },
            onNext = { moveTo(date.plusDays(1)) },
            onSettings = { showSettings = true }
        )

        Spacer(Modifier.height(8.dp))

        SegmentedBar(
            items = listOf("계획", "실제", "비교"),
            selectedIndex = tab.ordinal,
            onSelect = { tab = Tab.entries[it] }
        )

        Spacer(Modifier.height(6.dp))

        SegmentedBar(
            items = listOf("상세", "한눈"),
            selectedIndex = zoom.ordinal,
            onSelect = { zoom = Zoom.entries[it] }
        )

        Spacer(Modifier.height(8.dp))

        when {
            tab == Tab.COMPARE && zoom == Zoom.DETAIL -> CompareDetail(
                date = date,
                blocks = blocks,
                modifier = Modifier.weight(1f)
            )

            tab == Tab.COMPARE -> CompareOverview(
                date = date,
                blocks = blocks,
                modifier = Modifier.weight(1f)
            )

            zoom == Zoom.DETAIL -> DetailTimeline(
                date = date,
                kind = tab.kind(),
                blocks = blocks,
                onBlocksChange = { blocks = it },
                onEdit = { editingBlock = it },
                modifier = Modifier.weight(1f)
            )

            else -> OverviewTimeline(
                date = date,
                blocks = blocks.ofKind(tab.kind()),
                modifier = Modifier.weight(1f),
                leftPanel = { panelModifier ->
                    if (tab == Tab.PLAN) {
                        ChecklistPanel(
                            todos = todos,
                            onTodosChange = { todos = it },
                            modifier = panelModifier
                        )
                    } else {
                        OverviewStatsPanel(
                            blocks = blocks.ofKind(BlockKind.ACTUAL),
                            modifier = panelModifier
                        )
                    }
                }
            )
        }
    }

    editingBlock?.let { block ->
        EditBlockDialog(
            block = block,
            onSave = { title, color ->
                blocks = blocks.map {
                    if (it.id == block.id) it.copy(title = title.ifBlank { "새 블록" }, color = color) else it
                }
                editingBlock = null
            },
            onDelete = {
                blocks = blocks.filterNot { it.id == block.id || it.planId == block.id }
                editingBlock = null
            },
            onDismiss = { editingBlock = null }
        )
    }

    if (showSettings) {
        SettingsDialog(
            current = theme,
            onSelect = {
                theme = it
                store.saveThemeKey(it.key)
            },
            onDismiss = { showSettings = false }
        )
    }
    }
}

@Composable
fun SettingsDialog(current: AppTheme, onSelect: (AppTheme) -> Unit, onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.card,
        title = { Text("테마", color = theme.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemes.forEach { option ->
                    ThemeRow(
                        option = option,
                        selected = option.key == current.key,
                        onClick = { onSelect(option) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = theme.ink)
            }
        }
    )
}

@Composable
private fun ThemeRow(option: AppTheme, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) option.background else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) option.ink else option.line,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = option.label,
            color = option.ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            option.blocks.forEach { block ->
                Box(
                    Modifier
                        .size(width = 12.dp, height = 20.dp)
                        .background(block.stroke, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun DateBar(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSettings: () -> Unit
) {
    val theme = LocalAppTheme.current
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(40.dp))
            IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                Text("‹", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = theme.ink)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.ink)
            Text(sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                Text("›", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = theme.ink)
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
                Text("⚙", fontSize = 18.sp, color = theme.muted)
            }
        }
    }
}

@Composable
fun SegmentedBar(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(theme.cardBrush(), RoundedCornerShape(8.dp))
            .border(1.dp, theme.line, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, label ->
            ToggleButton(
                text = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ToggleButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (selected) theme.ink else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else theme.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EditBlockDialog(
    block: TimeBlock,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
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
                    textStyle = TextStyle(fontSize = 18.sp, color = theme.ink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, theme.line, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    theme.blocks.forEach { option ->
                        val selected = selectedColor == option.key
                        Box(
                            modifier = Modifier
                                .size(if (selected) 34.dp else 30.dp)
                                .background(option.fill, RoundedCornerShape(999.dp))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) option.stroke else theme.line,
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
                    Text("삭제", color = theme.now)
                }
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}
