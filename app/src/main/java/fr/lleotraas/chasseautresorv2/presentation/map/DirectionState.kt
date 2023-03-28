package fr.lleotraas.chasseautresorv2.presentation.map

import com.mapbox.geojson.Point
import fr.lleotraas.chasseautresorv2.domain.model.Route

data class DirectionState(
    val route: List<Route> = emptyList(),
    val pointList: ArrayList<Point> = ArrayList()
)
