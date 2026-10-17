package com.example.bookkeeping.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookkeeping.AppViewModelProvider
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.ui.common.AppBackground
import com.example.bookkeeping.ui.common.themeAmountStyle
import com.example.bookkeeping.ui.home.FittedLedgerAmount
import com.example.bookkeeping.ui.theme.*
import com.example.bookkeeping.util.AmountFormatter
import kotlin.math.roundToInt

private val ReadingSaver = listSaver<ReadingOptions, Any>(
    save = { listOf(it.size.value, it.weight.value, it.font.value, it.backgroundStrength, it.enhanced) },
    restore = { ReadingOptions(ReadingSize.from(it[0] as String), ReadingWeight.from(it[1] as String),
        ReadingFont.from(it[2] as String), it[3] as Int, it[4] as Boolean).normalized() }
)

@Composable
fun ReadingSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    var draft by rememberSaveable(stateSaver = ReadingSaver) { mutableStateOf(settings.reading) }
    val snackbar = remember { SnackbarHostState() }
    BackHandler(enabled = busy) { }
    LaunchedEffect(result) {
        result?.let { snackbar.showSnackbar(it.message); viewModel.consumeResult() }
    }
    ReadingSettingsContent(settings, draft, { draft = it },
        onApply = { viewModel.setReading(draft.normalized(), onBack) }, onBack = onBack, busy = busy, snackbar = snackbar)
}

/** 控件采用清晰字体和已应用字号；草稿只改变预览，选项不会边点边跳动。 */
@Composable
internal fun ReadingSettingsContent(settings: AppSettings, draft: ReadingOptions, onChange: (ReadingOptions) -> Unit, onApply: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onBack: () -> Unit = {}, busy: Boolean = false, snackbar: SnackbarHostState = remember { SnackbarHostState() }) {
    val controls = com.example.bookkeeping.ui.theme.Typography.withReading(settings.reading.copy(font = ReadingFont.SYSTEM))
    MaterialTheme(typography = controls) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.statusBarsPadding()) {
                        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp).heightIn(min = 56.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack, enabled = !busy) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回，不保存修改") }
                            Text("文字与阅读", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                            if (LocalDensity.current.fontScale > 1.5f) {
                                IconButton({ onChange(ReadingOptions()) }, enabled = !busy) { Icon(Icons.Outlined.Restore, "恢复默认") }
                            } else TextButton({ onChange(ReadingOptions()) }, enabled = !busy) { Text("恢复默认") }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    }
                }
            }, bottomBar = {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.navigationBarsPadding()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        Button(onApply, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 52.dp),
                            enabled = !busy, shape = RoundedCornerShape(12.dp)) { Text(if (busy) "正在应用…" else "应用设置") }
                    }
                }
            }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), state = listState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeading("效果预览", "仅供预览")
                        ReadingPreview(settings, draft)
                        Text("点应用后生效，返回不保存。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Surface(onClick = { onChange(ReadingOptions.Clear) }, enabled = !busy,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.AutoFixHigh, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("一键清晰", style = MaterialTheme.typography.labelLarge)
                            Text("推荐", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeading("字体大小")
                        ReadingChoices(ReadingSize.entries, draft.size, { if (it == ReadingSize.SYSTEM) "系统" else "${(it.scale * 100).roundToInt()}%" }, !busy) { onChange(draft.copy(size = it)) }
                        Text("在系统字号基础上调整", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeading("字体粗细")
                        ReadingChoices(ReadingWeight.entries, draft.weight, { if (it == ReadingWeight.THEME) "主题" else it.label }, !busy) { onChange(draft.copy(weight = it)) }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeading("字体样式")
                        ReadingChoices(ReadingFont.entries, draft.font, { when(it) { ReadingFont.THEME -> "主题字体"; ReadingFont.SYSTEM -> "系统字体"; ReadingFont.SERIF -> "宋体" } }, !busy,
                            sampleStyle = { font -> applyReadingStyle(TextStyle(fontSize = 22.sp,
                                fontFamily = ThemeRegistry.resolve(settings.themeStyle).typography.bodyLarge.fontFamily ?: FontFamily.Default), ReadingOptions(font = font)) }
                        ) { onChange(draft.copy(font = it)) }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("背景强度", "${draft.backgroundStrength}%")
                        ReadingStrengthSlider(draft.backgroundStrength, !busy) { onChange(draft.copy(backgroundStrength = it)) }
                        Text("越低越淡，0% 仅显示主题底色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionHeading("阅读增强")
                            Text("增加文字衬底，淡化背景干扰", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(draft.enhanced, { onChange(draft.copy(enhanced = it)) }, enabled = !busy,
                            modifier = Modifier.semantics { contentDescription = "阅读增强" })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, value: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        if (value != null) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun readingChoiceColumns(count: Int, width: Float, fontScale: Float): Int {
    val effectiveWidth = width / fontScale.coerceAtLeast(1f)
    return when { effectiveWidth >= 300f -> count; count == 4 && effectiveWidth >= 180f -> 2; else -> 1 }
}

@Composable
private fun <T> ReadingChoices(choices: List<T>, selected: T, label: (T) -> String, enabled: Boolean,
    sampleStyle: ((T) -> TextStyle)? = null, onSelect: (T) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
        val columns = readingChoiceColumns(choices.size, maxWidth.value, fontScale)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { value ->
                        val chosen = value == selected
                        Surface(selected = chosen, onClick = { onSelect(value) }, enabled = enabled,
                            modifier = Modifier.weight(1f).fillMaxHeight().semantics { role = Role.RadioButton }, shape = RoundedCornerShape(8.dp),
                            color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            contentColor = if (chosen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) {
                            Column(Modifier.heightIn(min = 48.dp).padding(horizontal = 6.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                if (sampleStyle != null) Text("珠衡", style = sampleStyle(value), modifier = Modifier.padding(bottom = 6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (chosen) Icon(Icons.Outlined.Check, null, Modifier.size(12.dp))
                                    Text(label(value), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingStrengthSlider(value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outlineVariant
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    Slider(value.toFloat(), { onChange(it.roundToInt()) }, valueRange = 0f..100f, steps = 19, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "背景强度" },
        thumb = { Box(Modifier.size(20.dp).background(if (enabled) active else inactive, CircleShape)) },
        track = { state ->
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val start = if (rtl) size.width else 0f
                val end = if (rtl) 0f else size.width
                drawLine(inactive, Offset(start, center.y), Offset(end, center.y), size.height, StrokeCap.Round)
                drawLine(if (enabled) active else inactive, Offset(start, center.y), Offset(start + (end - start) * state.value / 100f, center.y), size.height, StrokeCap.Round)
            }
        })
}

@Composable
internal fun ReadingPreview(settings: AppSettings, options: ReadingOptions) {
    BookkeepingTheme(settings.themeMode, settings.themeStyle, options) {
        val currency = AmountFormatter.symbolOf(settings.currency)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Box(Modifier.fillMaxWidth()) {
                AppBackground(Modifier.matchParentSize())
                Column(Modifier.fillMaxWidth().readingSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("本月支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FittedLedgerAmount("${currency}2,346.80", 28.sp, Modifier.fillMaxWidth(), textStyle = themeAmountStyle(MaterialTheme.typography.headlineMedium))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Restaurant, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("餐饮", style = MaterialTheme.typography.bodyLarge)
                            Text("午餐 · 备注示例", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FittedLedgerAmount("−${currency}28.00", 18.sp, Modifier.weight(1f), TextAlign.End, textStyle = themeAmountStyle(MaterialTheme.typography.titleMedium))
                    }
                }
            }
        }
    }
}
