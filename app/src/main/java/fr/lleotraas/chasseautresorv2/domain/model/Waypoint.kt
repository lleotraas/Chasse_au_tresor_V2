package fr.lleotraas.chasseautresorv2.domain.model

data class Waypoint(
    val distance: Double,
    val location: List<Double>,
    val name: String
)