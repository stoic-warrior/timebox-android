package com.example.timebox

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 날짜별 로컬 저장소.
 *
 * 블록은 예전과 같은 키("2026-08-21")를 그대로 쓴다. kind 필드가 없던 기존 레코드는
 * 읽을 때 PLAN으로 채워지므로 별도 마이그레이션 코드가 필요 없다.
 * 체크리스트는 "2026-08-21:todos"에 따로 저장한다. 날짜별로 끊기고 다음 날로 넘어가지 않는다.
 */
class TimeboxStore(context: Context) {
    private val prefs = context.getSharedPreferences("timeboxes", Context.MODE_PRIVATE)

    private fun blockKey(date: LocalDate) = date.toString()
    private fun todoKey(date: LocalDate) = "$date:todos"

    fun loadBlocks(date: LocalDate): List<TimeBlock> {
        val raw = prefs.getString(blockKey(date), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                TimeBlock(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    start = item.getInt("start"),
                    duration = item.getInt("duration"),
                    color = item.optString("color", "green"),
                    kind = runCatching { BlockKind.valueOf(item.optString("kind", "PLAN")) }
                        .getOrDefault(BlockKind.PLAN),
                    planId = if (item.isNull("planId")) null else item.optString("planId", null)
                )
            }
        }.getOrElse { emptyList() }
    }

    fun saveBlocks(date: LocalDate, blocks: List<TimeBlock>) {
        val array = JSONArray()
        blocks.forEach { block ->
            array.put(JSONObject().apply {
                put("id", block.id)
                put("title", block.title)
                put("start", block.start)
                put("duration", block.duration)
                put("color", block.color)
                put("kind", block.kind.name)
                put("planId", block.planId ?: JSONObject.NULL)
            })
        }
        prefs.edit().putString(blockKey(date), array.toString()).apply()
    }

    /** 테마는 날짜와 무관한 앱 설정이라 날짜 없는 키에 둔다. */
    fun loadThemeKey(): String? = prefs.getString("theme", null)

    fun saveThemeKey(key: String) {
        prefs.edit().putString("theme", key).apply()
    }

    fun loadTodos(date: LocalDate): List<TodoItem> {
        val raw = prefs.getString(todoKey(date), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                TodoItem(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    done = item.optBoolean("done", false)
                )
            }
        }.getOrElse { emptyList() }
    }

    fun saveTodos(date: LocalDate, todos: List<TodoItem>) {
        val array = JSONArray()
        todos.forEach { todo ->
            array.put(JSONObject().apply {
                put("id", todo.id)
                put("title", todo.title)
                put("done", todo.done)
            })
        }
        prefs.edit().putString(todoKey(date), array.toString()).apply()
    }
}
