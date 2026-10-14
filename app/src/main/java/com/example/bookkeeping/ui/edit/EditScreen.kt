package com.example.bookkeeping.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookkeeping.AppViewModelProvider
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.ui.common.ConfirmDialog
import com.example.bookkeeping.ui.common.hexToColor
import com.example.bookkeeping.ui.theme.AmountFontFeatures
import com.example.bookkeeping.ui.theme.LocalAppColors
import com.example.bookkeeping.util.AmountFormatter
import com.example.bookkeeping.util.CategoryIcons
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    onBack: () -> Unit,
    onManageCategories: (String) -> Unit,
    onSaved: () -> Unit,
    onDeleteBill: (Long) -> Unit,
    deleting: Boolean = false,
    viewModel: EditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val display by viewModel.input.display.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val billType by viewModel.billType.collectAsStateWithLifecycle()
    val selected by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val saveResult by viewModel.saveResult.collectAsStateWithLifecycle()
    val datePickerVisible by viewModel.datePickerVisible.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()
    var leaving by remember { mutableStateOf(false) }
    // 离开只拦截操作，不改变最后一帧的按钮颜色与表单内容。
    val enabled = ready && !saving && !deleting
    val currency = AmountFormatter.symbolOf(settings.currency)
    val incomeColor = LocalAppColors.current.income
    val categories = remember(allCategories, selected, billType) {
        val list = allCategories.orEmpty().filter { !it.isArchived && it.type == billType }
        selected?.takeIf { it.type == billType && list.none { c -> c.id == it.id } }?.let { list + it } ?: list
    }
    val snackbar = remember { SnackbarHostState() }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var noteVisible by rememberSaveable { mutableStateOf(false) }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    val leavePage: (() -> Unit) -> Unit = { action ->
        if (!leaving) {
            leaving = true
            focusManager.clearFocus()
            softwareKeyboard?.hide()
            action()
        }
    }
    val requestBack: () -> Unit = {
        if (!saving && !deleting && !leaving) {
            if (viewModel.hasUnsavedChanges()) showDiscard = true else leavePage(onBack)
        }
    }
    BackHandler { requestBack() }

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(saveResult) {
        saveResult?.let {
            viewModel.consumeSaveResult()
            if (it == SaveResult.CLOSE) leavePage(onSaved)
        }
    }
    if (showDiscard) {
        ConfirmDialog(
            title = "放弃未保存的修改？", text = "这笔记录的修改尚未保存。",
            confirmLabel = "放弃修改", cancelLabel = "继续编辑",
            onConfirm = { showDiscard = false; leavePage(onBack) }, onDismiss = { showDiscard = false }
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "删除这笔记录？", text = "这笔记录将被删除，删除后无法撤销。", confirmLabel = "删除",
            onConfirm = { showDelete = false; onDeleteBill(viewModel.billId) },
            onDismiss = { showDelete = false }
        )
    }
    if (datePickerVisible) {
        val todayMillis = LocalDate.now().toUtcMillis()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date.toUtcMillis(),
            selectableDates = remember(todayMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                    override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.setDatePickerVisible(false) },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { viewModel.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    viewModel.setDatePickerVisible(false)
                }, enabled = picker.selectedDateMillis != null) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { viewModel.setDatePickerVisible(false) }) { Text("取消") } }
        ) {
            DatePicker(picker, title = {
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("今天" to LocalDate.now(), "昨天" to LocalDate.now().minusDays(1)).forEach { (label, quickDate) ->
                        TextButton(onClick = {
                            viewModel.setDate(quickDate)
                            viewModel.setDatePickerVisible(false)
                        }) { Text(label) }
                    }
                }
            })
        }
    }
    if (noteVisible) {
        val noteFocus = remember { FocusRequester() }
        val closeNote = {
            focusManager.clearFocus()
            softwareKeyboard?.hide()
            noteVisible = false
        }
        val confirmNote = {
            viewModel.setNote(noteDraft)
            closeNote()
        }
        AlertDialog(
            onDismissRequest = closeNote,
            title = { Text("添加备注") },
            text = {
                LaunchedEffect(Unit) { noteFocus.requestFocus() }
                OutlinedTextField(
                    value = noteDraft, onValueChange = { noteDraft = it.take(50) },
                    modifier = Modifier.fillMaxWidth().focusRequester(noteFocus),
                    placeholder = { Text("例如：午餐、交通、工资") }, minLines = 2, maxLines = 3,
                    supportingText = { Text("${noteDraft.length}/50") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirmNote() }),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = { TextButton(onClick = confirmNote) { Text("完成") } },
            dismissButton = { TextButton(onClick = closeNote) { Text("取消") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEdit) "编辑记录" else "记一笔") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                navigationIcon = {
                    IconButton(onClick = requestBack, enabled = !saving && !deleting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (viewModel.isEdit) IconButton(onClick = { if (!leaving) showDelete = true }, enabled = enabled) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "删除这笔记录", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (!ready) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!loadFailed) CircularProgressIndicator()
                else Text("记录暂时无法读取，请返回重试", modifier = Modifier.padding(20.dp))
            }
            return@Scaffold
        }
        Column(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize()) {
            // 金额与类型固定在上方，只有分类列表滚动。
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(BillType.EXPENSE to "支出", BillType.INCOME to "收入").forEach { (type, label) ->
                        FilterChip(
                            selected = billType == type, onClick = { if (!leaving) viewModel.selectType(type) },
                            enabled = enabled, label = { Text(label, style = MaterialTheme.typography.titleSmall) },
                            leadingIcon = if (billType == type) ({ Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }) else null
                        )
                    }
                }
                Text("金额", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(currency, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))
                    Text(
                        display,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = if (display.length > 10) 36.sp else 44.sp,
                            fontFeatureSettings = AmountFontFeatures
                        ),
                        color = if (billType == BillType.INCOME) incomeColor else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f), maxLines = 1
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("选择分类", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (!leaving) onManageCategories(billType) }, enabled = enabled) { Text("管理") }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(64.dp), modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { c ->
                    CategoryCell(c, selected?.id == c.id, enabled) { if (!leaving) viewModel.selectCategory(c) }
                }
                if (categories.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("暂无可用分类", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
            // 备注使用独立弹窗；底层键盘保持布局，避免弹窗与系统输入法引起整页跳动。
            AmountKeyboard(
                enabled = enabled && !noteVisible && !datePickerVisible,
                date = date, hasNote = note.isNotBlank(), isEdit = viewModel.isEdit,
                saving = saving, deleting = deleting,
                onDigit = { if (!leaving) viewModel.input.digit(it) }, onDot = { if (!leaving) viewModel.input.dot() },
                onBackspace = { if (!leaving) viewModel.input.backspace() },
                onDate = { if (!leaving) viewModel.setDatePickerVisible(true) },
                onNote = { if (!leaving) { noteDraft = note; noteVisible = true } },
                onContinueOrClear = {
                    if (!leaving) {
                        if (viewModel.isEdit) viewModel.input.reset() else viewModel.save(continueEntry = true)
                    }
                },
                onSave = { if (!leaving) viewModel.save() }
            )
        }
    }
}

@Composable
private fun CategoryCell(category: CategoryEntity, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = category.color.hexToColor()
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "categorySelection"
    )
    Surface(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(16.dp), color = background,
        modifier = Modifier.semantics { this.selected = selected },
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(CategoryIcons.byKey(category.icon), null, Modifier.size(26.dp), tint = if (selected) MaterialTheme.colorScheme.primary else color)
            Text(
                if (category.isArchived) "${category.name}·归档" else category.name,
                style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AmountKeyboard(
    enabled: Boolean,
    date: LocalDate,
    hasNote: Boolean,
    isEdit: Boolean,
    saving: Boolean,
    deleting: Boolean,
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onBackspace: () -> Unit,
    onDate: () -> Unit,
    onNote: () -> Unit,
    onContinueOrClear: () -> Unit,
    onSave: () -> Unit
) {
    val today = LocalDate.now()
    val dateLabel = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("MM/dd"))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"), listOf(".", "0", "back")).forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    Surface(
                        onClick = {
                            when (key) { "." -> onDot(); "back" -> onBackspace(); else -> onDigit(key[0]) }
                        },
                        enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 52.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp),
                        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ) {
                        Box(Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                            if (key == "back") Icon(Icons.AutoMirrored.Filled.Backspace, "退格，删除最后一位", Modifier.size(24.dp))
                            else Text(key, style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = AmountFontFeatures))
                        }
                    }
                }
                val label = when (index) {
                    0 -> dateLabel
                    1 -> "备注"
                    2 -> if (isEdit) "清零" else "再记"
                    else -> if (deleting) "删除中" else if (saving) "保存中" else "保存"
                }
                val subtitle = when (index) {
                    0 -> if (date.year != today.year) date.year.toString() else "日期"
                    1 -> if (hasNote) "已填写" else "选填"
                    2 -> if (isEdit) "重输金额" else "保存后继续"
                    else -> if (isEdit) "保存修改" else null
                }
                val description = when (index) {
                    0 -> "选择日期，当前为${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                    1 -> if (hasNote) "编辑备注，已填写" else "添加备注"
                    2 -> if (isEdit) "清空当前金额，重新输入" else "保存并再记"
                    else -> if (isEdit) "保存修改" else "保存"
                }
                val primary = index == 3
                Surface(
                    onClick = when (index) { 0 -> onDate; 1 -> onNote; 2 -> onContinueOrClear; else -> onSave },
                    enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp).fillMaxHeight().semantics { contentDescription = description },
                    color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = (if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer)
                        .copy(alpha = if (enabled) 1f else 0.38f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    }
                }
            }
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
