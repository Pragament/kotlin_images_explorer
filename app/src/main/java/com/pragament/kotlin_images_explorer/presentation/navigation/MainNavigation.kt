package com.pragament.kotlin_images_explorer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.pragament.kotlin_images_explorer.presentation.navigation.graph.homeNavGraph

@Composable
fun MainNavigation(
    navController: NavHostController,
    modifier: Modifier
) {
    NavHost(
        navController = navController,
        startDestination = SubGraph.Home
    ) {
        homeNavGraph(navController)
    }
}