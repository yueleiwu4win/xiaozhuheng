package com.example.bookkeeping.ui.summary

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.bookkeeping.data.repo.MonthlySummary
import com.example.bookkeeping.util.AmountFormatter
import java.util.Locale

/** Export and preview use the same complete bitmap, independent of screen scroll/size. */
object SummaryPoster {
    fun lines(summary: MonthlySummary, currency: String, hidden: Boolean): List<String> {
        fun money(cents: Long) = if (hidden) "••••" else AmountFormatter.withSymbol(cents, currency)
        val p = summary.period
        return buildList {
            add("小珠衡 · 月度小结")
            add("${p.month.year}年${p.month.monthValue}月 · 截至 ${p.end}")
            add("本月支出  ${money(summary.expense)}")
            add("本月收入  ${money(summary.income)}")
            add("本月结余  ${money(summary.balance)}")
            add("共 ${summary.count} 笔记录")
            add("对比期间：${p.previousStart} 至 ${p.previousEnd}")
            add(if (hidden) "金额已隐藏" else summary.changeText(::money))
            add("支出分类")
            summary.categories.take(3).forEach { add("${it.name}  ${money(it.totalCents)} · ${String.format(Locale.CHINA, "%.1f", if (summary.expense == 0L) 0.0 else it.totalCents.toDouble() / summary.expense * 100)}%") }
            summary.largest?.let { add("最大单笔：${it.bill.date} · ${it.category.name} · ${money(it.bill.amountCents)}") }
            summary.budget?.let { budget ->
                add("月度预算  ${money(budget)}")
                add(if (summary.expense > budget) "超出预算  ${money(summary.expense - budget)}" else "预算剩余  ${money(budget - summary.expense)}")
            }
            add(summary.insight())
            add("依据本机账单生成 · ${if (p.month == java.time.YearMonth.from(p.end) && p.end != p.month.atEndOfMonth()) "本月尚未结束" else "月度记录"}")
        }
    }

    fun render(lines: List<String>, background: Int, foreground: Int, accent: Int, typeface: Typeface, artwork: Bitmap? = null): Bitmap {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = foreground; textSize = 36f; this.typeface = typeface }
        val layouts = lines.mapIndexed { index, line ->
            textPaint.textSize = if (index == 0) 62f else if (index in 2..4) 46f else 34f
            textPaint.color = if (index == 0 || index == 8) accent else foreground
            StaticLayout.Builder.obtain(line, 0, line.length, TextPaint(textPaint), 888).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true).setLineSpacing(8f, 1f).build()
        }
        val height = layouts.sumOf { it.height + 32 } + 180
        return Bitmap.createBitmap(1080, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap); canvas.drawColor(background)
            artwork?.let {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 28 }
                canvas.drawBitmap(it, null, Rect(0, 0, 1080, height), paint)
            }
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.STROKE; strokeWidth = 2f }
            canvas.drawRect(40f, 40f, 1040f, height - 40f, border)
            var y = 96f
            layouts.forEach { layout -> canvas.save(); canvas.translate(96f, y); layout.draw(canvas); canvas.restore(); y += layout.height + 32 }
        }
    }
}
