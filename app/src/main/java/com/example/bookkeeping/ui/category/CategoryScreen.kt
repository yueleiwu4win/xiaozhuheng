package com.example.bookkeeping.ui.category

import com.example.bookkeeping.ui.theme.LocalReadingOptions
import com.example.bookkeeping.ui.theme.LocalThemeTokens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookkeeping.AppViewModelProvider
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.ui.common.*
import com.example.bookkeeping.util.*

private data class CategoryEditor(val category: CategoryEntity? = null, val type: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBack: () -> Unit, initialType: String = BillType.EXPENSE,
    viewModel: CategoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val all by viewModel.all.collectAsStateWithLifecycle()
    val counts by viewModel.billCounts.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var type by rememberSaveable { mutableStateOf(initialType.takeIf { it == BillType.INCOME } ?: BillType.EXPENSE) }
    var archivedVisible by rememberSaveable(type) { mutableStateOf(false) }
    var editor by remember { mutableStateOf<CategoryEditor?>(null) }
    var deleteTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var draggedId by remember(type) { mutableStateOf<Long?>(null) }
    var dragCenter by remember(type) { mutableFloatStateOf(0f) }
    val active = remember(all, type) { all.filter { it.type == type && !it.isArchived } }
    val archived = remember(all, type) { all.filter { it.type == type && it.isArchived } }
    var order by remember(type) { mutableStateOf(active.map { it.id }) }
    val byId = remember(active) { active.associateBy { it.id } }
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val margin = with(LocalDensity.current) { 64.dp.toPx() }

    BackHandler(enabled = busy || draggedId != null) {
        if (draggedId != null) { draggedId = null; order = active.map { it.id } }
    }

    LaunchedEffect(active) { if (draggedId == null) order = active.map { it.id } }
    LaunchedEffect(message, editor) {
        if (editor == null) message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }
    val moveToPointer: () -> Unit = {
        val id = draggedId
        if (id != null) {
            val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                it.key is Long && it.key != id && dragCenter >= it.offset && dragCenter < it.offset + it.size
            }?.key as? Long
            if (target != null) order = moveCategoryId(order, order.indexOf(id), order.indexOf(target))
        }
    }
    LaunchedEffect(draggedId) {
        while (draggedId != null) {
            val layout = listState.layoutInfo
            val scroll = when {
                dragCenter < layout.viewportStartOffset + margin -> -12f
                dragCenter > layout.viewportEndOffset - margin -> 12f
                else -> 0f
            }
            if (scroll != 0f) { listState.scrollBy(scroll); moveToPointer() }
            withFrameNanos { }
        }
    }

    Scaffold(
        containerColor = if (LocalReadingOptions.current.enhanced) MaterialTheme.colorScheme.background.copy(alpha = 0.95f) else Color.Transparent, snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = { Text("分类管理") }, navigationIcon = {
                IconButton(onClick = onBack, enabled = !busy && draggedId == null) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            }, actions = {
                TextButton(onClick = { viewModel.consumeMessage(); editor = CategoryEditor(type = type) }, enabled = !busy && draggedId == null) { Text("新增") }
            })
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFilterChip(type == BillType.EXPENSE, onClick = { type = BillType.EXPENSE }, enabled = !busy && draggedId == null, label = { Text("支出") })
                AppFilterChip(type == BillType.INCOME, onClick = { type = BillType.INCOME }, enabled = !busy && draggedId == null, label = { Text("收入") })
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(
                state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = draggedId == null
            ) {
                item(key = "hint") {
                    Text("启用中 · ${active.size} 个\n长按右侧拖动柄排序，也可在更多菜单中调整",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp))
                }
                items(order.mapNotNull { byId[it] }, key = { it.id }) { c ->
                    CategoryRow(
                        c, counts[c.id] ?: 0, enabled = !busy && draggedId == null,
                        onEdit = { viewModel.consumeMessage(); editor = CategoryEditor(c, type) },
                        onArchive = { viewModel.toggleArchive(c.id) }, onDelete = { deleteTarget = c },
                        onMove = { direction ->
                            viewModel.reorder(type, moveCategoryId(order, order.indexOf(c.id), order.indexOf(c.id) + direction))
                        },
                        canMoveUp = order.indexOf(c.id) > 0, canMoveDown = order.indexOf(c.id) < order.lastIndex,
                        modifier = Modifier.zIndex(if (draggedId == c.id) 1f else 0f).graphicsLayer {
                            val item = if (draggedId == c.id) listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == c.id } else null
                            translationY = if (item != null) dragCenter - item.offset - item.size / 2f else 0f
                        },
                        dragHandle = {
                            Box(
                                Modifier.size(48.dp).pointerInput(c.id, busy) {
                                    if (!busy) detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == c.id }?.let {
                                                draggedId = c.id; dragCenter = it.offset + it.size / 2f
                                            }
                                        },
                                        onDrag = { change, amount -> change.consume(); dragCenter += amount.y; moveToPointer() },
                                        onDragEnd = {
                                            draggedId = null
                                            if (order != active.map { it.id }) viewModel.reorder(type, order) { order = active.map { it.id } }
                                        },
                                        onDragCancel = { draggedId = null; order = active.map { it.id } }
                                    )
                                }, contentAlignment = Alignment.Center
                            ) { Icon(themedIcon(Icons.Filled.DragHandle, Icons.Outlined.DragHandle), "长按拖动排序${c.name}", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    )
                }
                if (active.isEmpty()) item(key = "empty") { EmptyState("暂无启用分类，点击右上角新增") }
                if (archived.isNotEmpty()) {
                    item(key = "archived") {
                        TextButton(onClick = { archivedVisible = !archivedVisible }, enabled = !busy && draggedId == null) {
                            Icon(if (archivedVisible) themedIcon(Icons.Filled.ExpandLess, Icons.Outlined.ExpandLess) else themedIcon(Icons.Filled.ExpandMore, Icons.Outlined.ExpandMore), null)
                            Text("已归档 · ${archived.size} 个")
                        }
                    }
                    if (archivedVisible) items(archived, key = { "archive:${it.id}" }) { c ->
                        CategoryRow(c, counts[c.id] ?: 0, !busy && draggedId == null,
                            onEdit = { viewModel.consumeMessage(); editor = CategoryEditor(c, type) },
                            onArchive = { viewModel.toggleArchive(c.id) }, onDelete = { deleteTarget = c })
                    }
                }
            }
        }
    }
    editor?.let { current ->
        val c = current.category
        CategoryEditDialog(
            initialName = c?.name ?: "", initialIcon = c?.icon ?: CategoryIcons.ALL.first().key,
            initialColor = c?.color ?: CATEGORY_COLORS.first(),
            title = if (c == null) "新增${if (current.type == BillType.INCOME) "收入" else "支出"}分类" else "编辑分类",
            busy = busy, errorText = message,
            onConfirm = { name, icon, color ->
                if (c == null) viewModel.create(current.type, name, icon, color) { editor = null }
                else viewModel.save(c.id, name, icon, color) { editor = null }
            },
            onDismiss = { if (!busy) { editor = null; viewModel.consumeMessage() } }
        )
    }
    deleteTarget?.let { c ->
        val mustArchive = c.isPreset || (counts[c.id] ?: 0) > 0
        ConfirmDialog(
            title = if (mustArchive) "归档「${c.name}」？" else "删除「${c.name}」？",
            text = if (mustArchive) "内置分类或已有账单的分类将归档，历史账单和统计保留。" else "该分类没有账单，删除后无法恢复。",
            confirmLabel = if (mustArchive) "归档" else "删除",
            onConfirm = { deleteTarget = null; viewModel.delete(c.id) }, onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity, billCount: Int, enabled: Boolean,
    onEdit: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: ((Int) -> Unit)? = null, canMoveUp: Boolean = false, canMoveDown: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null
) {
    var menu by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(LocalThemeTokens.current.rowCorner), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().ledgerDivider().clickable(enabled = enabled, onClick = onEdit).padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            CategoryIconBox(categoryIcon(category.icon), category.color.hexToColor())
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${if (category.isPreset) "内置 · " else ""}$billCount 笔", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }, enabled = enabled) { Icon(themedIcon(Icons.Filled.MoreVert, Icons.Outlined.MoreVert), "更多操作：${category.name}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("编辑") }, onClick = { menu = false; onEdit() })
                    if (onMove != null) {
                        DropdownMenuItem(text = { Text("上移") }, enabled = canMoveUp, onClick = { menu = false; onMove(-1) })
                        DropdownMenuItem(text = { Text("下移") }, enabled = canMoveDown, onClick = { menu = false; onMove(1) })
                    }
                    DropdownMenuItem(text = { Text(if (category.isArchived) "恢复分类" else "归档") }, onClick = { menu = false; onArchive() })
                    if (!category.isArchived || !category.isPreset && billCount == 0) {
                        DropdownMenuItem(text = { Text("删除", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            dragHandle?.invoke()
        }
    }
}

@Composable
private fun CategoryEditDialog(
    initialName: String,
    initialIcon: String,
    initialColor: String,
    title: String,
    busy: Boolean,
    errorText: String?,
    onConfirm: (name: String, icon: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var color by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(10) },
                    label = { Text("名称（10 字以内）") },
                    singleLine = true,
                    enabled = !busy,
                    isError = errorText != null,
                    supportingText = { errorText?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("图标", style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(48.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(CategoryIcons.ALL) { opt ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(LocalThemeTokens.current.pickerCorner))
                                .background(
                                    if (icon == opt.key) color.hexToColor().copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable(enabled = !busy) { icon = opt.key }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon(opt.key),
                                contentDescription = opt.label,
                                modifier = Modifier.size(22.dp),
                                tint = if (icon == opt.key) color.hexToColor()
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text("颜色", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CATEGORY_COLORS.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c.hexToColor())
                                .clickable(enabled = !busy) { color = c },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == c) {
                                Icon(
                                    themedIcon(Icons.Filled.Check, Icons.Outlined.Check),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name, icon, color)
                },
                enabled = name.isNotBlank() && !busy
            ) { Text(if (busy) "保存中…" else "保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") }
        }
    )
}
