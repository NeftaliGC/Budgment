package com.nintech.budgment.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

enum class CategoryIcon(val key: String) {
    RESTAURANT("restaurant"),
    FASTFOOD("fastfood"),
    GROCERY("grocery"),
    CART("cart"),
    TRANSPORT("transport"),
    GAS("gas"),
    HOME("home"),
    BED("bed"),
    HEALTH("health"),
    ENTERTAINMENT("entertainment"),
    GAMES("games"),
    TRAVEL("travel"),
    SCHOOL("school"),
    PETS("pets"),
    PHONE("phone"),
    USES("uses"),
    SALARY("salary"),
    INCOME("income"),
    BANK("bank"),
    SAVINGS("savings"),
    INVESTMENT("investment");

    val icon: ImageVector
        get() = try {
            when (this) {
                RESTAURANT -> Icons.Filled.Restaurant
                FASTFOOD -> Icons.Filled.Fastfood
                GROCERY -> Icons.Filled.LocalGroceryStore
                CART -> Icons.Filled.ShoppingBag
                TRANSPORT -> Icons.Filled.DirectionsCar
                GAS -> Icons.Filled.LocalGasStation
                HOME -> Icons.Filled.Home
                BED -> Icons.Filled.Bed
                HEALTH -> Icons.Filled.FitnessCenter
                ENTERTAINMENT -> Icons.Filled.Movie
                GAMES -> Icons.Filled.SportsEsports
                TRAVEL -> Icons.Filled.Flight
                SCHOOL -> Icons.Filled.School
                PETS -> Icons.Filled.Pets
                PHONE -> Icons.Filled.PhoneIphone
                USES -> Icons.Filled.WaterDrop
                SALARY -> Icons.Filled.Work
                INCOME -> Icons.Filled.Payments
                BANK -> Icons.Filled.AccountBalance
                SAVINGS -> Icons.Filled.Savings
                INVESTMENT -> Icons.AutoMirrored.Filled.TrendingUp
            }
        } catch (_: Throwable) {
            fallbackIcon
        }

    private val fallbackIcon: ImageVector
        get() = when (this) {
            RESTAURANT, FASTFOOD, GROCERY, CART, INCOME -> Icons.Filled.ShoppingCart
            TRANSPORT, GAS, TRAVEL -> Icons.Filled.Place
            HOME, BED, BANK -> Icons.Filled.Home
            HEALTH, PETS -> Icons.Filled.Favorite
            ENTERTAINMENT, GAMES, SAVINGS, INVESTMENT, SALARY -> Icons.Filled.Star
            SCHOOL, USES -> Icons.Filled.Info
            PHONE -> Icons.Filled.Phone
        }

    companion object {
        fun resolveIcon(key: String?): ImageVector? =
            entries.firstOrNull { it.key == key }?.icon
    }
}