package com.example.bookkeeping.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookkeeping.AppViewModelProvider
import com.example.bookkeeping.data.prefs.SettingsRepository
import com.example.bookkeeping.data.repo.*
import com.example.bookkeeping.ui.common.*
import com.example.bookkeeping.util.*
import kotlinx.coroutines.flow.*
import java.time.YearMonth

class CategoryBillsViewModel(handle: SavedStateHandle, bills: BillRepository, categories: CategoryRepository, settings: SettingsRepository) : ViewModel() {
    val categoryId = handle.get<Long>("categoryId") ?: -1L
    val type = handle.get<String>("type")?.takeIf { it == BillType.INCOME } ?: BillType.EXPENSE
    val year = handle.get<Int>("year") ?: YearMonth.now().year
    val month = handle.get<Int>("month")?.takeIf { it in 1..12 }
    val category = categories.observeAll().map { list -> list.firstOrNull { it.id == categoryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val records = bills.observeCategoryBills(categoryId, type, year, month)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = settings.settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBillsScreen(onBack: () -> Unit, onEdit: (Long) -> Unit,
    viewModel: CategoryBillsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val category by viewModel.category.collectAsStateWithLifecycle()
    val bills by viewModel.records.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currency = AmountFormatter.symbolOf(settings.currency)
    val label = if (viewModel.type == BillType.INCOME) "收入" else "支出"
    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(title = { Text(category?.name ?: "分类账单") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回统计") }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard("${viewModel.year}年${viewModel.month?.let { "${it}月" } ?: "全年"} · $label") {
                    Text(AmountFormatter.withSymbol(bills.sumOf { it.bill.amountCents }, currency), style = MaterialTheme.typography.headlineMedium)
                    Text("${bills.size} 笔记录", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (bills.isEmpty()) item { EmptyState("该期间暂无记录") }
            bills.groupBy { it.bill.date }.forEach { (date, records) ->
                item(key = "day:$date") { GroupHeader(DateUtils.groupLabel(date), "$label ${AmountFormatter.withSymbol(records.sumOf { it.bill.amountCents }, currency)}") }
                records.forEach { record ->
                    item(key = record.bill.id) {
                        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                            Row(Modifier.fillMaxWidth().clickable { onEdit(record.bill.id) }.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CategoryIconBox(CategoryIcons.byKey(record.category.icon), record.category.color.hexToColor())
                                Column(Modifier.weight(1f)) {
                                    Text(record.category.name, style = MaterialTheme.typography.bodyMedium)
                                    record.bill.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                Text(AmountFormatter.withSymbol(record.bill.amountCents, currency), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
