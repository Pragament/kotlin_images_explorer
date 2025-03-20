package com.pragament.kotlin_images_explorer.presentation.navigation.graph

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.pragament.kotlin_images_explorer.presentation.navigation.Dest
import com.pragament.kotlin_images_explorer.presentation.navigation.SubGraph
import com.pragament.kotlin_images_explorer.presentation.navigation.bottomnav.KotlinImagesExplorerScreen
import com.pragament.kotlin_images_explorer.presentation.screens.*
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.homeNavGraph(navController: NavController) {
    navigation<SubGraph.Home>(startDestination = Dest.KotlinImagesExplorerScreen) {

        composable<Dest.KotlinImagesExplorerScreen> {
            KotlinImagesExplorerScreen()
        }

        composable<Dest.HomeScreen> {
            HomeScreen(
                onNavigateToFilteredImages = {
                    navController.navigate(Dest.FilteredImagesScreen)
                }
            )
        }

        composable<Dest.SettingsScreen> {
            SettingsScreen()
        }

        composable<Dest.TaggedImagesScreen> {
            val args = it.toRoute<Dest.TaggedImagesScreen>()
            TaggedImagesScreen(
                tag = args.tag,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Dest.FilteredImagesScreen> {
            FilteredImagesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}