package com.rodrip.precioslocales.comparador.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Stores : Screen("stores", "Locales", Icons.Rounded.Storefront)
    object Search : Screen("search", "Buscar", Icons.Rounded.Search)
    object Settings : Screen("settings", "Ajustes", Icons.Rounded.Settings)
    
    object AddEditStore : Screen("add_edit_store?storeId={storeId}") {
        fun createRoute(storeId: Long? = null) = if (storeId != null) "add_edit_store?storeId=$storeId" else "add_edit_store"
    }

    object ProductList : Screen("product_list/{storeId}") {
        fun createRoute(storeId: Long) = "product_list/$storeId"
    }

    object AddEditProduct : Screen("add_edit_product?storeId={storeId}&productId={productId}") {
        fun createRoute(storeId: Long? = null, productId: Long? = null): String {
            val builder = StringBuilder("add_edit_product")
            val params = mutableListOf<String>()
            if (storeId != null) params.add("storeId=$storeId")
            if (productId != null) params.add("productId=$productId")
            if (params.isNotEmpty()) builder.append("?").append(params.joinToString("&"))
            return builder.toString()
        }
    }

    object ProductComparison : Screen("product_comparison/{productId}") {
        fun createRoute(productId: Long) = "product_comparison/$productId"
    }

    object PriceHistory : Screen("price_history/{productId}/{storeId}") {
        fun createRoute(productId: Long, storeId: Long) = "price_history/$productId/$storeId"
    }
}

val bottomNavItems = listOf(
    Screen.Stores,
    Screen.Search,
    Screen.Settings
)
