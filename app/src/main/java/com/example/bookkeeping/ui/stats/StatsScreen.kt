package com.example.bookkeeping.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookkeeping.AppViewModelProvider
import com.example.bookkeeping.data.db.CategoryTotal
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.ui.chart.*
import com.example.bookkeeping.ui.common.*
import com.example.bookkeeping.ui.theme.AmountFontFeatures
import com.example.bookkeeping.ui.theme.LocalAppColors
import com.example.bookkeeping.util.AmountFormatter
import com.example.bookkeeping.util.CategoryIcons
import com.example.bookkeeping.util.DateUtils
import java.time.YearMonth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onOpenCategory: (Long, String, Int, Int?) -> Unit,
    viewModel: StatsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val monthly by viewModel.monthly.collectAsStateWithLifecycle()
    val annual by viewModel.annual.collectAsStateWithLifecycle()
    val years by viewModel.availableYears.collectAsStateWithLifecycle()
    val currency = AmountFormatter.symbolOf(settings.currency)
    val label = if (selection.type == BillType.INCOME) "收入" else "支出"
    val accent = if (selection.type == BillType.INCOME) LocalAppColors.current.income else MaterialTheme.colorScheme.primary
    var annualVisible by rememberSaveable { mutableStateOf(false) }
    var annualTab by rememberSaveable { mutableIntStateOf(0) }
    var pickerVisible by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = annualVisible) { annualVisible = false }

    if (pickerVisible) {
        if (!annualVisible) MonthPickerDialog(selection.month, viewModel::setYearMonth) { pickerVisible = false }
        else AlertDialog(
            onDismissRequest = { pickerVisible = false }, title = { Text("选择年份") },
            text = {
                LazyColumn {
                    items(years) { year ->
                        TextButton(onClick = { viewModel.setYear(year); pickerVisible = false }, modifier = Modifier.fillMaxWidth()) { Text("${year}年") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickerVisible = false }) { Text("关闭") } }
        )
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (annualVisible) "年度分析" else "收支分析") },
                navigationIcon = {
                    if (annualVisible) IconButton(onClick = { annualVisible = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回月度分析")
                    }
                },
                actions = {
                    if (!annualVisible) TextButton(onClick = { annualVisible = true }) { Text("年度分析") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selection.type == BillType.EXPENSE, onClick = { viewModel.setType(BillType.EXPENSE) }, label = { Text("支出") })
                FilterChip(selection.type == BillType.INCOME, onClick = { viewModel.setType(BillType.INCOME) }, label = { Text("收入") })
            }
            if (annualVisible) {
                TabRow(annualTab, containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("年度", "历年", "分类").forEachIndexed { index, title ->
                        Tab(annualTab == index, onClick = { annualTab = index }, text = { Text(title) })
                    }
                }
            }
            val monthData = monthly?.takeIf { it.selection.month == selection.month && it.selection.type == selection.type }
            val yearData = annual?.takeIf { it.year == selection.year && it.type == selection.type }
            if ((!annualVisible && monthData == null) || (annualVisible && yearData == null)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (annualVisible) viewModel.prevYear() else viewModel.prevMonth() }) {
                                        Icon(Icons.Filled.ChevronLeft, if (annualVisible) "上一年" else "上一月")
                                    }
                                    Text(
                                        if (annualVisible) "${selection.year}年" else DateUtils.monthName(selection.month.year, selection.month.monthValue),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f).clickable { pickerVisible = true }.padding(vertical = 12.dp)
                                    )
                                    IconButton(
                                        onClick = { if (annualVisible) viewModel.nextYear() else viewModel.nextMonth() },
                                        enabled = if (annualVisible) selection.year < YearMonth.now().year else selection.month < YearMonth.now()
                                    ) { Icon(Icons.Filled.ChevronRight, if (annualVisible) "下一年" else "下一月") }
                                }
                                if (!annualVisible && selection.month != YearMonth.now()) TextButton(onClick = viewModel::goCurrentMonth) { Text("回到本月") }
                            }
                        }
                    }
                    if (!annualVisible && monthData != null) {
                        item {
                            SectionCard("当月${label}") {
                                Text(AmountFormatter.withSymbol(monthData.total, currency),
                                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = AmountFontFeatures), color = accent)
                                Text("较上月 ${percentChange(monthData.total, monthData.previous) ?: "暂无可比数据"}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Text("${monthData.count} 笔记录", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricCard("日均${label}", AmountFormatter.withSymbol(monthData.total / DateUtils.daysPassed(selection.month.year, selection.month.monthValue).coerceAtLeast(1), currency), Modifier.weight(1f))
                                MetricCard("最大单笔", monthData.maximum?.let { AmountFormatter.withSymbol(it, currency) } ?: "—", Modifier.weight(1f))
                            }
                        }
                        item {
                            val shownDays = if (selection.month == YearMonth.now()) DateUtils.daysPassed(selection.month.year, selection.month.monthValue)
                                else selection.month.lengthOfMonth()
                            val byDay = monthData.daily.associate { it.label to it.totalCents }
                            val points = (1..shownDays).map { ChartPoint("$it", byDay[it] ?: 0L) }
                            var selectedDay by remember(selection.month, selection.type) { mutableIntStateOf(-1) }
                            SectionCard("每日${label}趋势") {
                                if (monthData.count == 0L) EmptyState("该月暂无${label}记录")
                                else {
                                    LineChart(listOf(ChartSeries(label, accent, points)), Modifier.fillMaxWidth().height(200.dp),
                                        onPointTap = { i, _ -> selectedDay = i })
                                    points.getOrNull(selectedDay)?.let {
                                        Text("${selection.month.monthValue}月${it.label}日 · ${AmountFormatter.withSymbol(it.value, currency)}",
                                            style = MaterialTheme.typography.bodyMedium, color = accent)
                                    }
                                }
                            }
                        }
                        item {
                            CategoryComposition(monthData.categories, currency, "分类排行") { category ->
                                onOpenCategory(category, selection.type, selection.month.year, selection.month.monthValue)
                            }
                        }
                    }
                    if (annualVisible && yearData != null) {
                        val total = yearData.months.sumOf { it.totalCents }
                        val count = yearData.months.sumOf { it.cnt }
                        item {
                            SectionCard("${selection.year}年${label}") {
                                Text(AmountFormatter.withSymbol(total, currency),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = AmountFontFeatures), color = accent)
                                val prev = yearData.years.firstOrNull { it.label == selection.year - 1 }?.totalCents ?: 0L
                                Text("较上一年 ${percentChange(total, prev) ?: "暂无可比数据"}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$count 笔记录", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (annualTab == 0) {
                            item {
                                var chartStyle by rememberSaveable { mutableIntStateOf(0) }
                                var highlighted by remember(selection.year, selection.type) { mutableIntStateOf(-1) }
                                val points = yearData.months.map { ChartPoint("${it.label}月", it.totalCents) }
                                SectionCard("每月${label}") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(chartStyle == 0, onClick = { chartStyle = 0 }, label = { Text("折线") })
                                        FilterChip(chartStyle == 1, onClick = { chartStyle = 1 }, label = { Text("柱状") })
                                    }
                                    if (count == 0) EmptyState("该年暂无${label}记录")
                                    else if (chartStyle == 0) {
                                        LineChart(listOf(ChartSeries(label, accent, points)), Modifier.fillMaxWidth().height(210.dp),
                                            onPointTap = { i, _ -> highlighted = i })
                                    } else {
                                        BarChart(points, Modifier.fillMaxWidth().height(210.dp), accent, highlighted,
                                            valueFormatter = { AmountFormatter.withSymbol(it, currency) }, onBarTap = { highlighted = it })
                                    }
                                    points.getOrNull(highlighted)?.let { Text("${it.label} · ${AmountFormatter.withSymbol(it.value, currency)}") }
                                }
                            }
                            item {
                                SectionCard("月度排行") {
                                    val max = yearData.months.maxOf { it.totalCents }.coerceAtLeast(1L)
                                    yearData.months.sortedByDescending { it.totalCents }.forEach { month ->
                                        Row(Modifier.fillMaxWidth().clickable {
                                            viewModel.setYearMonth(YearMonth.of(selection.year, month.label)); annualVisible = false
                                        }.padding(vertical = 8.dp)) {
                                            RankRow("${month.label}月", month.totalCents.toFloat() / max, accent, AmountFormatter.withSymbol(month.totalCents, currency))
                                        }
                                    }
                                }
                            }
                        } else if (annualTab == 1) {
                            item {
                                SectionCard("历年${label}趋势") {
                                    if (yearData.years.isEmpty()) EmptyState("暂无${label}记录")
                                    else LineChart(
                                        listOf(ChartSeries(label, accent, yearData.years.map { ChartPoint("${it.label}", it.totalCents) })),
                                        Modifier.fillMaxWidth().height(210.dp), onPointTap = { i, _ -> yearData.years.getOrNull(i)?.let { viewModel.setYear(it.label) } }
                                    )
                                }
                            }
                            item {
                                SectionCard("年度月均") {
                                    val months = if (selection.year == YearMonth.now().year) YearMonth.now().monthValue else 12
                                    Text(AmountFormatter.withSymbol(total / months, currency),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = AmountFontFeatures))
                                }
                            }
                        } else {
                            item {
                                SectionCard("分类逐年对比 · TOP5") {
                                    val rows = yearData.categoryYears
                                    val allYears = rows.map { it.year }.distinct().sorted()
                                    val top = rows.groupBy { it.categoryId }.values.sortedByDescending { list -> list.sumOf { it.totalCents } }.take(5)
                                    val series = top.map { list ->
                                        val first = list.first()
                                        ChartSeries(first.name, first.color.hexToColor(),
                                            allYears.map { y -> ChartPoint("$y", list.firstOrNull { it.year == y }?.totalCents ?: 0L) })
                                    }
                                    if (series.isEmpty()) EmptyState("暂无分类记录")
                                    else {
                                        LineChart(series, Modifier.fillMaxWidth().height(220.dp))
                                        series.forEach { Text(it.name, style = MaterialTheme.typography.bodySmall, color = it.color) }
                                    }
                                }
                            }
                        }
                        item {
                            CategoryComposition(yearData.categories, currency, "年度分类排行") { category ->
                                onOpenCategory(category, selection.type, selection.year, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryComposition(categories: List<CategoryTotal>, currency: String, title: String, onCategory: (Long) -> Unit) {
    SectionCard(title) {
        if (categories.isEmpty()) EmptyState("暂无分类记录")
        else {
            val total = categories.sumOf { it.totalCents }.coerceAtLeast(1L)
            val top = categories.take(6)
            val rest = categories.drop(6).sumOf { it.totalCents }
            val slices = top.map { DonutSlice(it.name, it.totalCents, it.color.hexToColor()) } +
                if (rest > 0L) listOf(DonutSlice("其他", rest, LocalAppColors.current.chartOther)) else emptyList()
            DonutChart(
                slices, Modifier.fillMaxWidth().height(180.dp),
                centerTitle = AmountFormatter.withSymbol(total, currency), centerSubtitle = "合计",
                onSliceTap = { i -> top.getOrNull(i)?.let { onCategory(it.categoryId) } }
            )
            Text("点击分类查看账单", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            categories.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().clickable(onClickLabel = "查看${c.name}账单") { onCategory(c.categoryId) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryIconBox(CategoryIcons.byKey(c.icon), c.color.hexToColor(), size = 36.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        Text("${String.format(Locale.CHINA, "%.1f", c.totalCents * 100.0 / total)}% · ${c.cnt} 笔",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(AmountFormatter.withSymbol(c.totalCents, currency),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = AmountFontFeatures))
                    Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
