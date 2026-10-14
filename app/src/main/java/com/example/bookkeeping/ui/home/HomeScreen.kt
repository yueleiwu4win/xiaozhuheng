package com.example.bookkeeping.ui.home

import com.example.bookkeeping.ui.theme.LocalThemeTokens
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookkeeping.AppViewModelProvider
import com.example.bookkeeping.R
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.ui.common.*
import com.example.bookkeeping.ui.theme.AmountFontFeatures
import com.example.bookkeeping.ui.theme.LocalAppColors
import com.example.bookkeeping.util.AmountFormatter
import com.example.bookkeeping.util.CategoryIcons
import com.example.bookkeeping.util.DateUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val SWIPE_REVEAL_DP = 84

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onEditBill: (Long?) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val yearMonth by viewModel.yearMonth.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val summary by viewModel.monthSummary.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val currency = AmountFormatter.symbolOf(settings.currency)
    val incomeColor = LocalAppColors.current.income
    var monthPickerVisible by rememberSaveable { mutableStateOf(false) }
    var filtersVisible by rememberSaveable { mutableStateOf(false) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var revealedBillId by remember { mutableStateOf(0L) }
    var deleteTarget by remember { mutableStateOf<BillWithCategory?>(null) }
    val focusManager = LocalFocusManager.current
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(bills, yearMonth, filter) { revealedBillId = 0L }
    LaunchedEffect(searchVisible) { if (searchVisible) searchFocus.requestFocus() }

    if (monthPickerVisible) {
        MonthPickerDialog(yearMonth, viewModel::setYearMonth) { monthPickerVisible = false }
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除这笔记录？", text = "这笔记录将被删除，删除后无法撤销。", confirmLabel = "删除",
            onConfirm = { deleteTarget = null; viewModel.deleteBill(target.bill.id) },
            onDismiss = { deleteTarget = null }
        )
    }
    if (filtersVisible) {
        ModalBottomSheet(onDismissRequest = { filtersVisible = false }, containerColor = if (LocalThemeTokens.current.ledgerRows) Color.Transparent else MaterialTheme.colorScheme.surface) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("筛选记录", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::resetFilters) { Text("重置") }
                    }
                }
                item {
                    Text("收支类型", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeFilters(filter.type, viewModel::setTypeFilter)
                    }
                }
                item {
                    Text("分类", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppFilterChip(
                            selected = filter.categoryId == null,
                            onClick = { viewModel.setCategoryFilter(null) }, label = { Text("全部分类") }
                        )
                        categories.filter { filter.type == null || it.type == filter.type }.forEach { c ->
                            AppFilterChip(
                                selected = filter.categoryId == c.id,
                                onClick = { viewModel.setCategoryFilter(if (filter.categoryId == c.id) null else c.id) },
                                label = { Text(c.name) },
                                leadingIcon = {
                                    Icon(categoryIcon(c.icon), null, Modifier.size(18.dp), tint = c.color.hexToColor())
                                }
                            )
                        }
                    }
                }
                item {
                    AppButton(onClick = { filtersVisible = false }, modifier = Modifier.fillMaxWidth()) { Text("查看结果") }
                }
            }
        }
    }

    val rows = remember(bills) {
        bills.groupBy { it.bill.date }.flatMap { (day, entries) ->
            listOf<HomeRow>(HomeRow.Header(day, summarizeBills(entries))) + entries.map { HomeRow.Item(it) }
        }
    }
    val filteredSummary = remember(bills) { summarizeBills(bills) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                focusManager.clearFocus()
                if (searchVisible) viewModel.setKeyword("")
                searchVisible = !searchVisible
            }) {
                Icon(if (searchVisible) themedIcon(Icons.Filled.Close, Icons.Outlined.Close) else themedIcon(Icons.Filled.Search, Icons.Outlined.Search),
                    contentDescription = if (searchVisible) "关闭搜索" else "搜索备注")
            }
            IconButton(onClick = { focusManager.clearFocus(); filtersVisible = true }) {
                Icon(themedIcon(Icons.Filled.FilterList, Icons.Outlined.FilterList), contentDescription = "筛选记录",
                    tint = if (filter.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (searchVisible) {
            OutlinedTextField(
                value = filter.keyword, onValueChange = viewModel::setKeyword,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).focusRequester(searchFocus),
                placeholder = { Text("搜索本月备注") }, singleLine = true,
                shape = RoundedCornerShape(LocalThemeTokens.current.rowCorner),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                leadingIcon = { Icon(themedIcon(Icons.Filled.Search, Icons.Outlined.Search), null) },
                trailingIcon = {
                    if (filter.keyword.isNotEmpty()) IconButton(onClick = { viewModel.setKeyword("") }) {
                        Icon(themedIcon(Icons.Filled.Close, Icons.Outlined.Close), contentDescription = "清空关键词")
                    }
                }
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "overview") {
                Card(
                    shape = RoundedCornerShape(LocalThemeTokens.current.overviewCorner),
                    colors = CardDefaults.cardColors(containerColor = if (LocalThemeTokens.current.ledgerRows) Color.Transparent else MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MonthSwitcher(
                            DateUtils.monthName(yearMonth.year, yearMonth.monthValue),
                            viewModel::prevMonth, viewModel::nextMonth,
                            onLabelClick = { monthPickerVisible = true }
                        )
                        Text("本月支出 · ${summary.expenseCount} 笔", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(currency, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp))
                            Text(AmountFormatter.format(summary.expenseCents),
                                style = MaterialTheme.typography.displayMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = AmountFontFeatures),
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        // 纵向允许换行，最大金额和大字体不会挤掉另一项。
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SummaryValue("收入 · ${summary.incomeCount} 笔", summary.incomeCents, currency, Modifier.weight(1f), incomeColor)
                            SummaryValue("结余 · 收入减支出", summary.balanceCents, currency, Modifier.weight(1f))
                        }
                        if (yearMonth != java.time.YearMonth.now()) {
                            TextButton(onClick = { viewModel.setYearMonth(java.time.YearMonth.now()) }) { Text("回到本月") }
                        }
                    }
                }
            }
            item(key = "types") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    TypeFilters(filter.type, viewModel::setTypeFilter)
                    filter.categoryId?.let { id ->
                        InputChip(
                            selected = true, onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text(categories.firstOrNull { it.id == id }?.name ?: "已选分类") },
                            trailingIcon = {
                                Icon(themedIcon(Icons.Filled.Close, Icons.Outlined.Close), contentDescription = "清除分类", modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
            if (filter.isActive) {
                item(key = "filtered") {
                    Surface(shape = RoundedCornerShape(LocalThemeTokens.current.rowCorner), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("筛选结果 · ${bills.size} 笔", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.resetFilters(); focusManager.clearFocus() }) { Text("清除筛选") }
                            }
                            Text(
                                "支出 ${AmountFormatter.withSymbol(filteredSummary.expenseCents, currency)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = AmountFontFeatures)
                            )
                            Text(
                                "收入 ${AmountFormatter.withSymbol(filteredSummary.incomeCents, currency)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = AmountFontFeatures)
                            )
                        }
                    }
                }
            }
            if (bills.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        if (filter.isActive) "没有符合条件的记录\n试试更换关键词或清除筛选" else "本月还没有记录\n从第一笔收支开始",
                        themedIcon(Icons.Filled.Receipt, Icons.Outlined.Receipt)
                    )
                    if (!filter.isActive) TextButton(onClick = { onEditBill(null) }, modifier = Modifier.fillMaxWidth()) { Text("记第一笔") }
                }
            }
            items(rows, key = { it.key }, contentType = { if (it is HomeRow.Header) "header" else "bill" }) { row ->
                when (row) {
                    is HomeRow.Header -> Row(
                        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(DateUtils.groupLabel(row.date), style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            if (row.summary.expenseCount > 0) Text(
                                "支出 ${AmountFormatter.withSymbol(row.summary.expenseCents, currency)}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (row.summary.incomeCount > 0) Text(
                                "收入 ${AmountFormatter.withSymbol(row.summary.incomeCents, currency)}",
                                style = MaterialTheme.typography.labelSmall, color = incomeColor
                            )
                        }
                    }
                    is HomeRow.Item -> SwipeToRevealRow(
                        row.item, currency, incomeColor, revealedBillId == row.item.bill.id,
                        onRevealedChange = { open -> revealedBillId = if (open) row.item.bill.id else 0L },
                        onClick = {
                            focusManager.clearFocus()
                            if (revealedBillId == row.item.bill.id) revealedBillId = 0L
                            else onEditBill(row.item.bill.id)
                        },
                        onDelete = { if (!deleting) deleteTarget = row.item }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, cents: Long, currency: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(AmountFormatter.withSymbol(cents, currency),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = AmountFontFeatures), color = color)
    }
}

@Composable
private fun TypeFilters(type: String?, onSelect: (String?) -> Unit) {
    listOf(null to "全部", BillType.EXPENSE to "支出", BillType.INCOME to "收入").forEach { (value, label) ->
        AppFilterChip(selected = type == value, onClick = { onSelect(value) }, label = { Text(label) })
    }
}

sealed interface HomeRow {
    val key: String
    data class Header(val date: String, val summary: MonthSummary) : HomeRow {
        override val key: String get() = "h:$date"
    }
    data class Item(val item: BillWithCategory) : HomeRow {
        override val key: String get() = "b:${item.bill.id}"
    }
}

@Composable
private fun SwipeToRevealRow(
    bill: BillWithCategory,
    currency: String,
    incomeColor: Color,
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val revealPx = with(LocalDensity.current) { SWIPE_REVEAL_DP.dp.toPx() }
    var dragging by remember(bill.bill.id) { mutableStateOf(false) }
    var dragOffset by remember(bill.bill.id) { mutableFloatStateOf(0f) }
    val offset by animateFloatAsState(
        targetValue = if (dragging) dragOffset else if (revealed) -revealPx else 0f,
        animationSpec = if (dragging) snap() else tween(180), label = "billSwipe"
    )
    val deleteWidth = with(LocalDensity.current) { (-offset).coerceIn(0f, revealPx).toDp() }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(LocalThemeTokens.current.rowCorner))) {
        BillRow(
            bill, currency, incomeColor, onClick,
            modifier = Modifier.offset { IntOffset(offset.roundToInt(), 0) }
                .pointerInput(bill.bill.id, revealed, revealPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragOffset = offset; dragging = true },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragOffset = (dragOffset + amount).coerceIn(-revealPx, 0f)
                        },
                        onDragEnd = { onRevealedChange(dragOffset < -revealPx / 2); dragging = false },
                        onDragCancel = { dragging = false }
                    )
                }
        )
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            if (deleteWidth > 0.dp) {
                Box(
                    Modifier.width(deleteWidth).fillMaxHeight().clipToBounds()
                        .background(LocalAppColors.current.swipeDelete)
                        .clickable(onClickLabel = "删除这笔记录", onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Text("删除", color = Color.White, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun BillRow(
    item: BillWithCategory,
    currency: String,
    incomeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = item.bill.type == BillType.INCOME
    Row(
        modifier.fillMaxWidth().background(if (LocalThemeTokens.current.ledgerRows) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface)
            .ledgerDivider().clickable(onClickLabel = "编辑这笔记录", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryIconBox(categoryIcon(item.category.icon), item.category.color.hexToColor(), size = 40.dp, corner = 12.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.category.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!item.bill.note.isNullOrBlank()) Text(
                item.bill.note, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            (if (isIncome) "+" else "−") + AmountFormatter.withSymbol(item.bill.amountCents, currency),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = AmountFontFeatures),
            color = if (isIncome) incomeColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 160.dp)
        )
    }
}

