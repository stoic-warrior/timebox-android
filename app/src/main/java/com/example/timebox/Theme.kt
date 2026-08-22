package com.example.timebox

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 화면 전체가 쓰는 색 한 벌.
 *
 * 블록에 저장되는 값은 "green" 같은 키라서, 테마를 바꿔도 기존 블록은 그대로 유지되고
 * 그 키가 가리키는 색만 달라진다.
 */
data class AppTheme(
    val key: String,
    val label: String,
    val background: Color,
    val backgroundTop: Color,
    val card: Color,
    val cardTop: Color,
    val hourBg: Color,
    val line: Color,
    val grid: Color,
    val gridMinor: Color,
    val rule: Color,
    val overviewMajor: Color,
    val overviewMinor: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val hint: Color,
    val doneText: Color,
    val now: Color,
    val blocks: List<BlockColor>
) {
    /** 위에서 아래로 아주 얕게 빠지는 배경. 명도 차이가 3% 안쪽이라 눈에 띄지 않는다. */
    fun backgroundBrush(): Brush = Brush.verticalGradient(
        0f to backgroundTop,
        0.42f to background,
        1f to background
    )

    fun cardBrush(): Brush = Brush.verticalGradient(listOf(cardTop, card))

    fun color(key: String): BlockColor = blocks.firstOrNull { it.key == key } ?: blocks.first()
}

data class BlockColor(
    val key: String,
    val label: String,
    val fill: Color,
    val stroke: Color
)

private fun Color.lighten(amount: Float) = lerp(this, Color.White, amount)
private fun Color.darken(amount: Float) = lerp(this, Color.Black, amount)

/** 블록 채움. 위가 살짝 밝고 아래가 살짝 진하다. */
fun BlockColor.fillBrush(): Brush = Brush.verticalGradient(
    listOf(fill.lighten(0.05f), fill.darken(0.035f))
)

/** 한눈 그래프의 색면. Canvas 안에서는 그라데이션 범위를 직접 지정해야 한다. */
fun BlockColor.segmentBrush(startY: Float, endY: Float): Brush = Brush.verticalGradient(
    colors = listOf(stroke.lighten(0.10f), stroke.darken(0.07f)),
    startY = startY,
    endY = endY
)

val CreamTheme = AppTheme(
    key = "cream",
    label = "크림",
    background = Color(0xFFF7F6F2),
    backgroundTop = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardTop = Color(0xFFFFFFFF),
    hourBg = Color(0xFFFBFAF6),
    line = Color(0xFFD9D6CC),
    grid = Color(0xFFF0EDE5),
    gridMinor = Color(0xFFF7F5EF),
    rule = Color(0xFFEBE8DF),
    overviewMajor = Color(0xFFE3E0D8),
    overviewMinor = Color(0xFFF2F0EA),
    ink = Color(0xFF232323),
    muted = Color(0xFF696A70),
    faint = Color(0xFFC9C6BE),
    hint = Color(0xFFB4B1A8),
    doneText = Color(0xFFAEABA3),
    now = Color(0xFFA9463A),
    blocks = listOf(
        BlockColor("green", "초록", Color(0xFFE7F1EB), Color(0xFF2F7D57)),
        BlockColor("blue", "파랑", Color(0xFFE5EDF8), Color(0xFF315F99)),
        BlockColor("yellow", "노랑", Color(0xFFF8EFD8), Color(0xFFA16E12)),
        BlockColor("violet", "보라", Color(0xFFEEE9F7), Color(0xFF6B529F)),
        BlockColor("red", "빨강", Color(0xFFF7E8E5), Color(0xFFA9463A))
    )
)

val FrostTheme = AppTheme(
    key = "frost",
    label = "서리",
    background = Color(0xFFEEF2F8),
    backgroundTop = Color(0xFFFFFFFF),
    card = Color(0xFFFBFCFE),
    cardTop = Color(0xFFFFFFFF),
    hourBg = Color(0xFFF5F8FC),
    line = Color(0xFFCBD6E4),
    grid = Color(0xFFE4EBF4),
    gridMinor = Color(0xFFEFF3F9),
    rule = Color(0xFFDFE7F1),
    overviewMajor = Color(0xFFD3DEEC),
    overviewMinor = Color(0xFFE9EFF7),
    ink = Color(0xFF16202E),
    muted = Color(0xFF5D6B7F),
    faint = Color(0xFFAEBACB),
    hint = Color(0xFF9EAABB),
    doneText = Color(0xFF98A5B6),
    now = Color(0xFFE0553F),
    blocks = listOf(
        BlockColor("green", "초록", Color(0xFFE3F1EC), Color(0xFF0F766E)),
        BlockColor("blue", "파랑", Color(0xFFE2ECFB), Color(0xFF1D5FBE)),
        BlockColor("yellow", "노랑", Color(0xFFF6EFDC), Color(0xFFA16207)),
        BlockColor("violet", "보라", Color(0xFFEBE8FA), Color(0xFF6D45D0)),
        BlockColor("red", "빨강", Color(0xFFFBE7EA), Color(0xFFBE123C))
    )
)

val AppThemes = listOf(CreamTheme, FrostTheme)

fun themeForKey(key: String?): AppTheme = AppThemes.firstOrNull { it.key == key } ?: FrostTheme

val LocalAppTheme = staticCompositionLocalOf { FrostTheme }
