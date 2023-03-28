package fr.lleotraas.chasseautresorv2.presentation.map

import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager

sealed class MapEvent{
    data class AddMarker(val point: Point): MapEvent()
    data class RemoveLastMarkers(val pointList: List<Point>): MapEvent()
}
