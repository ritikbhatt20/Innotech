package com.example.innotech

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants.LOCATION_INDICATOR_LAYER
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp.setup
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import java.util.Arrays
import java.util.Objects
import kotlin.properties.Delegates

class MapsActivity : AppCompatActivity() {
    private var mapView: MapView? = null
    private var focusLocationBtn: FloatingActionButton? = null
    private val navigationLocationProvider = NavigationLocationProvider()
    private var routeLineView: MapboxRouteLineView? = null
    private var routeLineApi: MapboxRouteLineApi? = null
    private var ambulanceLatitude by Delegates.notNull<Double>()
    private var ambulanceLongitude by Delegates.notNull<Double>()
    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onNewRawLocation(location: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val location = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                location,
                locationMatcherResult.keyPoints,
                null,
                null
            )
            if (focusLocation) {
                updateCamera(
                    Point.fromLngLat(location.longitude, location.latitude),
                    location.bearing.toDouble()
                )
            }
        }
    }
    private val routesObserver = RoutesObserver { routesUpdatedResult ->
        routeLineApi?.setNavigationRoutes(
            routesUpdatedResult.navigationRoutes
        ) { routeLineErrorRouteSetValueExpected ->
            val style = mapView?.getMapboxMap()?.getStyle()
            if (style != null) {
                routeLineView?.renderRouteDrawData(
                    style,
                    routeLineErrorRouteSetValueExpected
                )
            }
        }
    }
    private var focusLocation = true
    private var mapboxNavigation: MapboxNavigation? = null

    private fun updateCamera(point: Point, bearing: Double) {
        val animationOptions = MapAnimationOptions.Builder().duration(1500L).build()
        val cameraOptions =
            CameraOptions.Builder().center(point).zoom(18.0).bearing(bearing).pitch(45.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0)).build()
        mapView?.camera?.easeTo(cameraOptions, animationOptions)
    }

    private val onMoveListener: OnMoveListener = object : OnMoveListener {
        override fun onMoveBegin(moveGestureDetector: MoveGestureDetector) {
            focusLocation = false
            mapView?.gestures?.removeOnMoveListener(this)
            focusLocationBtn?.show()
        }

        override fun onMove(moveGestureDetector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(moveGestureDetector: MoveGestureDetector) {}
    }
    private val activityResultLauncher = registerForActivityResult<String, Boolean>(
        ActivityResultContracts.RequestPermission(),
        object : ActivityResultCallback<Boolean?> {
            override fun onActivityResult(result: Boolean?) {
                if (result == true) {
                    Toast.makeText(
                        this@MapsActivity,
                        "Permission granted! Restart this app",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        mapView = findViewById(R.id.mapView)
        focusLocationBtn = findViewById(R.id.focusLocation)
        val ambulanceLatitude = intent.getDoubleExtra("latitude", 0.0)
        val ambulanceLongitude = intent.getDoubleExtra("longitude", 0.0)

        val options: MapboxRouteLineOptions =
            MapboxRouteLineOptions.Builder(this).withRouteLineResources(
                RouteLineResources.Builder().build()
            )
                .withRouteLineBelowLayerId(LOCATION_INDICATOR_LAYER).build()
        routeLineView = MapboxRouteLineView(options)
        routeLineApi = MapboxRouteLineApi(options)
        val navigationOptions: NavigationOptions =
            NavigationOptions.Builder(this).accessToken(getString(R.string.mapbox_access_token)).build()
        setup(navigationOptions)
        mapboxNavigation = MapboxNavigation(navigationOptions)
        mapboxNavigation?.registerRoutesObserver(routesObserver)
        mapboxNavigation?.registerLocationObserver(locationObserver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this@MapsActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this@MapsActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this@MapsActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            activityResultLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            mapboxNavigation?.startTripSession()
        }
        val setRoute = findViewById<MaterialButton>(R.id.setRoute)
        focusLocationBtn?.hide()
        val locationComponentPlugin = mapView?.location
        mapView?.gestures?.addOnMoveListener(onMoveListener)
        mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS, object : Style.OnStyleLoaded {
            override fun onStyleLoaded(style: Style) {
                mapView?.getMapboxMap()?.setCamera(CameraOptions.Builder().zoom(20.0).build())
                locationComponentPlugin?.enabled = true
                locationComponentPlugin?.setLocationProvider(navigationLocationProvider)
                mapView?.gestures?.addOnMoveListener(onMoveListener)
//                locationComponentPlugin.updateSettings { locationComponentSettings ->
//                    locationComponentSettings?.isEnabled = true
//                    locationComponentSettings?.pulsingEnabled = true
//                    return@updateSettings null
//                }
                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ambulance)
                val annotationPlugin = mapView?.annotations
                val pointAnnotationManager = annotationPlugin?.createPointAnnotationManager(mapView!!)
                val point = Point.fromLngLat(ambulanceLongitude, ambulanceLatitude)
                val pointAnnotationOptions =
                    PointAnnotationOptions().withTextAnchor(TextAnchor.CENTER).withIconImage(bitmap)
                        .withPoint(point)
                pointAnnotationManager?.create(pointAnnotationOptions)
                setRoute.setOnClickListener {
                    fetchRoute(point)
                    setRoute.visibility = View.GONE
                }
                focusLocationBtn?.setOnClickListener(View.OnClickListener {
                    focusLocation = true
                    fetchRoute(point)
                    mapView?.gestures?.addOnMoveListener(onMoveListener)
                    focusLocationBtn?.hide()
                })
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun fetchRoute(point: Point) {
        val locationEngine = LocationEngineProvider.getBestLocationEngine(this@MapsActivity)
        locationEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                val location = result.lastLocation
                val builder = RouteOptions.builder()
                val origin = Objects.requireNonNull(location)?.let {
                    Point.fromLngLat(
                        it.longitude,
                        location!!.latitude
                    )
                }
                builder.coordinatesList(Arrays.asList(origin, point))
                builder.alternatives(false)
                builder.profile(DirectionsCriteria.PROFILE_DRIVING)
                if (location != null) {
                    builder.bearingsList(
                        Arrays.asList(
                            Bearing.builder().angle(location.bearing.toDouble()).degrees(45.0).build(), null
                        )
                    )
                }
                builder.applyDefaultNavigationOptions()
                mapboxNavigation?.requestRoutes(
                    builder.build(),
                    object : NavigationRouterCallback {
                        override fun onRoutesReady(
                            list: List<NavigationRoute>,
                            routerOrigin: RouterOrigin
                        ) {
                            mapboxNavigation?.setNavigationRoutes(list)
                            focusLocationBtn?.performClick()
                        }

                        override fun onFailure(
                            list: List<RouterFailure>,
                            routeOptions: RouteOptions
                        ) {
                            Toast.makeText(this@MapsActivity, "Route request failed", Toast.LENGTH_SHORT).show()
                        }

                        override fun onCanceled(
                            routeOptions: RouteOptions,
                            routerOrigin: RouterOrigin
                        ) {
                        }
                    })
            }

            override fun onFailure(exception: Exception) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.onDestroy()
        mapboxNavigation?.unregisterRoutesObserver(routesObserver)
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
    }
}
