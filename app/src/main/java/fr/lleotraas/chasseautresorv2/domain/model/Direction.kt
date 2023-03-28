package fr.lleotraas.chasseautresorv2.domain.model

data class Direction(
    val code: String,
    val routes: List<Route>,
    val uuid: String,
    val waypoints: List<Waypoint>
)