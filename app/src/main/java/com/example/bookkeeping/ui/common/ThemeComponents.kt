package com.example.bookkeeping.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.example.bookkeeping.R
import com.example.bookkeeping.ui.theme.OverviewStyle
import com.example.bookkeeping.ui.theme.SelectionStyle
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
import com.example.bookkeeping.ui.theme.HomeLayout
import com.example.bookkeeping.ui.theme.LedgerTypeface
import com.example.bookkeeping.ui.theme.readingTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
    if (tokens.filterStyle == SelectionStyle.BOOKMARK) {
        Surface(
            selected = selected, onClick = onClick, enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp), color = Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(tokens.chipCorner)
        ) {
            Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    leadingIcon?.invoke()
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) { label() }
                }
                HorizontalDivider(Modifier.width(28.dp), thickness = 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            }
        }
        return
    }
    FilterChip(
        selected, onClick, label, if (tokens.ledgerRows) modifier.heightIn(min = 48.dp) else modifier, enabled,
        leadingIcon = leadingIcon, shape = RoundedCornerShape(tokens.chipCorner),
        colors = if (tokens.filterStyle == SelectionStyle.PILL) FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ) else if (tokens.ledgerRows) FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent, selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) else FilterChipDefaults.filterChipColors(),
        border = if (tokens.filterStyle == SelectionStyle.PILL) BorderStroke(0.dp, Color.Transparent)
            else if (tokens.ledgerRows) BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            else FilterChipDefaults.filterChipBorder(enabled, selected)
    )
}

@Composable
fun categoryIcon(key: String): ImageVector = CategoryIcons.byKey(key, LocalThemeTokens.current.outlinedIcons)

@Composable
fun themedIcon(classic: ImageVector, outlined: ImageVector): ImageVector =
    if (LocalThemeTokens.current.outlinedIcons) outlined else classic

@Composable
fun themeAmountStyle(base: TextStyle): TextStyle = readingTextStyle(base.copy(
    fontFamily = if (LocalThemeTokens.current.homeLayout == HomeLayout.STANDARD) FontFamily.SansSerif else LedgerTypeface,
    fontFeatureSettings = "tnum"
))

/** 同一纸纤维素材由不同主题以低透明度使用，不将整页参考图当背景。 */
@Composable
internal fun LedgerPaper(modifier: Modifier = Modifier, alpha: Float = 0.1f) {
    Image(painterResource(R.drawable.ledger_paper_texture), null, modifier, contentScale = ContentScale.Crop, alpha = alpha)
}

@Composable
fun themeCategoryColor(color: Color): Color = when {
    LocalThemeTokens.current.monochromeCategoryIcons -> MaterialTheme.colorScheme.primary
    LocalThemeTokens.current.mutedCategoryIcons -> lerp(color, MaterialTheme.colorScheme.onSurfaceVariant, 0.3f)
    else -> color
}

@Composable
fun ThemeBrandTitle(modifier: Modifier = Modifier) {
    val tokens = LocalThemeTokens.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (tokens.portraitBrand) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
                Image(painterResource(R.drawable.brand_icon_foreground), null,
                    Modifier.fillMaxSize().graphicsLayer(scaleX = 2f, scaleY = 2f), contentScale = ContentScale.Crop)
            }
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        if (tokens.abacusBrand) {
            Image(painterResource(R.drawable.theme_abacus), null,
                Modifier.width(48.dp).height(24.dp), contentScale = ContentScale.Fit,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
fun ThemeOverviewPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val tokens = LocalThemeTokens.current
    val background = when (tokens.overviewStyle) {
        OverviewStyle.JADE -> MaterialTheme.colorScheme.primaryContainer
        OverviewStyle.LEDGER -> Color.Transparent
        OverviewStyle.CARD -> MaterialTheme.colorScheme.surface
    }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.overviewCorner), color = background) {
        Box {
            if (tokens.overviewStyle == OverviewStyle.JADE) tokens.ornament?.let { resource ->
                Image(painterResource(resource), null, Modifier.matchParentSize(),
                    contentScale = ContentScale.FillWidth, alignment = Alignment.TopEnd, alpha = 0.18f)
            }
            content()
        }
    }
}

@Composable
fun Modifier.ledgerDivider(): Modifier {
    if (!LocalThemeTokens.current.ledgerRows) return this
    val color = MaterialTheme.colorScheme.outlineVariant
    return drawWithContent {
        drawContent()
        drawLine(color, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1.dp.toPx())
    }
}
