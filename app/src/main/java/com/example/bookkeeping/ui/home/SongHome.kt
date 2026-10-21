package com.example.bookkeeping.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookkeeping.R
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.prefs.BACKGROUND_SPEC_DEFAULT
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.ui.common.*
import com.example.bookkeeping.ui.theme.LocalAppDark
import com.example.bookkeeping.ui.theme.readingArtworkAlpha
import com.example.bookkeeping.util.AmountFormatter
import com.example.bookkeeping.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

/** 绢本山水独立账页；筛选、编辑、滑动删除仍由原页面的回调处理。 */
@Composable
internal fun SongHomeContent(
    month: YearMonth, rows: List<HomeRow>, summary: MonthSummary, filteredSummary: MonthSummary,
    filter: MonthFilter, categoryName: String?, currency: String, searchVisible: Boolean,
    onSearch: () -> Unit, onFilters: () -> Unit, searchContent: @Composable () -> Unit,
    onPrevious: () -> Unit, onNext: () -> Unit, onChooseMonth: () -> Unit, onCurrentMonth: () -> Unit,
    onType: (String?) -> Unit, onClearCategory: () -> Unit, onReset: () -> Unit, onCreate: () -> Unit,
    today: LocalDate, billContent: @Composable (BillWithCategory) -> Unit
) {
    // 头部与账目一起滚动，横屏和大字号下始终能到达全部内容。
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item("landscape") {
            SongMasthead(month, searchVisible, filter.isActive, onSearch, onFilters, onPrevious, onNext, onChooseMonth)
        }
        if (searchVisible) item("search") { searchContent() }
        item("overview") { SongSummary(summary, currency) }
        if (month != YearMonth.from(today)) item("currentMonth") {
            TextButton(onCurrentMonth, Modifier.padding(horizontal = 16.dp)) { Text("回到本月") }
        }
        item("types") { SongTypeBar(filter.type, onType) }
        if (filter.isActive) item("filtered") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("筛选结果 · ${filteredSummary.expenseCount + filteredSummary.incomeCount} 笔", Modifier.weight(1f), style = ledgerText(13.sp))
                    TextButton(onReset) { Text("清除筛选") }
                }
                categoryName?.let { name ->
                    InputChip(true, onClearCategory, label = { Text(name) }, trailingIcon = { Icon(Icons.Outlined.Close, "清除分类", Modifier.size(16.dp)) })
                }
                Text("支出 ${AmountFormatter.withSymbol(filteredSummary.expenseCents, currency)} · 收入 ${AmountFormatter.withSymbol(filteredSummary.incomeCents, currency)}",
                    style = ledgerText(12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (rows.isEmpty()) item("empty") {
            EmptyState(if (filter.isActive) "没有符合条件的记录\n试试更换关键词或清除筛选" else "本月还没有记录\n从第一笔收支开始", Icons.Outlined.ReceiptLong)
            TextButton(if (filter.isActive) onReset else onCreate, Modifier.fillMaxWidth()) {
                Text(if (filter.isActive) "清除筛选" else "记第一笔")
            }
        }
        items(rows, key = { it.key }, contentType = { if (it is HomeRow.Header) "header" else "bill" }) { row ->
            when (row) {
                is HomeRow.Header -> SongDayHeader(row, today)
                is HomeRow.Item -> Box(Modifier.padding(horizontal = 24.dp)) { billContent(row.item) }
            }
        }
    }
}

@Composable
private fun SongMasthead(month: YearMonth, searching: Boolean, filtered: Boolean,
    onSearch: () -> Unit, onFilters: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onChoose: () -> Unit) {
    val painted = LocalBackgroundSpec.current == BACKGROUND_SPEC_DEFAULT
    Box(Modifier.fillMaxWidth().heightIn(min = if (painted) 220.dp else 130.dp)) {
        if (painted) Image(painterResource(R.drawable.song_landscape), null,
            Modifier.matchParentSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    // 只对真实山水素材的边缘做透明混合，避免浅深纸色出现接缝。
                    val edgeMask = Brush.verticalGradient(0f to Color.Black, 0.7f to Color.Black, 1f to Color.Transparent)
                    onDrawWithContent { drawContent(); drawRect(edgeMask, blendMode = BlendMode.DstIn) }
                }, contentScale = ContentScale.Crop, alignment = Alignment.BottomCenter,
            alpha = (if (LocalAppDark.current) 0.14f else 0.85f) * readingArtworkAlpha())
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.app_name), style = ledgerText(29.sp, FontWeight.SemiBold), maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false), overflow = TextOverflow.Ellipsis)
                    if (LocalDensity.current.fontScale <= 1.3f) Image(painterResource(R.drawable.song_abacus_seal), null, Modifier.size(22.dp))
                }
                IconButton(onSearch) { Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
                    if (searching) "关闭搜索" else "搜索备注", Modifier.size(25.dp)) }
                IconButton(onFilters) { Icon(Icons.Outlined.FilterList, "筛选记录", Modifier.size(25.dp),
                    tint = if (filtered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onPrevious) { Icon(Icons.Outlined.ChevronLeft, "上个月") }
                TextButton(onChoose, Modifier.widthIn(max = 220.dp).weight(1f, fill = false), contentPadding = PaddingValues(horizontal = 2.dp)) {
                    FittedLedgerAmount(DateUtils.monthName(month.year, month.monthValue), 20.sp,
                        Modifier.fillMaxWidth(), TextAlign.Center)
                }
                IconButton(onNext) { Icon(Icons.Outlined.ChevronRight, "下个月") }
            }
        }
    }
}

@Composable
private fun SongSummary(summary: MonthSummary, currency: String) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        val stacked = maxWidth < 310.dp || LocalDensity.current.fontScale > 1.3f
        val expense: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier) {
                Text("本月支出", style = ledgerText(16.sp), color = MaterialTheme.colorScheme.primary)
                FittedLedgerAmount(AmountFormatter.withSymbol(summary.expenseCents, currency), 44.sp,
                    Modifier.fillMaxWidth().padding(top = 6.dp), weight = FontWeight.SemiBold)
            }
        }
        val balance: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("收入", style = ledgerText(12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                FittedLedgerAmount(AmountFormatter.withSymbol(summary.incomeCents, currency), 18.sp, Modifier.fillMaxWidth(), weight = FontWeight.SemiBold)
                HorizontalDivider(Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text("结余", style = ledgerText(12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                FittedLedgerAmount(AmountFormatter.withSymbol(summary.balanceCents, currency), 18.sp, Modifier.fillMaxWidth(), weight = FontWeight.SemiBold)
            }
        }
        Column {
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                expense(Modifier.fillMaxWidth())
                balance(Modifier.fillMaxWidth())
            } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                expense(Modifier.weight(0.65f))
                Box(Modifier.weight(0.35f)) {
                    balance(Modifier.padding(start = 12.dp))
                    Box(Modifier.matchParentSize()) { VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant) }
                }
            }
            HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SongTypeBar(type: String?, onSelect: (String?) -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().selectableGroup().padding(top = 6.dp)) {
            listOf(null to "全部", BillType.EXPENSE to "支出", BillType.INCOME to "收入").forEach { (value, title) ->
                val selected = type == value
                Column(Modifier.weight(1f).selectable(selected, role = Role.Tab, onClick = { onSelect(value) })
                    .heightIn(min = 48.dp).padding(top = 8.dp, bottom = 1.dp), horizontalAlignment = Alignment.Start) {
                    Text(title, style = ledgerText(17.sp), color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.heightIn(min = 32.dp))
                    Box(Modifier.width(44.dp).height(3.dp).background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(2.dp)))
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SongDayHeader(row: HomeRow.Header, today: LocalDate) {
    val date = DateUtils.parse(row.date)
    val prefix = when (date) { today -> "今天 · "; today.minusDays(1) -> "昨天 · "; else -> "" }
    val label = "$prefix${if (date.year != today.year) "${date.year}年" else ""}${date.monthValue}月${date.dayOfMonth}日 ${DateUtils.weekdayLabel(date)}"
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = ledgerText(15.sp), modifier = Modifier.weight(if (LocalDensity.current.fontScale > 1.15f) 4f else 1f, fill = false))
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun SongBillRow(item: BillWithCategory, currency: String, incomeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit, modifier: Modifier = Modifier) {
    val income = item.bill.type == BillType.INCOME
    Column(modifier.fillMaxWidth().clickable(onClickLabel = "编辑这笔记录", onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(categoryIcon(item.category.icon), null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurface)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.category.name, style = ledgerText(15.sp, FontWeight.SemiBold).copy(platformStyle = PlatformTextStyle(includeFontPadding = false)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!item.bill.note.isNullOrBlank()) Text(item.bill.note, style = ledgerText(12.sp).copy(platformStyle = PlatformTextStyle(includeFontPadding = false)), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FittedLedgerAmount((if (income) "+" else "−") + AmountFormatter.withSymbol(item.bill.amountCents, currency), 19.sp,
                Modifier.weight(1.05f), TextAlign.End, if (income) incomeColor else MaterialTheme.colorScheme.onSurface, FontWeight.SemiBold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

/** 放在 Scaffold 底栏，给按钮和笔搁预留真实高度。 */
@Composable
internal fun SongCreateAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        if (LocalBackgroundSpec.current == BACKGROUND_SPEC_DEFAULT && LocalDensity.current.fontScale <= 1.3f) {
            Image(painterResource(R.drawable.song_brush_rest), null, Modifier.width(100.dp).height(42.dp),
                contentScale = ContentScale.Fit, alpha = (if (LocalAppDark.current) 0.55f else 1f) * readingArtworkAlpha())
            Spacer(Modifier.width(10.dp))
        }
        Surface(onClick, Modifier.widthIn(min = 148.dp).heightIn(min = 48.dp), shape = RoundedCornerShape(5.dp),
            color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Box {
                LedgerPaper(Modifier.matchParentSize(), 0.13f)
                Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(23.dp))
                    Text("记一笔", style = ledgerText(19.sp))
                }
            }
        }
    }
}
