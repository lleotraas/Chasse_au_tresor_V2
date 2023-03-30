package fr.lleotraas.chasseautresorv2.presentation.map.utils

import android.location.Location
import com.mapbox.geojson.Point
import fr.lleotraas.chasseautresorv2.presentation.map.DirectionState

object Utils {

    fun createDestination(state: DirectionState, point: Point): Point {
        return if (state.routes.isEmpty()) {
            Point.fromLngLat(
                if (state.pointList.size == 1)
                    point.longitude()
                else
                    state.pointList.first().longitude(),

                if (state.pointList.size == 1)
                    point.latitude()
                else
                    state.pointList.first().latitude(),
            )
        } else {
            Point.fromLngLat(
                point.longitude(),
                point.latitude()
            )
        }
    }

    fun createOrigin(location: Location, state: DirectionState): Point {
        return Point.fromLngLat(
            if (state.pointList.size == 1)
                location.longitude
            else
                state.pointList[state.pointList.size - 2].longitude(),

            if (state.pointList.size == 1)
                location.latitude
            else
                state.pointList[state.pointList.size - 2].latitude()
        )

    }

    fun createOriginLocation(state: DirectionState, location: Point): Location {
        return Location("last_know_location").apply {
            longitude = if (state.pointList.size == 1) location.longitude() else state.pointList[state.pointList.size - 2].longitude()
            latitude = if (state.pointList.size == 1) location.latitude() else state.pointList[state.pointList.size - 2].latitude()
            bearing = 10f
        }
    }

}