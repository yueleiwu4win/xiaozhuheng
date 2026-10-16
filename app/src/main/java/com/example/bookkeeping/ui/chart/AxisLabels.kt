package com.example.bookkeeping.ui.chart

internal data class AxisLabelPosition(val index: Int, val left: Float, val width: Float)

/** 以真实文字宽度排布，末项优先保留；标签不得超出画布或相互覆盖。 */
internal fun placeAxisLabels(centers: List<Float>, widths: List<Float>, canvasWidth: Float, gap: Float): List<AxisLabelPosition> {
    require(centers.size == widths.size)
    if (centers.isEmpty() || canvasWidth <= 0f) return emptyList()
    fun position(i: Int): AxisLabelPosition? {
        val width = widths[i]
        if (width > canvasWidth) return null
        return AxisLabelPosition(i, (centers[i] - width / 2f).coerceIn(0f, canvasWidth - width), width)
    }
    val last = position(centers.lastIndex) ?: return emptyList()
    val result = mutableListOf<AxisLabelPosition>()
    val step = if (centers.size > 8) 2 else 1
    for (i in 0 until centers.lastIndex step step) {
        val next = position(i) ?: continue
        if ((result.isEmpty() || next.left >= result.last().left + result.last().width + gap) &&
            next.left + next.width + gap <= last.left) result += next
    }
    result += last
    return result
}
