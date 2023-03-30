package fr.lleotraas.chasseautresorv2.presentation.map

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import fr.lleotraas.chasseautresorv2.domain.model.Route

data class DirectionState(
    val route: ArrayList<Route> = ArrayList(),
    val pointList: ArrayList<Point> = ArrayList(),
    val routes: List<NavigationRoute> = emptyList(),
    val lastKnownLocation: Location? = null
)
