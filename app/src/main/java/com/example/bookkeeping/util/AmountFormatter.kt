package com.example.bookkeeping.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object AmountFormatter {

    private val FMT = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    /** 分 → "3,280.50"（千分位、两位小数）。 */
    fun format(cents: Long): String = FMT.format(cents / 100.0)

    fun withSymbol(cents: Long, symbol: String = "¥"): String = "$symbol${format(cents)}"

    /** 图表坐标轴缩写：3500 → "3,500"；1560000 → "1.56万"。 */
    fun axisLabel(cents: Long): String {
        val yuan = cents / 100.0
        return when {
            yuan >= 100_000_000 -> String.format(Locale.US, "%.1f亿", yuan / 100_000_000.0)
            yuan >= 10_000 -> String.format(Locale.US, "%.1f万", yuan / 10_000.0)
            yuan >= 1_000 -> String.format(Locale.US, "%,.0f", yuan)
            else -> String.format(Locale.US, "%,.2f", yuan)
        }
    }

    fun symbolOf(currency: String): String = when (currency.uppercase()) {
        "CNY", "RMB" -> "¥"
        "USD" -> "$"
        "JPY" -> "JP¥"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$currency "
    }
}