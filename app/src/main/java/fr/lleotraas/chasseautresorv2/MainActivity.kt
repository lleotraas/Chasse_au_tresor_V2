package fr.lleotraas.chasseautresorv2

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WrongLocation
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.*
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import fr.lleotraas.chasseautresorv2.ui.theme.ChasseAuTresorV2Theme
import fr.lleotraas.chasseautresorv2.utils.isPermanentDenied

@ExperimentalPermissionsApi
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
    private lateinit var viewAnnotationManager: ViewAnnotationManager
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private val pointList = mutableListOf<Point>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        viewAnnotationManager = mapView.viewAnnotationManager
        setContent {
            ChasseAuTresorV2Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
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
                    val scaffoldState = rememberScaffoldState()
                    if (isCoarseAccepted && isFineAccepted) {
                        Scaffold(
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = {
                                        if (pointList.isNotEmpty()) {
                                            pointList.remove(pointList.last())
                                            removeMarkerFromMap()
                                        }
                                    }
                                ){
                                    Icon(
                                        imageVector = Icons.Default.WrongLocation,
                                        contentDescription = applicationContext.resources.getString(
                                            R.string.remove_marker
                                        )
                                    )
                                }
                            },
                            scaffoldState = scaffoldState
                        ) {
                            it.apply {  }
                            MapboxScreen(
                                mapView,
                                onMoveListener,
                                onIndicatorPositionChangedListener,
                                onIndicatorBearingChangedListener,
                                this
                            )
                        }



                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = applicationContext.resources.getString(R.string.permission_denied))
                        }
                    }
                }
            }
        }
    }

    private fun removeMarkerFromMap() {
        pointAnnotationManager.deleteAll()
        pointList.forEach {
            addMarkerToPoint(it)
        }
    }

    override fun onMapClick(point: Point): Boolean {
//        addViewAnnotation(point)
        addMarkerToPoint(point)
        pointList.add(point)
        return true
    }

    override fun onMapLongClick(point: Point): Boolean {
//        addMarkerToPoint(point)
//        pointList.add(point)
        return true
    }

    private fun addMarkerToPoint(point: Point) {
        bitmapFromDrawableRes(
            this,
            R.drawable.baseline_location_on_24
        )?.let{
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(point.longitude(), point.latitude()))
                .withIconImage(it)
            pointAnnotationManager.create(pointAnnotationOptions)

        }
    }


    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(ContextCompat.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else  {
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0,0,canvas.width*2, canvas.height*2)
            drawable.draw(canvas)
            bitmap
        }
    }

//    private fun addViewAnnotation(point: Point) {
//        val viewAnnotation = viewAnnotationManager.addViewAnnotation(
//            ContextCompat.getDrawable(this, R.drawable.baseline_location_on_24),
//            options = viewAnnotationOptions {
//                geometry(point)
//                allowOverlap(true)
//            }
//        )
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapboxScreen(
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
                    addAnnotationToMap(mapView, context)
                }
            }
        })
    }
}

fun addAnnotationToMap(mapView: MapView, context: Context) {
    bitmapFromDrawableRes(
        context,
        R.drawable.baseline_location_on_24
    )?.let{
        val annotationApi = mapView.annotations
        val pointAnnotationManager = annotationApi.createPointAnnotationManager()
        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(18.06, 59.31))
            .withIconImage(it)
        pointAnnotationManager.create(pointAnnotationOptions)
    }
}

private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
    convertDrawableToBitmap(ContextCompat.getDrawable(context, resourceId))

fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
    if (sourceDrawable == null) {
        return null
    }
    return if (sourceDrawable is BitmapDrawable) {
        sourceDrawable.bitmap
    } else  {
        val constantState = sourceDrawable.constantState ?: return null
        val drawable = constantState.newDrawable().mutate()
        val bitmap: Bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0,0,canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
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

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ChasseAuTresorV2Theme {
        Greeting("Android")
    }

}