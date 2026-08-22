package com.example.timebox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 계획 탭의 한눈 모드 왼쪽에 붙는 체크리스트.
 * 오른쪽 타임라인과는 연결되지 않는다. 항목을 블록으로 옮기는 경로는 없고, 있어서도 안 된다.
 */
@Composable
fun ChecklistPanel(
    todos: List<TodoItem>,
    onTodosChange: (List<TodoItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    var draft by remember { mutableStateOf("") }

    fun commit() {
        val text = draft.trim()
        if (text.isNotEmpty()) {
            onTodosChange(todos + TodoItem(title = text))
            draft = ""
        }
    }

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = theme.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                decorationBox = { inner ->
                    if (draft.isEmpty()) {
                        Text(
                            text = "쏟아내기",
                            color = theme.hint,
                            fontSize = 13.sp
                        )
                    }
                    inner()
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .background(theme.ink, RoundedCornerShape(999.dp))
                    .clickable { commit() },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(theme.grid)
        )

        if (todos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "오늘 머릿속에 있는 걸\n전부 적어두세요.",
                    color = theme.hint,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(todos, key = { it.id }) { todo ->
                    ChecklistRow(
                        todo = todo,
                        onToggle = {
                            onTodosChange(todos.map {
                                if (it.id == todo.id) it.copy(done = !it.done) else it
                            })
                        },
                        onDelete = { onTodosChange(todos.filterNot { it.id == todo.id }) }
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(theme.grid)
            )
            Text(
                text = "${todos.count { it.done }} / ${todos.size} 완료",
                color = theme.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun ChecklistRow(todo: TodoItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 9.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
                .background(
                    if (todo.done) theme.ink else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
                .border(
                    1.5.dp,
                    if (todo.done) theme.ink else theme.line,
                    RoundedCornerShape(4.dp)
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (todo.done) {
                Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = todo.title,
            color = if (todo.done) theme.doneText else theme.ink,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textDecoration = if (todo.done) TextDecoration.LineThrough else null,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onToggle)
        )

        Box(
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Text("×", color = theme.faint, fontSize = 15.sp)
        }
    }
}
