package fr.lleotraas.chasseautresorv2.presentation.map

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import fr.lleotraas.chasseautresorv2.R
import fr.lleotraas.chasseautresorv2.presentation.MainActivity
import fr.lleotraas.chasseautresorv2.presentation.map.component.MapBox
import fr.lleotraas.chasseautresorv2.presentation.map.utils.MarkerUtils

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel,
    mapView: MapView,
    isPermissionsAccepted: Boolean,
    onMoveListener: OnMoveListener,
    onIndicatorPositionChangedListener: OnIndicatorPositionChangedListener,
    onIndicatorBearingChangedListener: OnIndicatorBearingChangedListener,
    mainActivity: MainActivity
) {
    if (isPermissionsAccepted) {
        MapBox(
            mapView,
            onMoveListener = onMoveListener,
            onIndicatorPositionChangedListener = onIndicatorPositionChangedListener,
            onIndicatorBearingChangedListener = onIndicatorBearingChangedListener,
            mainActivity = mainActivity
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = navController.context.resources.getString(R.string.permission_denied))
        }
    }
}