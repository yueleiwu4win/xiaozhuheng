package com.example.bookkeeping.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookkeeping.R
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.ui.common.EmptyState
import com.example.bookkeeping.ui.common.categoryIcon
import com.example.bookkeeping.util.AmountFormatter
import com.example.bookkeeping.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

private val LocalInkGoldWide = staticCompositionLocalOf { false }

/** 墨金账页独立组织信息，复用原有查询、编辑、删除逻辑；照片背景照常透出。 */
@Composable
internal fun InkGoldHomeContent(
    yearMonth: YearMonth, rows: List<HomeRow>, summary: MonthSummary, filteredSummary: MonthSummary,
    filter: MonthFilter, categories: List<CategoryEntity>, currency: String,
    onKeyword: (String) -> Unit, onFilters: () -> Unit,
    onPrevious: () -> Unit, onNext: () -> Unit, onChooseMonth: () -> Unit, onCurrentMonth: () -> Unit,
    onCategory: (Long?) -> Unit, onReset: () -> Unit, onCreate: () -> Unit,
    today: LocalDate, billContent: @Composable (BillWithCategory) -> Unit
) {
    // 整个账页可以滚动，横屏/大字体下不会由固定头部挤没账单。
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 520.dp
        CompositionLocalProvider(LocalInkGoldWide provides wide) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = if (wide) 32.dp else 20.dp, vertical = if (wide) 16.dp else 10.dp)) {
                item("masthead") {
                    InkGoldMasthead(yearMonth, filter, onKeyword, onFilters, onPrevious, onNext, onChooseMonth)
                }
                item("overview") { InkGoldSummary(summary, currency) }
                if (yearMonth != YearMonth.from(today)) item("currentMonth") {
                    TextButton(onCurrentMonth) { Text("回到本月") }
                }
                item("categories") { InkGoldCategories(categories, filter, onCategory) }
                if (filter.isActive) item("filterSummary") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val type = when (filter.type) { BillType.INCOME -> "收入"; BillType.EXPENSE -> "支出"; else -> "全部收支" }
                        Column(Modifier.weight(1f)) {
                            Text("$type · ${filteredSummary.expenseCount + filteredSummary.incomeCount} 笔", style = ledgerText(12.sp))
                            Text("支出 ${ledgerMoney(filteredSummary.expenseCents, currency)} · 收入 ${ledgerMoney(filteredSummary.incomeCents, currency)}",
                                style = ledgerText(11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onReset) { Text("重置", style = ledgerText(12.sp)) }
                    }
                }
                if (rows.isEmpty()) item("empty") {
                    EmptyState(if (filter.isActive) "没有符合条件的记录\n试试更换分类或搜索词" else "本月还没有记录\n从第一笔收支开始", Icons.Outlined.ReceiptLong)
                    TextButton(if (filter.isActive) onReset else onCreate, Modifier.fillMaxWidth()) {
                        Text(if (filter.isActive) "清除筛选" else "记第一笔")
                    }
                }
                items(rows, key = { it.key }, contentType = { if (it is HomeRow.Header) "header" else "bill" }) {
                    when (it) {
                        is HomeRow.Header -> InkGoldDayHeader(it, currency, today)
                        is HomeRow.Item -> billContent(it.item)
                    }
                }
            }
        }
    }
}

@Composable
private fun InkGoldMasthead(month: YearMonth, filter: MonthFilter, onKeyword: (String) -> Unit,
    onFilters: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onChoose: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = if (LocalInkGoldWide.current) 0.dp else 8.dp)) {
        val stacked = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.25f
        val brand: @Composable (Modifier) -> Unit = { modifier ->
            Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.app_name), style = ledgerText(if (LocalInkGoldWide.current) 43.sp else if (stacked) 30.sp else 29.sp, FontWeight.SemiBold))
                Image(painterResource(R.drawable.ledger_abacus_mark), null, Modifier.weight(1f).height(if (LocalInkGoldWide.current) 36.dp else 26.dp), contentScale = ContentScale.Fit)
            }
        }
        val tools: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onPrevious, Modifier.size(48.dp)) { Icon(Icons.Outlined.ChevronLeft, "上个月", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
                    Column(Modifier.weight(1f).clickable(role = Role.Button, onClickLabel = "选择月份", onClick = onChoose)
                        .heightIn(min = 48.dp), verticalArrangement = Arrangement.Center) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        FittedLedgerAmount("${month.year} 年 ${month.monthValue} 月", if (LocalInkGoldWide.current) 17.sp else 14.sp,
                            Modifier.fillMaxWidth().padding(vertical = 5.dp), TextAlign.Center)
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    IconButton(onNext, Modifier.size(48.dp)) { Icon(Icons.Outlined.ChevronRight, "下个月", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                InkGoldSearch(filter, onKeyword, onFilters)
            }
        }
        if (stacked) Column {
            brand(Modifier.fillMaxWidth().padding(end = 68.dp, bottom = 4.dp))
            tools(Modifier.fillMaxWidth())
        } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            brand(Modifier.weight(if (LocalInkGoldWide.current) 0.56f else 0.45f))
            tools(Modifier.weight(if (LocalInkGoldWide.current) 0.44f else 0.55f))
        }
    }
}

@Composable
private fun InkGoldSearch(filter: MonthFilter, onKeyword: (String) -> Unit, onFilters: () -> Unit) {
    val focus = LocalFocusManager.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicTextField(filter.keyword, onKeyword, Modifier.weight(1f).heightIn(min = 48.dp)
            .semantics { contentDescription = "搜索本月备注" }, singleLine = true,
            textStyle = ledgerText(12.sp).copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            decorationBox = { field ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.weight(1f)) {
                        if (filter.keyword.isEmpty()) Text("搜索记录…", style = ledgerText(12.sp), maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        field()
                    }
                }
            })
        IconButton({ focus.clearFocus(); onFilters() }, Modifier.size(48.dp)) {
            Box(Modifier.size(34.dp).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.FilterList, "筛选记录", Modifier.size(22.dp), tint = if (filter.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InkGoldSummary(summary: MonthSummary, currency: String) {
    val gold = MaterialTheme.colorScheme.primary
    val wide = LocalInkGoldWide.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = if (wide) 0.dp else 8.dp)) {
        val stacked = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.25f
        Column {
            Text("本月支出", style = ledgerText(if (wide) 18.sp else 14.sp), color = gold)
            val expense: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier) {
                    HorizontalDivider(Modifier.padding(top = 5.dp, bottom = 8.dp), 1.dp, gold)
                    InkGoldHeadline(summary.expenseCents, currency, if (wide) 56f else if (stacked) 40f else 38f)
                }
            }
            val balances: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier) {
                    HorizontalDivider(Modifier.padding(top = 5.dp, bottom = 12.dp), 1.dp, gold)
                    Box {
                        Column(Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InkGoldBalanceLine("本月收入", summary.incomeCents, currency)
                            InkGoldBalanceLine("本月结余", summary.balanceCents, currency)
                        }
                        Box(Modifier.matchParentSize()) {
                            VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                expense(Modifier.fillMaxWidth())
                balances(Modifier.fillMaxWidth())
            } else Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                expense(Modifier.weight(0.56f))
                balances(Modifier.weight(0.44f))
            }
        }
    }
}

/** 数字与单位共用基线，单位紧跟金额；长金额按实际宽度缩放。 */
@Composable
private fun InkGoldHeadline(cents: Long, currency: String, preferredSize: Float) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        fun label(size: Float) = buildAnnotatedString {
            append(if (currency == "¥") AmountFormatter.format(cents) else AmountFormatter.withSymbol(cents, currency))
            if (currency == "¥") withStyle(SpanStyle(fontSize = (size * 0.33f).coerceAtLeast(10f).sp)) { append(" 元") }
        }
        val size = remember(cents, currency, preferredSize, constraints.maxWidth, density.fontScale, density.density, measurer) {
            var low = 10f
            var high = preferredSize
            repeat(7) {
                val next = (low + high) / 2
                if (measurer.measure(label(next), ledgerText(next.sp), softWrap = false).size.width <= constraints.maxWidth) low = next
                else high = next
            }
            low
        }
        Text(label(size), style = ledgerText(size.sp).copy(lineHeight = size.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)))
    }
}

@Composable
private fun InkGoldBalanceLine(label: String, cents: Long, currency: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = ledgerText(if (LocalInkGoldWide.current) 14.sp else 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FittedLedgerAmount(ledgerMoney(cents, currency), if (LocalInkGoldWide.current) 17.sp else 14.sp, Modifier.weight(1f), TextAlign.End)
    }
}

@Composable
private fun InkGoldCategories(categories: List<CategoryEntity>, filter: MonthFilter, onCategory: (Long?) -> Unit) {
    Column {
        LazyRow(Modifier.fillMaxWidth().selectableGroup()) {
            item("all") { InkGoldCategoryTab("全部", filter.categoryId == null) { onCategory(null) } }
            items(categories, key = { it.id }) { category ->
                InkGoldCategoryTab(category.name, filter.categoryId == category.id) { onCategory(category.id) }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun InkGoldCategoryTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.widthIn(min = if (LocalInkGoldWide.current) 67.dp else 52.dp).selectable(selected, role = Role.Tab, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.heightIn(min = 42.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, style = ledgerText(14.sp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.width(48.dp), 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        Box(Modifier.height(7.dp)) {
            if (selected) Icon(Icons.Filled.Bookmark, null, Modifier.requiredSize(13.dp).offset(y = (-3).dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun InkGoldDayHeader(row: HomeRow.Header, currency: String, today: LocalDate) {
    val date = DateUtils.parse(row.date)
    Column(Modifier.padding(top = if (LocalInkGoldWide.current) 10.dp else 13.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${if (date.year != today.year) "${date.year}年 " else ""}${date.monthValue} 月 ${date.dayOfMonth} 日　${DateUtils.weekdayLabel(date)}",
                Modifier.weight(1f), style = ledgerText(13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(horizontalAlignment = Alignment.End) {
                if (row.summary.expenseCount > 0) Text("− ${ledgerMoney(row.summary.expenseCents, currency)}", style = ledgerText(13.sp))
                if (row.summary.incomeCount > 0) Text("+ ${ledgerMoney(row.summary.incomeCents, currency)}", style = ledgerText(13.sp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun InkGoldBillRow(item: BillWithCategory, currency: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val income = item.bill.type == BillType.INCOME
    Column(modifier.fillMaxWidth().clickable(onClickLabel = "编辑这笔记录", onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = if (LocalInkGoldWide.current) 48.dp else 54.dp).padding(vertical = if (LocalInkGoldWide.current) 6.dp else 7.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(item.category.icon), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.bill.note?.takeIf { it.isNotBlank() } ?: item.category.name,
                    style = ledgerText(14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (item.bill.note.isNullOrBlank()) { if (income) "收入" else "支出" } else item.category.name,
                    style = ledgerText(11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FittedLedgerAmount((if (income) "+ " else "− ") + ledgerMoney(item.bill.amountCents, currency), 16.sp,
                Modifier.weight(0.9f), TextAlign.End)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

internal fun ledgerMoney(cents: Long, currency: String): String =
    if (currency == "¥") "${AmountFormatter.format(cents)} 元" else AmountFormatter.withSymbol(cents, currency)
