package com.example.bookkeeping.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.bookkeeping.data.prefs.ThemeMode
import com.example.bookkeeping.data.prefs.ThemeStyle
import com.example.bookkeeping.ui.theme.BookkeepingTheme
import com.example.bookkeeping.ui.theme.LocalAppDark

/** 用实际主题渲染可选择的样片，避免只凭文字猜配色。 */
@Composable
fun ThemeStyleChoice(style: ThemeStyle, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val previewMode = style.recommendedMode ?: if (LocalAppDark.current) ThemeMode.DARK else ThemeMode.LIGHT
    val selectionColor = MaterialTheme.colorScheme.primary
    BookkeepingTheme(previewMode, style) {
        Surface(
            selected = selected, onClick = onClick, enabled = enabled,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(if (selected) 2.dp else 1.dp,
                if (selected) selectionColor else MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(style.label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.SansSerif)
                    if (selected) Icon(Icons.Outlined.CheckCircle, "已选择", tint = MaterialTheme.colorScheme.primary)
                }
                Text(style.description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onSurface).forEach { color ->
                        Box(Modifier.size(16.dp).background(color, CircleShape))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(when (style.recommendedMode) {
                        ThemeMode.LIGHT -> "推荐浅色"
                        ThemeMode.DARK -> "推荐深色"
                        else -> "明暗随当前设置"
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
