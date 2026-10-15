package com.example.bookkeeping.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookkeeping.AppViewModelProvider
import com.example.bookkeeping.data.export.ExportManager
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.ui.common.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CURRENCIES = linkedMapOf("CNY" to "人民币 ¥", "USD" to "美元 $", "JPY" to "日元 JP¥", "EUR" to "欧元 €", "GBP" to "英镑 £")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenCategories: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val billCount by viewModel.billCount.collectAsStateWithLifecycle()
    val cacheSize by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val preview by viewModel.backupPreview.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var clearVisible by rememberSaveable { mutableStateOf(false) }
    var cleanVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refreshCacheSize() }
    LaunchedEffect(result, preview) {
        if (preview == null) result?.let { snackbar.showSnackbar(it.message); viewModel.consumeResult() }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        uri -> uri?.let { viewModel.exportToUri(ExportManager.Kind.CSV, it) }
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        uri -> uri?.let { viewModel.exportToUri(ExportManager.Kind.JSON, it) }
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        uri -> uri?.let { viewModel.setBackgroundImage(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri -> uri?.let { viewModel.previewBackup(it) }
    }
    fun share(kind: ExportManager.Kind) {
        viewModel.prepareShareFile(kind) { file ->
            try {
                val uri = viewModel.exportManager.shareUriFor(file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = viewModel.exportManager.mimeTypeOf(kind)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, viewModel.exportManager.shareTitleOf(kind, billCount.toInt()))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "选择分享方式").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            } catch (e: Exception) { viewModel.reportShareFailure(e) }
        }
    }
    val themeLabel = when (settings.themeMode) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "浅色"; ThemeMode.DARK -> "深色" }
    val backgroundLabel = when (settings.background) {
        BACKGROUND_SPEC_DEFAULT -> "主题默认"
        BACKGROUND_SPEC_CUSTOM_IMAGE -> "相册图片"
        else -> BackgroundPresets.firstOrNull { it.spec() == settings.background }?.label ?: "自定义"
    }
    Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbar) }, topBar = { TopAppBar(title = { Text("设置") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SectionCard("外观") {
                        SettingRow("界面风格", themedIcon(Icons.Filled.Palette, Icons.Outlined.Palette), value = settings.themeStyle.label, enabled = !busy) { panel = "style" }
                        SettingRow("明暗模式", themedIcon(Icons.Filled.Palette, Icons.Outlined.Palette), value = themeLabel, enabled = !busy) { panel = "theme" }
                        SettingRow("背景", themedIcon(Icons.Filled.Image, Icons.Outlined.Image), value = backgroundLabel, enabled = !busy) { panel = "background" }
                        SettingRow("显示币种", themedIcon(Icons.Filled.Payments, Icons.Outlined.Payments), value = CURRENCIES[settings.currency] ?: settings.currency, enabled = !busy) { panel = "currency" }
                    }
                }
                item {
                    SectionCard("分类") {
                        SettingRow("分类管理", themedIcon(Icons.Filled.Category, Icons.Outlined.Category), subtitle = "收入与支出分类、排序、归档", enabled = !busy, onClick = onOpenCategories)
                    }
                }
                item {
                    SectionCard("数据 · $billCount 笔账单") {
                        SettingRow("备份数据", themedIcon(Icons.Filled.SaveAlt, Icons.Outlined.SaveAlt), subtitle = "保存账单、分类和设置，便于换机恢复", enabled = !busy) { panel = "backup" }
                        SettingRow("恢复备份", themedIcon(Icons.Filled.Restore, Icons.Outlined.Restore), subtitle = "先查看备份摘要，再确认恢复", enabled = !busy) {
                            importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*", "*/*"))
                        }
                        SettingRow("导出表格", themedIcon(Icons.Filled.TableChart, Icons.Outlined.TableChart), subtitle = "用于 Excel 查看与分析", enabled = !busy) { panel = "csv" }
                    }
                }
                item {
                    SectionCard("存储") {
                        SettingRow("清理缓存", themedIcon(Icons.Filled.CleaningServices, Icons.Outlined.CleaningServices), subtitle = "账单与分类保留", value = formatSize(cacheSize), enabled = !busy) { cleanVisible = true }
                    }
                }
                item {
                    SectionCard("高级") {
                        SettingRow("清空所有数据", themedIcon(Icons.Filled.DeleteOutline, Icons.Outlined.DeleteOutline), subtitle = "删除全部账单，恢复内置分类",
                            enabled = !busy, danger = true) { clearVisible = true }
                    }
                }
                item {
                    Text("账单保存在本机。卸载或换机前，请先备份数据。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    panel?.let { current ->
        ModalBottomSheet(onDismissRequest = { panel = null }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(when (current) { "style" -> "界面风格"; "theme" -> "明暗模式"; "background" -> "背景"; "currency" -> "显示币种"; "backup" -> "备份数据"; else -> "导出表格" },
                    style = MaterialTheme.typography.titleLarge)
                when (current) {
                    "style" -> {
                        Text("新主题会采用推荐的明暗模式。你也可以在「明暗模式」中自行调整或跟随系统。", style = MaterialTheme.typography.bodyMedium)
                        ThemeStyle.entries.forEach { style ->
                            ThemeStyleChoice(style, style == settings.themeStyle, enabled = !busy) { viewModel.setThemeStyle(style); panel = null }
                        }
                    }
                    "theme" -> ThemeMode.entries.forEach { mode ->
                        val label = when (mode) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "浅色"; ThemeMode.DARK -> "深色" }
                        ChoiceRow(label, mode == settings.themeMode, enabled = !busy) { viewModel.setThemeMode(mode); panel = null }
                    }
                    "background" -> {
                        ChoiceRow("主题默认", settings.background == BACKGROUND_SPEC_DEFAULT, !busy) { viewModel.setBackgroundPreset("default"); panel = null }
                        BackgroundPresets.forEach { preset ->
                            ChoiceRow(preset.label, settings.background == preset.spec(), !busy) { viewModel.setBackgroundPreset(preset.key); panel = null }
                        }
                        ChoiceRow("从相册选择", settings.background == BACKGROUND_SPEC_CUSTOM_IMAGE, !busy) { panel = null; photoLauncher.launch("image/*") }
                    }
                    "currency" -> {
                        Text("仅改变显示符号，不进行汇率换算。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CURRENCIES.forEach { (code, label) ->
                            ChoiceRow(label, settings.currency == code, !busy) { viewModel.setCurrency(code); panel = null }
                        }
                    }
                    else -> {
                        val kind = if (current == "backup") ExportManager.Kind.JSON else ExportManager.Kind.CSV
                        Text(if (kind == ExportManager.Kind.JSON) "备份包含账单、分类和设置；相册背景图片不包含在文件中。" else "表格文件可用 Excel 打开，用于查看和分析。",
                            style = MaterialTheme.typography.bodyMedium)
                        AppButton(onClick = {
                            panel = null
                            if (kind == ExportManager.Kind.JSON) jsonLauncher.launch(viewModel.defaultName(kind))
                            else csvLauncher.launch(viewModel.defaultName(kind))
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("保存到文件") }
                        AppOutlinedButton(onClick = { panel = null; share(kind) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("通过系统分享") }
                    }
                }
            }
        }
    }
    preview?.let { backup ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelBackupPreview() }, title = { Text("确认恢复备份") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(backup.fileName, style = MaterialTheme.typography.titleSmall)
                    backup.exportedAt?.let { Text("备份时间：${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA).format(Date(it))}") }
                    Text("可恢复：${backup.bills.size} 笔账单、${backup.categories.size} 个分类")
                    Text("日期范围：${backup.firstDate ?: "无账单"}${backup.lastDate?.let { " 至 $it" } ?: ""}")
                    Text(if (backup.settings == null) "此备份不含设置，将保留当前设置。" else "将恢复备份中的设置；缺失的相册背景图使用默认背景。")
                    if (backup.skippedBills + backup.skippedCategories > 0) Text(
                        "校验发现 ${backup.skippedBills} 笔无效账单、${backup.skippedCategories} 个无效分类，恢复时会跳过。",
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        if (backup.bills.isEmpty()) "备份中没有账单，恢复会清空当前 $billCount 笔账单。"
                        else "恢复会替换当前全部 $billCount 笔账单和分类。建议先备份当前数据。",
                        color = MaterialTheme.colorScheme.error
                    )
                    AppOutlinedButton(onClick = { jsonLauncher.launch(viewModel.defaultName(ExportManager.Kind.JSON)) }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth()) { Text("先备份当前数据") }
                    result?.let { Text(it.message, color = if (it.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::restoreBackup, enabled = !busy) { Text(if (busy) "处理中…" else "确认替换并恢复", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelBackupPreview, enabled = !busy) { Text("取消") } }
        )
    }
    if (cleanVisible) ConfirmDialog(
        title = "清理缓存？", text = "清理约 ${formatSize(cacheSize)} 缓存和未使用背景图，账单与分类保留。",
        confirmLabel = "清理", destructive = false, onConfirm = { cleanVisible = false; viewModel.clearCache() }, onDismiss = { cleanVisible = false }
    )
    if (clearVisible) ConfirmDialog(
        title = "清空所有数据？", text = "将删除全部 $billCount 笔账单，并恢复内置分类。此操作无法撤销，请先备份数据。",
        confirmLabel = "清空", onConfirm = { clearVisible = false; viewModel.clearAll() }, onDismiss = { clearVisible = false }
    )
}

@Composable
private fun SettingRow(
    title: String, icon: ImageVector, subtitle: String? = null, value: String? = null,
    enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit
) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (danger) color else MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        value?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 110.dp), maxLines = 2, overflow = TextOverflow.Ellipsis) }
        Icon(themedIcon(Icons.Filled.ChevronRight, Icons.Outlined.ChevronRight), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChoiceRow(title: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick).padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Icon(themedIcon(Icons.Filled.Check, Icons.Outlined.Check), null, tint = MaterialTheme.colorScheme.primary)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1048576 -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
    bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
