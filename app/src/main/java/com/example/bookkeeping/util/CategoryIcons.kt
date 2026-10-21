package com.example.bookkeeping.util

import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** 分类图标表：key 持久化到数据库，这里映射到 Material 图标。 */
object CategoryIcons {

    data class IconOption(val key: String, val label: String, val icon: ImageVector)

    val ALL: List<IconOption> = listOf(
        IconOption("restaurant", "餐饮", Icons.Filled.Restaurant),
        IconOption("local_cafe", "咖啡", Icons.Filled.LocalCafe),
        IconOption("fastfood", "快餐", Icons.Filled.Fastfood),
        IconOption("local_grocery_store", "超市", Icons.Filled.LocalGroceryStore),
        IconOption("local_bar", "酒吧", Icons.Filled.LocalBar),
        IconOption("directions_bus", "公交", Icons.Filled.DirectionsBus),
        IconOption("directions_car", "打车", Icons.Filled.DirectionsCar),
        IconOption("local_taxi", "出租", Icons.Filled.LocalTaxi),
        IconOption("train", "火车", Icons.Filled.Train),
        IconOption("flight", "机票", Icons.Filled.Flight),
        IconOption("luggage", "行李", Icons.Filled.Luggage),
        IconOption("hotel", "住宿", Icons.Filled.Hotel),
        IconOption("beach_access", "度假", Icons.Filled.BeachAccess),
        IconOption("local_gas_station", "加油", Icons.Filled.LocalGasStation),
        IconOption("shopping_bag", "购物", Icons.Filled.ShoppingBag),
        IconOption("checkroom", "服装", Icons.Filled.Checkroom),
        IconOption("sports_esports", "娱乐", Icons.Filled.SportsEsports),
        IconOption("movie", "电影", Icons.Filled.Movie),
        IconOption("music_note", "音乐", Icons.Filled.MusicNote),
        IconOption("sports_soccer", "运动", Icons.Filled.SportsSoccer),
        IconOption("pets", "宠物", Icons.Filled.Pets),
        IconOption("toys", "玩具", Icons.Filled.Toys),
        IconOption("local_hospital", "医疗", Icons.Filled.LocalHospital),
        IconOption("medication", "药品", Icons.Filled.Medication),
        IconOption("school", "教育", Icons.Filled.School),
        IconOption("menu_book", "图书", Icons.Filled.MenuBook),
        IconOption("phone_iphone", "通讯", Icons.Filled.PhoneIphone),
        IconOption("wifi", "网络", Icons.Filled.Wifi),
        IconOption("electric_bolt", "水电", Icons.Filled.ElectricBolt),
        IconOption("home", "居家", Icons.Filled.Home),
        IconOption("kitchen", "厨房", Icons.Filled.Kitchen),
        IconOption("build", "维修", Icons.Filled.Build),
        IconOption("card_giftcard", "人情", Icons.Filled.CardGiftcard),
        IconOption("emoji_events", "奖杯", Icons.Filled.EmojiEvents),
        IconOption("savings", "存钱", Icons.Filled.Savings),
        IconOption("credit_card", "卡", Icons.Filled.CreditCard),
        IconOption("receipt_long", "收据", Icons.Filled.ReceiptLong),
        IconOption("work", "工作", Icons.Filled.Work),
        IconOption("currency_exchange", "汇兑", Icons.Filled.CurrencyExchange),
        IconOption("more_horiz", "其他", Icons.Filled.MoreHoriz)
    )

    private val INDEX: Map<String, ImageVector> =
        ALL.associate { it.key to it.icon }

    private val OUTLINE_INDEX = mapOf(
        "restaurant" to Icons.Outlined.Restaurant,
        "local_cafe" to Icons.Outlined.LocalCafe,
        "fastfood" to Icons.Outlined.Fastfood,
        "local_grocery_store" to Icons.Outlined.LocalGroceryStore,
        "local_bar" to Icons.Outlined.LocalBar,
        "directions_bus" to Icons.Outlined.DirectionsBus,
        "directions_car" to Icons.Outlined.DirectionsCar,
        "local_taxi" to Icons.Outlined.LocalTaxi,
        "train" to Icons.Outlined.Train,
        "flight" to Icons.Outlined.Flight,
        "luggage" to Icons.Outlined.Luggage,
        "hotel" to Icons.Outlined.Hotel,
        "beach_access" to Icons.Outlined.BeachAccess,
        "local_gas_station" to Icons.Outlined.LocalGasStation,
        "shopping_bag" to Icons.Outlined.ShoppingBag,
        "checkroom" to Icons.Outlined.Checkroom,
        "sports_esports" to Icons.Outlined.SportsEsports,
        "movie" to Icons.Outlined.Movie,
        "music_note" to Icons.Outlined.MusicNote,
        "sports_soccer" to Icons.Outlined.SportsSoccer,
        "pets" to Icons.Outlined.Pets,
        "toys" to Icons.Outlined.Toys,
        "local_hospital" to Icons.Outlined.LocalHospital,
        "medication" to Icons.Outlined.Medication,
        "school" to Icons.Outlined.School,
        "menu_book" to Icons.Outlined.MenuBook,
        "phone_iphone" to Icons.Outlined.PhoneIphone,
        "wifi" to Icons.Outlined.Wifi,
        "electric_bolt" to Icons.Outlined.ElectricBolt,
        "home" to Icons.Outlined.Home,
        "kitchen" to Icons.Outlined.Kitchen,
        "build" to Icons.Outlined.Build,
        "card_giftcard" to Icons.Outlined.CardGiftcard,
        "emoji_events" to Icons.Outlined.EmojiEvents,
        "savings" to Icons.Outlined.Savings,
        "credit_card" to Icons.Outlined.CreditCard,
        "receipt_long" to Icons.Outlined.ReceiptLong,
        "work" to Icons.Outlined.Work,
        "currency_exchange" to Icons.Outlined.CurrencyExchange,
        "more_horiz" to Icons.Outlined.MoreHoriz
    )

    fun byKey(key: String, outlined: Boolean = false): ImageVector =
        if (outlined) OUTLINE_INDEX[key] ?: Icons.Outlined.MoreHoriz else INDEX[key] ?: Icons.Filled.MoreHoriz

    fun labelOf(key: String): String =
        ALL.firstOrNull { it.key == key }?.label ?: key
}

/** 分类可选颜色（12 色预设调色板）。 */
val CATEGORY_COLORS: List<String> = listOf(
    "#FF7043", "#42A5F5", "#AB47BC", "#EC407A", "#FFA726", "#26A69A",
    "#5C6BC0", "#8D6E63", "#EF5350", "#66BB6A", "#29B6F6", "#90A4AE"
)