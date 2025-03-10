package com.pragament.kotlin_images_explorer.presentation.navigation.bottomnav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pragament.kotlin_images_explorer.presentation.navigation.Dest
import com.pragament.kotlin_images_explorer.presentation.navigation.graph.homeNavGraph
import com.pragament.kotlin_images_explorer.presentation.screens.HomeScreen
import com.pragament.kotlin_images_explorer.presentation.screens.SettingsScreen
import com.pragament.kotlin_images_explorer.presentation.screens.TaggedImagesListScreen
import com.pragament.kotlin_images_explorer.presentation.screens.TaggedImagesScreen

@Composable
fun KotlinImagesExplorerScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            KotlinImagesExplorerNavBar(
                currentDestination = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = KotlinImagesExplorerBottomNavItem.HOME.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(KotlinImagesExplorerBottomNavItem.HOME.route) {
                HomeScreen()
            }

            composable(KotlinImagesExplorerBottomNavItem.RESULT.route) {
                TaggedImagesListScreen(
                    onTagSelected = { tag ->
                        navController.navigate(Dest.TaggedImagesScreen(tag))
                    }
                )
            }

            composable(KotlinImagesExplorerBottomNavItem.SETTINGS.route) {
                SettingsScreen()
            }

            composable<Dest.TaggedImagesScreen> {
                val args = it.toRoute<Dest.TaggedImagesScreen>()
                TaggedImagesScreen(
                    tag = args.tag,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            homeNavGraph(navController)
        }
    }
}