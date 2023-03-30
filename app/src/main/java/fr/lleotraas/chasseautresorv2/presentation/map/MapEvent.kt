package fr.lleotraas.chasseautresorv2.presentation.map

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.navigation.base.route.NavigationRoute
import fr.lleotraas.chasseautresorv2.domain.model.Route

sealed class MapEvent{
    data class AddMarker(val point: Point): MapEvent()
    data class RemoveLastMarkers(val pointList: List<Point>): MapEvent()
    data class UpdateRoutes(val routes: List<NavigationRoute>): MapEvent()
    data class UpdateLastKnownPosition(val location: Location): MapEvent()
}
