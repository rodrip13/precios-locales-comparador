package com.rodrip.precioslocales.comparador.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rodrip.precioslocales.comparador.MainApplication
import com.rodrip.precioslocales.comparador.ui.navigation.Screen
import com.rodrip.precioslocales.comparador.ui.navigation.bottomNavItems
import com.rodrip.precioslocales.comparador.ui.products.*
import com.rodrip.precioslocales.comparador.ui.search.SearchScreen
import com.rodrip.precioslocales.comparador.ui.stores.AddEditStoreScreen
import com.rodrip.precioslocales.comparador.ui.stores.StoreListScreen
import com.rodrip.precioslocales.comparador.ui.stores.StoreViewModel
import com.rodrip.precioslocales.comparador.ui.settings.SettingsScreen

@Composable
fun MainScreen(storeViewModel: StoreViewModel) {
    val context = LocalContext.current
    val productViewModel: ProductViewModel = viewModel(
        factory = ProductViewModelFactory((context.applicationContext as MainApplication).repository)
    )
    
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (bottomNavItems.any { it.route == currentDestination?.route }) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title!!) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Stores.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Stores.route) {
                StoreListScreen(
                    viewModel = storeViewModel,
                    onAddStore = { navController.navigate(Screen.AddEditStore.createRoute()) },
                    onAddProduct = { navController.navigate(Screen.AddEditProduct.createRoute()) },
                    onEditStore = { storeId -> navController.navigate(Screen.AddEditStore.createRoute(storeId)) },
                    onStoreClick = { storeId -> navController.navigate(Screen.ProductList.createRoute(storeId)) }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    productViewModel = productViewModel,
                    storeViewModel = storeViewModel,
                    onProductClick = { productId -> navController.navigate(Screen.ProductComparison.createRoute(productId)) },
                    onStoreClick = { storeId -> navController.navigate(Screen.ProductList.createRoute(storeId)) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.AddEditStore.route,
                arguments = listOf(navArgument("storeId") { 
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getLong("storeId")?.takeIf { it != -1L }
                AddEditStoreScreen(
                    viewModel = storeViewModel,
                    storeId = storeId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.ProductList.route,
                arguments = listOf(navArgument("storeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getLong("storeId") ?: 0L
                ProductListScreen(
                    viewModel = productViewModel,
                    storeId = storeId,
                    onAddProduct = { navController.navigate(Screen.AddEditProduct.createRoute(storeId)) },
                    onEditProduct = { productId -> navController.navigate(Screen.AddEditProduct.createRoute(storeId, productId)) },
                    onNavigateBack = { navController.popBackStack() },
                    onProductClick = { productId -> navController.navigate(Screen.ProductComparison.createRoute(productId)) }
                )
            }
            composable(
                route = Screen.AddEditProduct.route,
                arguments = listOf(
                    navArgument("storeId") { 
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("productId") { 
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getLong("storeId")?.takeIf { it != -1L }
                val productId = backStackEntry.arguments?.getLong("productId")?.takeIf { it != -1L }
                AddEditProductScreen(
                    viewModel = productViewModel,
                    initialStoreId = storeId,
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.ProductComparison.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                ProductComparisonScreen(
                    viewModel = productViewModel,
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onHistoryClick = { pId, storeId -> 
                        navController.navigate(Screen.PriceHistory.createRoute(pId, storeId))
                    }
                )
            }
            composable(
                route = Screen.PriceHistory.route,
                arguments = listOf(
                    navArgument("productId") { type = NavType.LongType },
                    navArgument("storeId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                val storeId = backStackEntry.arguments?.getLong("storeId") ?: 0L
                PriceHistoryScreen(
                    viewModel = productViewModel,
                    productId = productId,
                    storeId = storeId,
                    onNavigateBack = { navController.popBackStack() },
                    onCompareStores = { navController.popBackStack() }
                )
            }
        }
    }
}
