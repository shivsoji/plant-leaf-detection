package com.heartyculture.app

import androidx.camera.view.PreviewView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.heartyculture.app.screens.DiseaseScreen
import com.heartyculture.app.screens.GalleryScreen
import com.heartyculture.app.screens.ImageDetailScreen
import com.heartyculture.app.screens.PlantScreen
import com.heartyculture.app.viewModels.DetectorViewModel

sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
    data object Plant : BottomNavItem("plant", Icons.Default.Search, "Plant")
    data object Disease : BottomNavItem("disease", Icons.Default.Home, "Disease")
    data object Gallery : BottomNavItem("gallery", Icons.Default.List, "Gallery")
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
//        BottomNavItem.Disease,
        BottomNavItem.Plant,
        BottomNavItem.Gallery,

    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController, viewModel: DetectorViewModel, viewFinder: PreviewView) {
    NavHost(navController, startDestination = BottomNavItem.Plant.route) {
        composable(BottomNavItem.Disease.route) {
            DiseaseScreen(viewModel = viewModel, viewFinder = viewFinder)
        }
        composable(BottomNavItem.Gallery.route) {
            GalleryScreen(viewModel = viewModel, navController = navController)
        }
        composable(BottomNavItem.Plant.route) {
            PlantScreen(viewModel = viewModel, viewFinder = viewFinder)
        }
        composable("image_detail/{plant}/{disease}",  arguments = listOf(
            navArgument("plant") { type = NavType.StringType },
            navArgument("disease") { type = NavType.StringType }
        )) { backStackEntry ->
            val plant = backStackEntry.arguments?.getString("plant") ?: "Unknown"
            val disease = backStackEntry.arguments?.getString("disease") ?: "Unknown"
            ImageDetailScreen(
                plant = plant,
                disease = disease,
                onBack = { navController.popBackStack() },
                onDelete = {
                    // Implement delete logic
                    navController.popBackStack()
                }
            )
        }
    }
}