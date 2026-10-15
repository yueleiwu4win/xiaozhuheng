package com.example.bookkeeping.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookkeeping.ui.theme.HomeLayout
import com.example.bookkeeping.ui.theme.LocalThemeTokens
import com.example.bookkeeping.ui.common.LedgerPaper

@Composable
internal fun LedgerCreateButton(onClick: () -> Unit, wide: Boolean, modifier: Modifier = Modifier) {
    Surface(onClick, modifier.heightIn(min = if (wide) 54.dp else 48.dp),
        shape = RoundedCornerShape(if (wide) 22.dp else 10.dp),
        color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        Box {
            LedgerPaper(Modifier.matchParentSize(), if (wide) 0.1f else 0.16f)
            Row(Modifier.padding(horizontal = if (wide) 20.dp else 16.dp, vertical = 8.dp).heightIn(min = 32.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(if (wide) 29.dp else 24.dp))
                Spacer(Modifier.width(12.dp))
                Text("记一笔", style = ledgerText(if (wide) 23.sp else 19.sp))
            }
        }
    }
}

@Composable
internal fun LedgerNavigationBar(selected: Int, onSelect: (Int) -> Unit) {
    val jade = LocalThemeTokens.current.homeLayout == HomeLayout.CELADON
    Surface(color = if (jade) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background) {
        Column(Modifier.navigationBarsPadding()) {
            if (!jade) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 12.dp, vertical = 6.dp)) {
                val labels = listOf("明细", "统计", "设置")
                val icons = listOf(Icons.Outlined.ReceiptLong, Icons.Outlined.PieChartOutline, Icons.Outlined.Settings)
                labels.forEachIndexed { index, label ->
                    val active = selected == index
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(Modifier.widthIn(min = 88.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (!jade && active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .selectable(active, role = Role.Tab, onClick = { onSelect(index) })
                            .padding(horizontal = 12.dp, vertical = 4.dp).heightIn(min = 52.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Box(Modifier.width(58.dp).height(28.dp).clip(RoundedCornerShape(20.dp))
                                .background(if (jade && active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                contentAlignment = Alignment.Center) {
                                Icon(icons[index], null, Modifier.size(25.dp), tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(label, style = ledgerText(12.sp), color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
