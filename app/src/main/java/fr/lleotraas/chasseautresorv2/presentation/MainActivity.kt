package fr.lleotraas.chasseautresorv2.presentation

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WrongLocation
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.location2
import dagger.hilt.android.AndroidEntryPoint
import fr.lleotraas.chasseautresorv2.R
import fr.lleotraas.chasseautresorv2.presentation.map.DirectionState
import fr.lleotraas.chasseautresorv2.presentation.map.MapEvent
import fr.lleotraas.chasseautresorv2.presentation.map.MapScreen
import fr.lleotraas.chasseautresorv2.presentation.map.MapViewModel
import fr.lleotraas.chasseautresorv2.presentation.map.utils.MarkerUtils
import fr.lleotraas.chasseautresorv2.presentation.map.utils.Screen
import fr.lleotraas.chasseautresorv2.ui.theme.ChasseAuTresorV2Theme
import fr.lleotraas.chasseautresorv2.utils.Utils.ROUTE_SOURCE_ID
import fr.lleotraas.chasseautresorv2.utils.isPermanentDenied

@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity(),OnMapClickListener , OnMapLongClickListener {

    companion object {
        const val TAG = "Main Activity"
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed(
                mapView,
                applicationContext,
                onMoveListener = this,
                onIndicatorPositionChangedListener = onIndicatorPositionChangedListener,
                onIndicatorBearingChangedListener = onIndicatorBearingChangedListener
            )
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private val viewModel: MapViewModel by viewModels()
    private lateinit var state: DirectionState


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        setContent {
            ChasseAuTresorV2Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val scaffoldState = rememberScaffoldState()
                    state = viewModel.state.value
                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.event(MapEvent.RemoveLastMarkers(state.pointList))
                        removeMarkerFromMap(
                            pointAnnotationManager,
                            state.pointList
                        )
                                }
                            ){
                                Icon(
                                    imageVector = Icons.Default.WrongLocation,
                                    contentDescription = this.resources.getString(
                                        R.string.remove_marker
                                    )
                                )
                            }
                        },
                        scaffoldState = scaffoldState
                    ) {
                        it.apply {  }
                        val navController = rememberNavController()
                        val permissionState = rememberMultiplePermissionsState(
                            permissions = listOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                        val lifecycleOwner = LocalLifecycleOwner.current
                        DisposableEffect(
                            key1 = lifecycleOwner,
                            effect = {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_START) {
                                        permissionState.launchMultiplePermissionRequest()
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)

                                onDispose {
                                    lifecycleOwner.lifecycle.removeObserver(observer)
                                }
                            })
                        var isCoarseAccepted = false
                        var isFineAccepted = false
                        permissionState.permissions.forEach { perm ->
                            when (perm.permission) {
                                Manifest.permission.ACCESS_COARSE_LOCATION -> {
                                    when {
                                        perm.hasPermission -> isCoarseAccepted = true
                                        perm.shouldShowRationale -> isCoarseAccepted = false
                                        perm.isPermanentDenied() -> isCoarseAccepted = false
                                    }
                                }
                                Manifest.permission.ACCESS_FINE_LOCATION -> {
                                    when {
                                        perm.hasPermission -> isFineAccepted = true
                                        perm.shouldShowRationale -> isFineAccepted = false
                                        perm.isPermanentDenied() -> isFineAccepted = false
                                    }
                                }
                            }
                        }
                        NavHost(
                            navController = navController,
                            startDestination = Screen.MapScreen.route
                        ) {
                            composable(route = Screen.MapScreen.route) {
                                MapScreen(
                                    navController = navController,
                                    mapView = mapView,
                                    isPermissionsAccepted = isCoarseAccepted && isFineAccepted,
                                    onMoveListener = onMoveListener,
                                    onIndicatorPositionChangedListener = onIndicatorPositionChangedListener,
                                    onIndicatorBearingChangedListener = onIndicatorBearingChangedListener,
                                    mainActivity = this@MainActivity,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onMapClick(point: Point): Boolean {
        addMarkerToView(point)
        viewModel.event(MapEvent.AddMarker(point))
        viewModel.getDirection(
            "walking",
            "${state.pointList.first().longitude()},${state.pointList.first().latitude()};${point.longitude()},${point.latitude()}",
            "geojson",
            this.resources.getString(R.string.mapbox_access_token)
        )
        state.route.forEach {route ->
            Log.e(TAG, "onMapClick: distance=${route.distance} geometry type${route.geometry.type}", )
            mapView.getMapboxMap().getStyle {style ->
                val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
            }
        }
        return true
    }

    override fun onMapLongClick(point: Point): Boolean {
//        addMarkerToPoint(point)
//        pointList.add(point)
        return true
    }

    private fun removeMarkerFromMap(pointAnnotationManager: PointAnnotationManager, pointList: List<Point>) {
        pointAnnotationManager.deleteAll()
        pointList.forEach {
            addMarkerToView(it)
        }
    }

    private fun addMarkerToView(point: Point) {
        MarkerUtils.bitmapFromDrawableRes(
            this,
            R.drawable.baseline_location_on_24
        )?.let{
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(point.longitude(), point.latitude()))
                .withIconImage(it)
            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }
}

private fun onCameraTrackingDismissed(
    mapView: MapView,
    context: Context,
    onMoveListener: OnMoveListener,
    onIndicatorPositionChangedListener: OnIndicatorPositionChangedListener,
    onIndicatorBearingChangedListener: OnIndicatorBearingChangedListener
) {
    Toast.makeText(context, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
    mapView.location
        .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
    mapView.location
        .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
    mapView.gestures.removeOnMoveListener(onMoveListener)
}