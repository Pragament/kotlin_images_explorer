package com.pragament.kotlin_images_explorer.presentation.navigation

import kotlinx.serialization.Serializable

sealed class SubGraph {
    @Serializable
    data object Home : SubGraph()
    @Serializable
    data object Settings : SubGraph()
}

sealed interface Dest {
    @Serializable
    data object KotlinImagesExplorerScreen: Dest

    @Serializable
    data object HomeScreen : Dest

    @Serializable
    data object SettingsScreen : Dest

    @Serializable
    data class TaggedImagesScreen(val tag: String) : Dest
}