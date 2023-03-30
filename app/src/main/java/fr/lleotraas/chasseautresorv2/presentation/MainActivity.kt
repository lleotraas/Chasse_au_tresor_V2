package fr.lleotraas.chasseautresorv2.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
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
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.clearRouteLine
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import dagger.hilt.android.AndroidEntryPoint
import fr.lleotraas.chasseautresorv2.R
import fr.lleotraas.chasseautresorv2.presentation.map.DirectionState
import fr.lleotraas.chasseautresorv2.presentation.map.MapEvent
import fr.lleotraas.chasseautresorv2.presentation.map.MapScreen
import fr.lleotraas.chasseautresorv2.presentation.map.MapViewModel
import fr.lleotraas.chasseautresorv2.presentation.map.utils.MarkerUtils
import fr.lleotraas.chasseautresorv2.presentation.map.utils.Screen
import fr.lleotraas.chasseautresorv2.presentation.map.utils.Utils
import fr.lleotraas.chasseautresorv2.ui.theme.ChasseAuTresorV2Theme
import fr.lleotraas.chasseautresorv2.utils.isPermanentDenied

@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity(),OnMapClickListener , OnMapLongClickListener, MapboxNavigationObserver {

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
    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onInitialize = this::createMapboxApp
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        getLastKnownPosition()
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
                                    removeMarkerFromMap(pointAnnotationManager, state.pointList)
                                    updateRoute(state)
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

    private fun getLastKnownPosition() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                viewModel.event(MapEvent.UpdateLastKnownPosition(location!!))
            }
        }
    }

    private fun updateRoute(state: DirectionState) {
        if (state.pointList.isNotEmpty()) {
            val origin = Utils.createOrigin(state.lastKnownLocation!!, this.state)
            val destination = Utils.createDestination(state, state.pointList.last())
            val originLocation = Utils.createOriginLocation(state, origin)
            val routeOptions = createRouteOptions(originLocation, origin, destination)
            drawRouteOnMap(routeOptions, mapboxNavigation)
        }
        else {
            val routeLineOptions = MapboxRouteLineOptions.Builder(this@MainActivity).build()
            val routeLineApi = MapboxRouteLineApi(routeLineOptions)
            val routeLineView = MapboxRouteLineView(routeLineOptions)
            routeLineApi.clearRouteLine { value ->
                routeLineView.renderClearRouteLineValue(mapView.getMapboxMap().getStyle()!!, value)
            }

        }
    }

    private fun createMapboxApp() {
        val string = this.resources.getString(R.string.mapbox_access_token)
        MapboxNavigationApp.setup (
            NavigationOptions.Builder(this)
                .accessToken(string)
                .build()
        )
    }

    override fun onMapClick(point: Point): Boolean {
        addMarkerToView(point)
        viewModel.event(MapEvent.AddMarker(point))
        val origin = Utils.createOrigin(state.lastKnownLocation!!, state)
        val destination = Utils.createDestination(state, point)
        val routeOptions = createRouteOptions(state.lastKnownLocation!!, origin, destination)
        drawRouteOnMap(routeOptions, mapboxNavigation)
        return true
    }

    private fun drawRouteOnMap(routeOptions: RouteOptions, mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onCanceled(
                    routeOptions: RouteOptions,
                    routerOrigin: RouterOrigin
                ) {
                    Log.e(TAG, "onRoutesReady: navigation canceled")
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    Log.e(TAG, "onRoutesReady: navigation failure")
                }

                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    viewModel.event(MapEvent.UpdateRoutes(routes))
                    val routeLineOptions = MapboxRouteLineOptions.Builder(this@MainActivity).build()
                    val routeLineApi = MapboxRouteLineApi(routeLineOptions)
                    val routeLineView = MapboxRouteLineView(routeLineOptions)
                    routeLineApi.setNavigationRoutes(routes) { value ->
                        routeLineView.renderRouteDrawData(mapView.getMapboxMap().getStyle()!!, value)
                    }
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val json = routes.map {
                        gson.toJson(
                            JsonParser.parseString(it.directionsRoute.toJson())
                        )
                    }
//                            Log.e(TAG, """onRoutesReady: ""|routes ready (origin: ${routerOrigin::class.simpleName}):|$json""".trimMargin())
                }
            }
        )
    }

    private fun createRouteOptions(
        originLocation: Location,
        origin: Point,
        destination: Point
    ): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinatesList(listOf(origin, destination))
            .alternatives(false)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .bearingsList(
                listOf(
                    Bearing.builder()
                        .angle(originLocation.bearing.toDouble())
                        .degrees(45.0)
                        .build(),
                    null
                )
            )
            .build()
    }

    override fun onMapLongClick(point: Point): Boolean {
//        addMarkerToView(point)
//        viewModel.event(MapEvent.AddMarker(point))
//        val origin = Utils.createOrigin(state.lastKnownLocation!!, state)
//        val destination = Utils.createDestination(state, point)
//        val routeOptions = createRouteOptions(state.lastKnownLocation!!, origin, destination)
//        drawRouteOnMap(routeOptions, mapboxNavigation)
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

    override fun onAttached(mapboxNavigation: MapboxNavigation) {

    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {

    }

//    init {
//       lifecycle.addObserver(object : DefaultLifecycleObserver {
//            override fun onResume(owner: LifecycleOwner) {
//                super.onResume(owner)
//                mapboxNavigation.attach(owner)
//            }
//
//            override fun onPause(owner: LifecycleOwner) {
//                super.onPause(owner)
//                mapboxNavigation.detach(owner)
//            }
//        })
//    }

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

