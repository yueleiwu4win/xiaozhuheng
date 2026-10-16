package com.example.bookkeeping.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.ui.common.AppBackground
import com.example.bookkeeping.ui.home.FittedLedgerAmount
import com.example.bookkeeping.ui.theme.*
import kotlin.math.roundToInt

private val ReadingSaver = listSaver<ReadingOptions, Any>(
    save = { listOf(it.size.value, it.weight.value, it.font.value, it.backgroundStrength, it.enhanced) },
    restore = { ReadingOptions(ReadingSize.from(it[0] as String), ReadingWeight.from(it[1] as String),
        ReadingFont.from(it[2] as String), it[3] as Int, it[4] as Boolean).normalized() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingSettingsSheet(settings: AppSettings, onApply: (ReadingOptions) -> Unit, onDismiss: () -> Unit) {
    var draft by rememberSaveable(settings.reading, stateSaver = ReadingSaver) { mutableStateOf(settings.reading) }
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        ReadingSettingsContent(settings, draft, { draft = it }, { onApply(draft.normalized()) })
    }
}

@Composable
internal fun ReadingSettingsContent(settings: AppSettings, draft: ReadingOptions, onChange: (ReadingOptions) -> Unit, onApply: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f)) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    Text("文字与阅读", style = MaterialTheme.typography.titleLarge)
                    Text("调整后先看预览，点应用后对所有主题生效。关闭不保存。", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item { ReadingPreview(settings, draft) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ onChange(ReadingOptions.Clear) }, Modifier.weight(1f)) { Text("一键清晰") }
                        TextButton({ onChange(ReadingOptions()) }, Modifier.weight(1f)) { Text("恢复默认") }
                    }
                }
                item {
                    ReadingChoices("字体大小", ReadingSize.entries, draft.size, { it.label }) { onChange(draft.copy(size = it)) }
                    Text("在系统字号基础上放大；金额会根据可用空间适配。", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item { ReadingChoices("字体粗细", ReadingWeight.entries, draft.weight, { it.label }) { onChange(draft.copy(weight = it)) } }
                item { ReadingChoices("字体样式", ReadingFont.entries, draft.font, { it.label }) { onChange(draft.copy(font = it)) } }
                item {
                    Text("背景强度 · ${draft.backgroundStrength}%", style = MaterialTheme.typography.titleSmall)
                    Slider(draft.backgroundStrength.toFloat(), { onChange(draft.copy(backgroundStrength = it.roundToInt())) },
                        Modifier.semantics { contentDescription = "背景强度" }, valueRange = 0f..100f, steps = 19)
                    Text("越低越淡，0% 显示主题底色。", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("阅读增强", style = MaterialTheme.typography.titleSmall)
                            Text("为账目和操作区域增加衬底，淡化装饰。", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(draft.enhanced, { onChange(draft.copy(enhanced = it)) }, Modifier.semantics { contentDescription = "阅读增强" })
                    }
                }
            }
            HorizontalDivider()
            Button({ onApply() }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).heightIn(min = 48.dp)) {
                Text("应用")
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ReadingChoices(title: String, choices: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { value ->
                FilterChip(value == selected, { onSelect(value) }, label = { Text(label(value)) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}

/** 复用真实主题、背景、金额测量与文字设置，预览不会叠乘当前已应用字号。 */
@Composable
internal fun ReadingPreview(settings: AppSettings, options: ReadingOptions) {
    BookkeepingTheme(settings.themeMode, settings.themeStyle, options) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                AppBackground(Modifier.matchParentSize())
                Column(Modifier.fillMaxWidth().readingSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("阅读效果预览", style = MaterialTheme.typography.titleMedium)
                    Text("本月支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FittedLedgerAmount("¥2,346.80", 28.sp, Modifier.fillMaxWidth())
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Restaurant, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("餐饮", style = MaterialTheme.typography.bodyLarge)
                            Text("午餐 · 备注示例", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FittedLedgerAmount("−¥28.00", 18.sp, Modifier.weight(1f), androidx.compose.ui.text.style.TextAlign.End)
                    }
                    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.small) {
                        Text("记一笔", Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
