package com.example.bookkeeping.ui.chart

import androidx.compose.foundation.Canvas
import com.example.bookkeeping.ui.theme.readingTextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookkeeping.util.AmountFormatter
import com.example.bookkeeping.ui.theme.LocalThemeTokens
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** value 单位：分。 */
data class ChartPoint(val label: String, val value: Long)

data class ChartSeries(
    val name: String,
    val color: Color,
    val points: List<ChartPoint>
)

data class DonutSlice(
    val label: String,
    val value: Long,
    val color: Color
)

private const val PAD_LEFT_DP = 48f
private const val PAD_RIGHT_DP = 12f

/** 顶部留白要能容纳「高亮数值」标签（约 13dp 文字 + 4dp 间距），否则顶到轴顶时会被裁掉。 */
private const val PAD_TOP_DP = 22f
private const val PAD_BOTTOM_DP = 26f

/** 文本测量缓存：一次绘制需要 5 个 Y 刻度 + 最多 31 个 X 标签，默认缓存只有 8。 */
private const val TEXT_CACHE_SIZE = 40

private data class AxisTextMetrics(
    val y: List<TextLayoutResult>, val x: List<TextLayoutResult>,
    val left: Float, val right: Float, val top: Float, val bottom: Float
)

@Composable
private fun axisTextMetrics(maxCents: Long, labels: List<String>, style: TextStyle, measurer: TextMeasurer, line: Boolean): AxisTextMetrics {
    val density = LocalDensity.current
    val y = (0..4).map { measurer.measure(AmountFormatter.axisLabel((maxCents * (it / 4f)).toLong()), style) }
    val x = labels.map { measurer.measure(it, style, maxLines = 1, softWrap = false) }
    return with(density) {
        AxisTextMetrics(y, x,
            maxOf(PAD_LEFT_DP.dp.toPx(), (y.maxOfOrNull { it.size.width } ?: 0) + 6.dp.toPx()),
            maxOf(PAD_RIGHT_DP.dp.toPx(), if (line) (x.lastOrNull()?.size?.width ?: 0) / 2f + 2.dp.toPx() else 0f),
            maxOf(PAD_TOP_DP.dp.toPx(), (y.maxOfOrNull { it.size.height } ?: 0) / 2f + 4.dp.toPx()),
            maxOf(PAD_BOTTOM_DP.dp.toPx(), (x.maxOfOrNull { it.size.height } ?: 0) + 12.dp.toPx()))
    }
}

/** 把最大值向上取整到「好看的刻度」。入参单位：元。 */
private fun niceCeilYuan(v: Double): Double {
    if (v <= 0) return 10.0
    val base = 10.0.pow(floor(log10(v)))
    val m = v / base
    val niceM = when {
        m <= 1.0 -> 1.0
        m <= 2.0 -> 2.0
        m <= 2.5 -> 2.5
        m <= 5.0 -> 5.0
        else -> 10.0
    }
    return niceM * base
}

/**
 * 折线图（支持多序列，如「类别逐年对比」）。
 * 约定：所有序列共享同一份 X 轴 label（缺数据的年份在 ViewModel 层补 0）。
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    onPointTap: ((pointIndex: Int, seriesIndex: Int) -> Unit)? = null
) {
    val density = LocalDensity.current
    val textMeasurer: TextMeasurer = rememberTextMeasurer(cacheSize = TEXT_CACHE_SIZE)
    // 坐标轴配色跟随主题（深色模式下网格/刻度需用主题色，否则网格线不可见）
    val axisText = readingTextStyle(TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labels = remember(series) { series.firstOrNull()?.points?.map { it.label } ?: emptyList() }
    val maxCents = remember(series) { series.flatMap { it.points }.maxOfOrNull { it.value } ?: 0L }
    var niceMaxCents = remember(maxCents) { (niceCeilYuan(maxCents / 100.0) * 100).toLong() }
    if (niceMaxCents <= 0L) niceMaxCents = 100L

    val metrics = axisTextMetrics(niceMaxCents, labels, axisText, textMeasurer, line = true)
    val padL = metrics.left
    val padR = metrics.right
    val padT = metrics.top
    val padB = metrics.bottom

    // 无障碍：读屏可播报一行摘要（明细数据在下方列表里可读）
    val chartDesc = remember(labels, maxCents) {
        "折线图，共 ${labels.size} 个数据点，最高 ${AmountFormatter.withSymbol(maxCents)}"
    }

    Canvas(
        modifier = modifier
            .semantics { contentDescription = chartDesc }
            .then(
                if (onPointTap != null) {
                    Modifier.pointerInput(series, labels, padL, padR, padT, padB) {
                    val plotW = size.width - padL - padR
                    val plotH = size.height - padT - padB
                    detectTapGestures { pos ->
                        val n = labels.size
                        if (n == 0 || plotW <= 0) return@detectTapGestures
                        val step = if (n == 1) plotW else plotW / (n - 1)
                        val idx = if (n == 1) 0 else ((pos.x - padL) / step).roundToInt().coerceIn(0, n - 1)
                        var bestS = 0
                        var bestDist = Float.MAX_VALUE
                        for ((si, s) in series.withIndex()) {
                            if (idx !in s.points.indices) continue
                            val y = padT + plotH * (1f - s.points[idx].value.toFloat() / niceMaxCents)
                            val d = abs(y - pos.y)
                            if (d < bestDist) {
                                bestDist = d
                                bestS = si
                            }
                        }
                        if (bestDist < 40.dp.toPx()) onPointTap(idx, bestS)
                    }
                }
            } else Modifier
        )
    ) {
        val plotW = size.width - padL - padR
        val plotH = size.height - padT - padB
        if (plotW <= 0 || plotH <= 0 || labels.isEmpty()) return@Canvas
        val n = labels.size
        val step = if (n == 1) plotW else plotW / (n - 1)
        fun xOf(i: Int): Float = if (n == 1) padL + plotW / 2f else padL + step * i
        fun yOf(v: Long): Float = padT + plotH * (1f - v.toFloat() / niceMaxCents)

        // 网格 + Y 轴刻度
        val divs = 4
        for (d in 0..divs) {
            val frac = d.toFloat() / divs
            val y = padT + plotH * (1f - frac)
            drawLine(gridColor, Offset(padL, y), Offset(padL + plotW, y), strokeWidth = 1f)
            val layout = metrics.y[d]
            if (d == 0 || d == divs || plotH / divs >= layout.size.height + 4.dp.toPx() || d % 2 == 0)
                drawText(layout, topLeft = Offset(padL - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f))
        }

        for (label in placeAxisLabels(labels.indices.map { xOf(it) }, metrics.x.map { it.size.width.toFloat() }, size.width, 6.dp.toPx())) {
            drawText(metrics.x[label.index], topLeft = Offset(label.left, size.height - padB + 8.dp.toPx()))
        }

        // 序列
        for ((si, s) in series.withIndex()) {
            if (s.points.size != n) continue
            val path = Path()
            s.points.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(xOf(i), yOf(p.value)) else path.lineTo(xOf(i), yOf(p.value))
            }
            if (series.size == 1) {
                val area = Path().apply {
                    addPath(path)
                    lineTo(xOf(n - 1), padT + plotH)
                    lineTo(xOf(0), padT + plotH)
                    close()
                }
                drawPath(area, color = s.color.copy(alpha = 0.10f))
            }
            drawPath(path, color = s.color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            s.points.forEachIndexed { i, p ->
                drawCircle(s.color, radius = 3.5f, center = Offset(xOf(i), yOf(p.value)))
            }
        }
    }
}

/** 纵向柱状图（如某年 12 个月消费）。 */
@Composable
fun BarChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF4A90D9),
    highlightIndex: Int = -1,
    /** 高亮数值的格式化（含币种符号），默认 ¥。 */
    valueFormatter: (Long) -> String = { AmountFormatter.withSymbol(it) },
    onBarTap: ((Int) -> Unit)? = null
) {
    val density = LocalDensity.current
    val textMeasurer: TextMeasurer = rememberTextMeasurer(cacheSize = TEXT_CACHE_SIZE)
    val axisText = readingTextStyle(TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val valueText = readingTextStyle(TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
    val maxCents = remember(points) { points.maxOfOrNull { it.value } ?: 0L }
    var niceMaxCents = remember(maxCents) { (niceCeilYuan(maxCents / 100.0) * 100).toLong() }
    if (niceMaxCents <= 0L) niceMaxCents = 100L

    val metrics = axisTextMetrics(niceMaxCents, points.map { it.label }, axisText, textMeasurer, line = false)
    val padL = metrics.left
    val padR = metrics.right
    val padT = metrics.top
    val padB = metrics.bottom

    Canvas(
        modifier = modifier.then(
            if (onBarTap != null) {
                Modifier.pointerInput(points, padL, padR) {
                    val plotW = size.width - padL - padR
                    detectTapGestures { pos ->
                        val n = points.size
                        if (n == 0 || plotW <= 0) return@detectTapGestures
                        // 用 floor 命中：柱 i 覆盖 [i*slot, (i+1)*slot)，roundToInt 会整体右移半格
                        val slot = plotW / n
                        val idx = ((pos.x - padL) / slot).toInt().coerceIn(0, n - 1)
                        onBarTap(idx)
                    }
                }
            } else Modifier
        )
    ) {
        val plotW = size.width - padL - padR
        val plotH = size.height - padT - padB
        val n = points.size
        if (plotW <= 0 || plotH <= 0 || n == 0) return@Canvas
        val slot = plotW / n
        val barW = slot * 0.52f

        val divs = 4
        for (d in 0..divs) {
            val frac = d.toFloat() / divs
            val y = padT + plotH * (1f - frac)
            drawLine(gridColor, Offset(padL, y), Offset(padL + plotW, y), strokeWidth = 1f)
            val layout = metrics.y[d]
            if (d == 0 || d == divs || plotH / divs >= layout.size.height + 4.dp.toPx() || d % 2 == 0)
                drawText(layout, topLeft = Offset(padL - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f))
        }

        for (i in points.indices) {
            val cx = padL + slot * i + slot / 2f
            val v = points[i].value
            val barH = plotH * v.toFloat() / niceMaxCents
            val top = padT + plotH - barH
            val color = if (i == highlightIndex) barColor else barColor.copy(alpha = 0.55f)
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - barW / 2f, if (barH > 2f) top else padT + plotH - 1f),
                size = Size(barW, if (barH > 2f) barH else 1f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            if (i == highlightIndex && v > 0) {
                val layout = textMeasurer.measure(valueFormatter(v), valueText)
                // 顶到轴顶时改画在柱内顶部，避免被画布裁掉
                val above = top - layout.size.height - 4.dp.toPx()
                val labelTop = if (above < padT) top + 4.dp.toPx() else above
                drawText(layout, topLeft = Offset((cx - layout.size.width / 2f).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f)), labelTop))
            }
        }
        for (label in placeAxisLabels(points.indices.map { padL + slot * (it + 0.5f) }, metrics.x.map { it.size.width.toFloat() }, size.width, 6.dp.toPx())) {
            drawText(metrics.x[label.index], topLeft = Offset(label.left, size.height - padB + 8.dp.toPx()))
        }
    }
}

/** 环形图（类别占比）。 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    centerTitle: String = "",
    centerSubtitle: String = "",
    onSliceTap: ((Int) -> Unit)? = null
) {
    val density = LocalDensity.current
    val textMeasurer: TextMeasurer = rememberTextMeasurer(cacheSize = TEXT_CACHE_SIZE)
    val centerTitleStyle = readingTextStyle(TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold))
    val centerSubtitleStyle = readingTextStyle(TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface))
    val total = remember(slices) { slices.sumOf { it.value } }
    val centerTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val chartDesc = remember(slices, total) {
        "环形图，共 ${slices.size} 个分类，合计 $centerTitle"
    }

    Canvas(
        modifier = modifier
            .semantics { contentDescription = chartDesc }
            .then(
                if (onSliceTap != null) {
                Modifier.pointerInput(slices, total) {
                    detectTapGestures { pos ->
                        if (total <= 0) return@detectTapGestures
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val strokeW = minOf(size.width, size.height) * 0.16f
                        val ringR = minOf(size.width, size.height) / 2f - strokeW / 2f
                        val dist = hypot((pos.x - cx).toDouble(), (pos.y - cy).toDouble()).toFloat()
                        if (dist < ringR - strokeW / 2f - 8.dp.toPx() ||
                            dist > ringR + strokeW / 2f + 8.dp.toPx()
                        ) {
                            return@detectTapGestures
                        }
                        var angle =
                            Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())).toFloat()
                        angle = (angle + 90f + 360f) % 360f // 起点 12 点方向
                        var acc = 0f
                        for ((i, s) in slices.withIndex()) {
                            acc += s.value.toFloat() / total * 360f
                            if (angle < acc) {
                                onSliceTap(i)
                                break
                            }
                        }
                    }
                }
            } else Modifier
        )
    ) {
        if (total <= 0) return@Canvas
        val r = minOf(size.width, size.height) / 2f
        val strokeW = minOf(size.width, size.height) * 0.16f
        val topLeft = Offset(size.width / 2f - r, size.height / 2f - r)
        val arcSize = Size(r * 2f, r * 2f)
        val gap = if (slices.size > 1) 1.5f else 0f
        var start = -90f
        for (s in slices) {
            val sweep = s.value.toFloat() / total * 360f
            if (sweep > gap) {
                drawArc(
                    color = s.color,
                    startAngle = start,
                    sweepAngle = sweep - gap,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW)
                )
            }
            start += sweep
        }
        // 中心文字
        if (centerTitle.isNotEmpty()) {
            val t = textMeasurer.measure(
                centerTitle,
                centerTitleStyle.copy(color = centerTextColor)
            )
            drawText(t, topLeft = Offset((size.width - t.size.width) / 2f, (size.height - t.size.height) / 2f - 8.dp.toPx()))
        }
        if (centerSubtitle.isNotEmpty()) {
            val s = textMeasurer.measure(centerSubtitle, centerSubtitleStyle.copy(color = centerTextColor))
            drawText(s, topLeft = Offset((size.width - s.size.width) / 2f, (size.height - s.size.height) / 2f + 10.dp.toPx()))
        }
    }
}

/** 横向占比条（排行列表用）。 */
@Composable
fun RankBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(LocalThemeTokens.current.rankCorner))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(f)
                .background(color, RoundedCornerShape(LocalThemeTokens.current.rankCorner))
        )
    }
}
