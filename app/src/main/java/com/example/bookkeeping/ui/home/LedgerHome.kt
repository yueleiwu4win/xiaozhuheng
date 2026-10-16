package com.example.bookkeeping.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookkeeping.R
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.ui.common.*
import com.example.bookkeeping.ui.theme.*
import com.example.bookkeeping.util.AmountFormatter
import com.example.bookkeeping.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

private val LocalLedgerCompact = staticCompositionLocalOf { false }

/** 两套账页有各自的排版；这里只接收页面状态，查询、筛选、编辑和删除继续由原 ViewModel 负责。 */
@Composable
internal fun LedgerHomeContent(
    yearMonth: YearMonth,
    rows: List<HomeRow>,
    summary: MonthSummary,
    filteredSummary: MonthSummary,
    filter: MonthFilter,
    categoryName: String?,
    currency: String,
    searchVisible: Boolean,
    onSearch: () -> Unit,
    onFilters: () -> Unit,
    searchContent: @Composable () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onChooseMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onType: (String?) -> Unit,
    onClearCategory: () -> Unit,
    onReset: () -> Unit,
    onCreate: () -> Unit,
    today: LocalDate = LocalDate.now(),
    categories: List<com.example.bookkeeping.data.db.entity.CategoryEntity> = emptyList(),
    onCategory: (Long?) -> Unit = {},
    onKeyword: (String) -> Unit = {},
    billContent: @Composable (BillWithCategory) -> Unit
) {
    if (LocalThemeTokens.current.homeLayout == HomeLayout.SONG) {
        SongHomeContent(yearMonth, rows, summary, filteredSummary, filter, categoryName, currency, searchVisible,
            onSearch, onFilters, searchContent, onPreviousMonth, onNextMonth, onChooseMonth, onCurrentMonth,
            onType, onClearCategory, onReset, onCreate, today, billContent)
        return
    }
    if (LocalThemeTokens.current.homeLayout == HomeLayout.INK_GOLD) {
        InkGoldHomeContent(yearMonth, rows, summary, filteredSummary, filter, categories, currency,
            onKeyword, onFilters, onPreviousMonth, onNextMonth, onChooseMonth, onCurrentMonth,
            onCategory, onReset, onCreate, today, billContent)
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    CompositionLocalProvider(LocalLedgerCompact provides (maxHeight < 680.dp)) {
    Column(Modifier.fillMaxSize()) {
        CeladonHeader(searchVisible, filter.isActive, onSearch, onFilters)
        if (searchVisible) searchContent()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp,
                bottom = 12.dp)
        ) {
            item(key = "overview") {
                CeladonOverview(yearMonth, summary, currency, onPreviousMonth, onNextMonth, onChooseMonth)
                if (yearMonth != YearMonth.from(today)) {
                    TextButton(onClick = onCurrentMonth) { Text("回到本月") }
                }
            }
            item(key = "types") {
                CeladonTypeBar(filter.type, onType, Modifier.padding(top = 10.dp))
            }
            if (filter.isActive) item(key = "filtered") {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("筛选结果", style = ledgerText(14.sp), modifier = Modifier.weight(1f))
                        TextButton(onClick = onReset) { Text("清除筛选") }
                    }
                    categoryName?.let { name ->
                        InputChip(selected = true, onClick = onClearCategory, label = { Text(name) },
                            trailingIcon = { Icon(Icons.Outlined.Close, "清除分类", Modifier.size(16.dp)) })
                    }
                    Text("支出 ${AmountFormatter.withSymbol(filteredSummary.expenseCents, currency)} · 收入 ${AmountFormatter.withSymbol(filteredSummary.incomeCents, currency)}",
                        style = ledgerText(13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (rows.isEmpty()) item(key = "empty") {
                EmptyState(if (filter.isActive) "没有符合条件的记录\n试试更换关键词或清除筛选" else "本月还没有记录\n从第一笔收支开始", Icons.Outlined.Receipt)
                if (!filter.isActive) TextButton(onCreate, Modifier.fillMaxWidth()) { Text("记第一笔") }
            }
            items(rows, key = { it.key }, contentType = { if (it is HomeRow.Header) "header" else "bill" }) { row ->
                when (row) {
                    is HomeRow.Header -> LedgerDayHeader(row, currency, today)
                    is HomeRow.Item -> billContent(row.item)
                }
            }
        }
    }
    }
    }
}

@Composable
private fun CeladonHeader(searching: Boolean, filtered: Boolean, onSearch: () -> Unit, onFilters: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val compact = LocalLedgerCompact.current
    Row(Modifier.fillMaxWidth().heightIn(min = if (compact) 64.dp else 72.dp).padding(start = 30.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).offset(y = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (fontScale <= 1.6f) Box(Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
                Image(painterResource(R.drawable.brand_icon_foreground), null, Modifier.fillMaxSize()
                    .graphicsLayer { scaleX = 4f; scaleY = 4f; translationY = size.height * 0.22f }, contentScale = ContentScale.Crop)
            }
            Text(stringResource(R.string.app_name), Modifier.weight(1f, fill = false),
                style = ledgerText(23.sp, FontWeight.SemiBold), maxLines = 1)

        }
        IconButton(onSearch, Modifier.offset(y = 8.dp)) { Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
            if (searching) "关闭搜索" else "搜索备注", Modifier.size(27.dp)) }
        IconButton(onFilters, Modifier.offset(y = 8.dp)) { Icon(Icons.Outlined.FilterList, "筛选记录", Modifier.size(27.dp),
            tint = if (filtered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
private fun CeladonOverview(month: YearMonth, summary: MonthSummary, currency: String,
    onPrevious: () -> Unit, onNext: () -> Unit, onChoose: () -> Unit) {
    val compact = LocalLedgerCompact.current
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(22.dp)) {
        Box(Modifier.fillMaxWidth()) {
            LedgerPaper(Modifier.matchParentSize(), 0.15f)
            Image(painterResource(R.drawable.guofeng_bamboo), null, Modifier.align(Alignment.TopEnd).width(230.dp).height(220.dp),
                alignment = Alignment.TopEnd, contentScale = ContentScale.FillWidth, alpha = 0.24f * readingArtworkAlpha())
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = if (compact) 8.dp else 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val monthModifier = if (LocalDensity.current.fontScale > 1.15f) Modifier.fillMaxWidth()
                    else Modifier.align(Alignment.Start).offset(x = (-12).dp).width(200.dp)
                LedgerMonth(month, onPrevious, onNext, onChoose, monthModifier, 19.sp)
                Spacer(Modifier.height(if (compact) 0.dp else 4.dp))
                Text("本月支出", style = ledgerText(16.sp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                FittedLedgerAmount(AmountFormatter.withSymbol(summary.expenseCents, currency), if (compact) 36.sp else 40.sp,
                    Modifier.fillMaxWidth().padding(top = 2.dp, bottom = if (compact) 6.dp else 10.dp), TextAlign.Center, weight = FontWeight.SemiBold)
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                LedgerBalance(summary, currency, centered = true, Modifier.padding(top = if (compact) 6.dp else 10.dp, bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun LedgerMonth(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit, onChoose: () -> Unit,
    modifier: Modifier = Modifier, size: TextUnit = 20.sp) {
    Row(modifier.widthIn(max = 220.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onPrevious) { Icon(Icons.Outlined.ChevronLeft, "上个月", Modifier.size(26.dp)) }
        TextButton(onChoose, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) {
            FittedLedgerAmount(DateUtils.monthName(month.year, month.monthValue), size, Modifier.fillMaxWidth(), TextAlign.Center)
        }
        IconButton(onNext) { Icon(Icons.Outlined.ChevronRight, "下个月", Modifier.size(26.dp)) }
    }
}

@Composable
private fun LedgerBalance(summary: MonthSummary, currency: String, centered: Boolean, modifier: Modifier) {
    if (LocalDensity.current.fontScale > 1.5f) {
        Column(modifier.fillMaxWidth(), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            Text(if (centered) "本月收入" else "收入", style = ledgerText(15.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            FittedLedgerAmount(AmountFormatter.withSymbol(summary.incomeCents, currency), 22.sp, Modifier.fillMaxWidth(),
                if (centered) TextAlign.Center else TextAlign.Start)
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text("结余", style = ledgerText(15.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            FittedLedgerAmount(AmountFormatter.withSymbol(summary.balanceCents, currency), 22.sp, Modifier.fillMaxWidth(),
                if (centered) TextAlign.Center else TextAlign.Start)
        }
        return
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            Text(if (centered) "本月收入" else "收入", style = ledgerText(15.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            FittedLedgerAmount(AmountFormatter.withSymbol(summary.incomeCents, currency), 22.sp,
                Modifier.fillMaxWidth().padding(top = 2.dp), if (centered) TextAlign.Center else TextAlign.Start)
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.height(44.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            Text("结余", style = ledgerText(15.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            FittedLedgerAmount(AmountFormatter.withSymbol(summary.balanceCents, currency), 22.sp,
                Modifier.fillMaxWidth().padding(top = 2.dp), if (centered) TextAlign.Center else TextAlign.Start)
        }
    }
}

@Composable
private fun CeladonTypeBar(type: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(null to "全部", BillType.EXPENSE to "支出", BillType.INCOME to "收入").forEach { (value, label) ->
            val selected = type == value
            Box(Modifier.weight(1f).selectable(selected, onClick = { onSelect(value) }, role = Role.Tab)
                .heightIn(min = 48.dp).padding(vertical = 4.dp).clip(RoundedCornerShape(28.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                .heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
                Text(label, style = ledgerText(18.sp), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun LedgerDayHeader(row: HomeRow.Header, currency: String, today: LocalDate) {
    Column(Modifier.fillMaxWidth().padding(top = if (LocalLedgerCompact.current) 14.dp else 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val date = DateUtils.parse(row.date)
            val prefix = when (date) { today -> "今天 · "; today.minusDays(1) -> "昨天 · "; else -> "" }
            val dateLabel = "$prefix${if (date.year != today.year) "${date.year}年" else ""}${date.monthValue}月${date.dayOfMonth}日 ${DateUtils.weekdayLabel(date)}"
            Text(dateLabel, style = ledgerText(15.sp), modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                if (row.summary.expenseCount > 0) Text("支出 ${AmountFormatter.withSymbol(row.summary.expenseCents, currency)}",
                    style = ledgerText(13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (row.summary.incomeCount > 0) Text("收入 ${AmountFormatter.withSymbol(row.summary.incomeCents, currency)}",
                    style = ledgerText(13.sp), color = LocalAppColors.current.income)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun LedgerBillRow(item: BillWithCategory, currency: String, incomeColor: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (LocalThemeTokens.current.homeLayout == HomeLayout.SONG) {
        SongBillRow(item, currency, incomeColor, onClick, modifier)
        return
    }
    if (LocalThemeTokens.current.homeLayout == HomeLayout.INK_GOLD) {
        InkGoldBillRow(item, currency, onClick, modifier)
        return
    }
    val compact = LocalLedgerCompact.current
    val income = item.bill.type == BillType.INCOME
    Column(modifier.fillMaxWidth().clickable(onClickLabel = "编辑这笔记录", onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = if (compact) 56.dp else 58.dp).padding(vertical = if (compact) 7.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                val icon = if (item.category.icon == "restaurant") Icons.Outlined.RamenDining
                    else if (item.category.icon == "shopping_bag") Icons.Outlined.LocalGroceryStore else categoryIcon(item.category.icon)
                Icon(icon, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.category.name, style = ledgerText(16.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!item.bill.note.isNullOrBlank()) Text(item.bill.note, style = ledgerText(12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FittedLedgerAmount((if (income) "+" else "−") + AmountFormatter.withSymbol(item.bill.amountCents, currency), 18.sp,
                Modifier.weight(1.1f), TextAlign.End, if (income) incomeColor else MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun ledgerText(size: TextUnit, weight: FontWeight = FontWeight.Normal) = readingTextStyle(TextStyle(
    fontFamily = LedgerTypeface, fontWeight = weight, fontSize = size, lineHeight = size * 1.35f,
    fontFeatureSettings = AmountFontFeatures
))

/** 数字保持完整；按实际字体和可用宽度测量，也覆盖大金额、窄屏和系统大字体。 */
@Composable
internal fun FittedLedgerAmount(text: String, preferredSize: TextUnit, modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start, color: Color = MaterialTheme.colorScheme.onSurface, weight: FontWeight = FontWeight.Normal, textStyle: TextStyle? = null, minSize: TextUnit = 10.sp) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseStyle = readingTextStyle(textStyle ?: ledgerText(preferredSize, weight))
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        val size = remember(text, preferredSize, width, density.fontScale, density.density, measurer, weight, baseStyle, minSize) {
            var low = minSize.value
            var high = preferredSize.value
            repeat(7) {
                val candidate = (low + high) / 2
                if (measurer.measure(text, baseStyle.copy(fontSize = candidate.sp, lineHeight = candidate.sp * 1.35f), maxLines = 1, softWrap = false).size.width <= width)
                    low = candidate else high = candidate
            }
            low
        }
        val fitsOnOneLine = measurer.measure(text, baseStyle.copy(fontSize = size.sp, lineHeight = size.sp * 1.35f), maxLines = 1, softWrap = false).size.width <= width
        Text(text, Modifier.fillMaxWidth(), style = baseStyle.copy(fontSize = size.sp, lineHeight = size.sp * 1.35f), textAlign = align, color = color,
            maxLines = if (fitsOnOneLine) 1 else Int.MAX_VALUE, softWrap = !fitsOnOneLine)
    }
}
