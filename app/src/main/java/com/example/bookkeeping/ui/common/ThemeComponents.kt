package com.example.bookkeeping.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.bookkeeping.ui.theme.LocalThemeTokens
import com.example.bookkeeping.util.CategoryIcons

@Composable
fun AppButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val tokens = LocalThemeTokens.current
    Button(onClick, if (tokens.ledgerRows) modifier.heightIn(min = 48.dp) else modifier, enabled, shape = RoundedCornerShape(tokens.actionCorner), content = content)
}

@Composable
fun AppOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val tokens = LocalThemeTokens.current
    OutlinedButton(onClick, if (tokens.ledgerRows) modifier.heightIn(min = 48.dp) else modifier, enabled, shape = RoundedCornerShape(tokens.actionCorner), content = content)
}

@Composable
fun AppFilterChip(
    selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, leadingIcon: (@Composable () -> Unit)? = null
) {
    val tokens = LocalThemeTokens.current
    FilterChip(
        selected, onClick, label, if (tokens.ledgerRows) modifier.heightIn(min = 48.dp) else modifier, enabled,
        leadingIcon = leadingIcon, shape = RoundedCornerShape(tokens.chipCorner),
        colors = if (tokens.ledgerRows) FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent, selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) else FilterChipDefaults.filterChipColors(),
        border = if (tokens.ledgerRows) BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            else FilterChipDefaults.filterChipBorder(enabled, selected)
    )
}

@Composable
fun categoryIcon(key: String): ImageVector = CategoryIcons.byKey(key, LocalThemeTokens.current.outlinedIcons)

@Composable
fun themedIcon(classic: ImageVector, outlined: ImageVector): ImageVector =
    if (LocalThemeTokens.current.outlinedIcons) outlined else classic

@Composable
fun themeCategoryColor(color: Color): Color = if (LocalThemeTokens.current.mutedCategoryIcons)
    lerp(color, MaterialTheme.colorScheme.onSurfaceVariant, 0.3f) else color

@Composable
fun Modifier.ledgerDivider(): Modifier {
    if (!LocalThemeTokens.current.ledgerRows) return this
    val color = MaterialTheme.colorScheme.outlineVariant
    return drawWithContent {
        drawContent()
        drawLine(color, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1.dp.toPx())
    }
}
