package com.example.bookkeeping.ui.common

import com.example.bookkeeping.ui.theme.LocalThemeTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.YearMonth

/** 十六进制颜色字符串（如 "#FF7043"）→ Compose Color；非法值回退灰色，避免组合期崩溃。 */
fun String.hexToColor(fallback: Color = Color(0xFF90A4AE)): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)

/** 白色卡片区块：标题 + 内容。 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalThemeTokens.current.sectionCorner),
        colors = CardDefaults.cardColors(containerColor = if (LocalThemeTokens.current.groupedSections) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = if (LocalThemeTokens.current.groupedSections) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

/** 空状态占位。 */
@Composable
fun EmptyState(
    text: String,
    icon: ImageVector = Icons.Outlined.Inbox,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** 统计指标小卡。 */
@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(LocalThemeTokens.current.metricCorner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = "tnum")
            )
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 分类图标圆角方块。 */
@Composable
fun CategoryIconBox(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    corner: Dp = 12.dp,
    iconPadding: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(if (LocalThemeTokens.current.mutedCategoryIcons) MaterialTheme.colorScheme.primaryContainer else color.copy(alpha = 0.14f), RoundedCornerShape(if (LocalThemeTokens.current.mutedCategoryIcons) size / 2 else corner)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(iconPadding),
            tint = themeCategoryColor(color)
        )
    }
}

/** 列表分组头。 */
@Composable
fun GroupHeader(
    left: String,
    right: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = right,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 通用确认对话框。 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "确定",
    cancelLabel: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        }
    )
}

/** 月份切换器：‹  2025年6月  ›（传入 onLabelClick 时点击文字可直接选择月份） */
@Composable
fun MonthSwitcher(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    prevEnabled: Boolean = true,
    nextEnabled: Boolean = true,
    contentDescription: String = label,
    onLabelClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, enabled = prevEnabled) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "上一个"
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .then(if (onLabelClick != null) Modifier.clickable(onClick = onLabelClick) else Modifier)
                .padding(horizontal = 4.dp)
        )
        IconButton(onClick = onNext, enabled = nextEnabled) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "下一个"
            )
        }
    }
}

/**
 * 月份选择对话框：年份左右切换 + 12 个月宫格，点选某月立即确认。
 */
@Composable
fun MonthPickerDialog(
    initial: YearMonth,
    onSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    var shown by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择月份") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { shown = shown.minusMonths(12) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "上一年")
                    }
                    Text(
                        text = "${shown.year}年",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { shown = shown.plusMonths(12) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "下一年")
                    }
                }
                Spacer(Modifier.height(8.dp))
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0..3) {
                            val month = row * 4 + col + 1
                            val selected = shown == YearMonth.of(shown.year, month)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(LocalThemeTokens.current.pickerCorner))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        onSelected(YearMonth.of(shown.year, month))
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${month}月",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

/** 行内占比条的容器行（标签 + 条 + 数值）。 */
@Composable
fun RankRow(
    label: String,
    fraction: Float,
    color: Color,
    valueText: String,
    subText: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(64.dp),
            maxLines = 1
        )
        com.example.bookkeeping.ui.chart.RankBar(
            fraction = fraction,
            color = color,
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(92.dp),
            textAlign = TextAlign.End
        )
        if (subText != null) {
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.End
            )
        }
    }
}
