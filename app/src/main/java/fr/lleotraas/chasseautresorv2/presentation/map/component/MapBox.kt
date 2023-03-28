package fr.lleotraas.chasseautresorv2.presentation.map.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import fr.lleotraas.chasseautresorv2.R
import fr.lleotraas.chasseautresorv2.presentation.MainActivity

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapBox(
    mapView: MapView,
    onMoveListener: OnMoveListener,
    onIndicatorPositionChangedListener: OnIndicatorPositionChangedListener,
    onIndicatorBearingChangedListener: OnIndicatorBearingChangedListener,
    mainActivity: MainActivity
) {
    Box(modifier = Modifier
        .background(MaterialTheme.colors.background)
        .fillMaxSize()) {
        AndroidView(factory = { context ->
            ResourceOptionsManager.getDefault(
                context,
                context.getString(R.string.mapbox_access_token)
            )
            mapView.apply {

                getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .zoom(14.0)
                        .build()
                )

                getMapboxMap().loadStyleUri(
                    Style.MAPBOX_STREETS
                ) {
                    getMapboxMap().addOnMapClickListener(mainActivity)
                    location.updateSettings {
                        enabled = true
                        pulsingEnabled = true

                    }
                    initLocationComponent(mapView, context, onIndicatorPositionChangedListener, onIndicatorBearingChangedListener)
                    setupGesturesListener(mapView, onMoveListener)
//                    addAnnotationToMap(mapView, context)
                }
            }
        })
    }
}

private fun initLocationComponent(
    mapView: MapView,
    context: Context,
    onIndicatorPositionChangedListener: OnIndicatorPositionChangedListener,
    onIndicatorBearingChangedListener: OnIndicatorBearingChangedListener
) {
    val locationComponentPlugin = mapView.location
    locationComponentPlugin.updateSettings {
        this.enabled = true
        this.locationPuck = LocationPuck2D(
            bearingImage =  ContextCompat.getDrawable(context, com.mapbox.maps.R.drawable.mapbox_user_puck_icon),
            shadowImage =  ContextCompat.getDrawable(context, com.mapbox.maps.R.drawable.mapbox_user_icon_shadow),
            scaleExpression = interpolate {
                linear()
                zoom()
                stop {
                    literal(0.0)
                    literal(0.6)
                }
                stop {
                    literal(20.0)
                    literal(1.0)
                }
            }.toJson()
        )
    }
    locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
    locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
}

private fun setupGesturesListener(mapView: MapView, onMoveListener: OnMoveListener) {
    mapView.gestures.addOnMoveListener(onMoveListener)
}