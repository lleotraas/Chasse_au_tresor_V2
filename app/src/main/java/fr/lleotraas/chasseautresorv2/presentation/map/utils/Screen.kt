package fr.lleotraas.chasseautresorv2.presentation.map.utils

sealed class Screen(val route: String) {
    object MapScreen: Screen("map_screen")
}
