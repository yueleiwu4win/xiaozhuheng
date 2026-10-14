package com.example.bookkeeping.ui.theme

import androidx.compose.ui.graphics.Color

// 品牌主色：深青绿
val Primary = Color(0xFF214F43)
val PrimaryDark = Color(0xFF173D33)
val OnPrimary = Color(0xFFFFFFFF)

val SurfaceLight = Color(0xFFFFFDF9)
val SurfaceVariantLight = Color(0xFFF0EEE7)
val BackgroundLight = Color(0xFFF7F5EF)
val OnSurfaceLight = Color(0xFF242B27)
val OnSurfaceVariantLight = Color(0xFF646D65)
val OutlinedLight = Color(0xFFB9C1B8)

val SurfaceDark = Color(0xFF1C2521)
val SurfaceVariantDark = Color(0xFF2A3530)
val BackgroundDark = Color(0xFF131B17)
val OnSurfaceDark = Color(0xFFE7EDE7)
val OnSurfaceVariantDark = Color(0xFFB4C0B6)
val OutlinedDark = Color(0xFF78867D)

val ChartGrid = Color(0x1F2B2622)
val ChartText = Color(0xFF8A7F75)
val Accent = Color(0xFF4A90D9)
val Danger = Color(0xFFE5484D)

// ---------- 语义色（明暗各一套，统一在 AppColors 中取用） ----------

/** 收入色：浅色主题（对 #FFFBF8 约 5:1）。 */
val IncomeGreenLight = Color(0xFF2E7D32)

/** 收入色：深色主题（对 #14110F 约 9:1，避免原 #2E7D32 的 3.7:1 不可读）。 */
val IncomeGreenDark = Color(0xFF7CD48A)

/** 左滑删除区：橙色（白字可读）。 */
val SwipeDeleteOrange = Color(0xFFB43F3F)

/** 图表「其他」分类（环形图兜底色）。 */
val ChartOtherLight = Color(0xFF90A4AE)
val ChartOtherDark = Color(0xFF9EA7AE)
