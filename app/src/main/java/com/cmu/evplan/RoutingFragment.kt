package com.cmu.evplan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cmu.evplan.databinding.FragmentRoutingBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.Response
import com.cmu.evplan.BuildConfig.MAPS_API_KEY
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.model.Place

import java.util.Arrays
class RoutingFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentRoutingBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: RoutingViewModel by activityViewModels()

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    private var markerInit = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRoutingBinding.inflate(inflater, container, false)
        val view = binding.root

        val mapFragment = childFragmentManager.findFragmentById(R.id.routeMap) as SupportMapFragment?
        if (viewModel.getSrc()?.name == null) {
            mapFragment?.getMapAsync { errorCatchCallback }
        } else if (viewModel.getDst()?.name == null) {
            viewModel.setDst(viewModel.getSrc()!!)
            mapFragment?.getMapAsync {errorCatchCallback}
        } else {
            mapFragment?.getMapAsync(callback)
        }

        _binding!!.destination.setText(viewModel.getDst()?.name)
        _binding!!.destinationBlock.setOnClickListener {
            viewModel.setStatus(SearchStatus.Destination)
            findNavController().navigate(R.id.action_routingFragment_to_searchFragment)
        }
        _binding!!.current.setText(viewModel.getSrc()?.name)
        _binding!!.currentBlock.setOnClickListener {
            viewModel.setStatus(SearchStatus.Source)
            findNavController().navigate(R.id.action_routingFragment_to_searchFragment)
        }
        _binding!!.routingBackButton.setOnClickListener {
            findNavController().navigate(R.id.action_routingFragment_to_searchFragment)
        }

        return view
    }

    fun MetersToMiles(meters: Float) : Float {
        return (meters * 0.000621371192).toFloat()
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(mapFragment.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Make sure fine location's permission is issued", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(mapFragment.requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MapsFragment.LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        if (ActivityCompat.checkSelfPermission(mapFragment.requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mapFragment.requireActivity(),
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                MapsFragment.LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(map : GoogleMap) {
        fusedLocationClient.lastLocation.addOnSuccessListener(mapFragment.requireActivity()) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

                val src: Place = Place.builder()
                    .setLatLng(currentLatLng)
                    .setName("Current Location")
                    .build()
                viewModel.setSrc(src)
            }
        }
    }

    // Pulls from an EV Station API and parses it to plot all EV stations in the US on the map
    private fun processEVJson(googleMap: GoogleMap) {
        if (markerInit)         return
        // val markers: MutableList<LatLng> = ArrayList()
        // val markers = mutableMapOf<String, LatLng>()
        var markers: MutableList<MarkerType> = ArrayList()

        val queue = Volley.newRequestQueue(context)
        val evURL = "https://services.arcgis.com/xOi1kZaI0eWDREZv/arcgis/rest/services/Alternative_Fueling_Stations/FeatureServer/0/query?where=state%20%3D%20%27CA%27%20AND%20fuel_type_code%20%3D%20%27ELEC%27&outFields=fuel_type_code,id,station_name,facility_type,city,state,street_address,zip,country,ev_connector_types,ev_network,ev_network_web,ev_other_evse,ev_pricing,ev_renewable_source,longitude,latitude,ev_level1_evse_num,ev_dc_fast_num,ev_level2_evse_num&outSR=4326&f=json"
        val evStationRequest = object : StringRequest(Request.Method.GET, evURL, Response.Listener<String> {
                response ->
            val evJsonResponse = JSONObject(response)
            // Get EV Stations
            val evStations = evJsonResponse.getJSONArray("features")
            // Log.e("Test", evStations.getJSONObject(0).getJSONObject("attributes").getString("LATITUDE"))
            for (i in 0 until evStations.length()) {
                var markerType: MarkerType = MarkerType()
                val latitude = evStations.getJSONObject(i).getJSONObject("attributes").getDouble("latitude")
                val longitude = evStations.getJSONObject(i).getJSONObject("attributes").getDouble("longitude")
                val stationName = evStations.getJSONObject(i).getJSONObject("attributes").getString("station_name")
                val connector = evStations.getJSONObject(i).getJSONObject("attributes").getString("ev_connector_types");

                var chargeOutput = "connector type: $connector"
                var latLong = LatLng(latitude, longitude)
                markerType.chargerType = connector
                markerType.stationName = stationName
                markerType.location = latLong
                markers.add(markerType)
                // println("adding marker" + markerType.location.latitude)
//                googleMap.addMarker(MarkerOptions().position(latLong).title(stationName).snippet(chargeOutput))
            }
            viewModel.setMarkers(markers)
        }, Response.ErrorListener {

        }){}
        queue.add(evStationRequest)

        markerInit = true
    }

    @SuppressLint("MissingPermission")
    private val errorCatchCallback = OnMapReadyCallback { googleMap ->
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        processEVJson(googleMap)
        val bundle = arguments
        val name = bundle?.getString("PLACE_NAME")
        val long = bundle?.getDouble("LONGITUDE")
        val lat = bundle?.getDouble("LATITUDE")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mapFragment.requireActivity())
        checkPermissions()
        getCurrentLocation(googleMap)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to
     * install it inside the SupportMapFragment. This method will only be triggered once the
     * user has installed Google Play services and returned to the app.
     */
    private val callback = OnMapReadyCallback { googleMap ->
        val boundsBuilder = LatLngBounds.Builder()
        viewModel.getSrc()?.latLng?.let {
            MarkerOptions().position(it).title(viewModel.getSrc()?.name)
        }
            ?.let { googleMap.addMarker(it) }
        viewModel.getDst()?.latLng?.let {
            MarkerOptions().position(it).title(viewModel.getDst()?.name)
        }
            ?.let { googleMap.addMarker(it) }
        viewModel.getSrc()?.latLng?.let { CameraUpdateFactory.newLatLng(it) }
            ?.let { googleMap.moveCamera(it) }
        viewModel.getSrc()?.latLng?.let {
            CameraUpdateFactory.newLatLngZoom(
                it, 12f
            )
        }?.let { googleMap.animateCamera(it) }
        val path: MutableList<List<LatLng>> = ArrayList()
        val srcLat = viewModel.getSrc()?.latLng?.latitude
        val srcLng = viewModel.getSrc()?.latLng?.longitude
        val dstLat = viewModel.getDst()?.latLng?.latitude
        val dstLng = viewModel.getDst()?.latLng?.longitude
        val markers = viewModel.getMarkers()
        // println("all makrers" + markers.toString())
        if (srcLat != null && dstLat != null && srcLng != null && dstLng != null) {
            boundsBuilder.include(LatLng(srcLat, srcLng))
            boundsBuilder.include(LatLng(dstLat, dstLng))
        }

        var acceptableDistance = viewModel.calRemainRange()
//        Log.e("jane", "remain range $remainRange miles")
        val requestQueue = Volley.newRequestQueue(context)

        val elevationApi =
            "https://maps.googleapis.com/maps/api/elevation/json?path=${srcLat}%2C${srcLng}%7C${dstLat}%2C${dstLng}&samples=5&key=$MAPS_API_KEY"
        // Store the elevation in the array, 2 different pointers
        // Log.i("Test", acceptableDistance.toString())
        val elevationRequest = object : StringRequest(Request.Method.GET, elevationApi, Response.Listener<String> { elevResponse ->
            val elevJSONResponse = JSONObject(elevResponse)
            val results = elevJSONResponse.getJSONArray("results")
            val startElevation = results.getJSONObject(0).getDouble("elevation")
            val endElevation = results.getJSONObject(1).getDouble("elevation")
            // Negative diffElevation means incline, positive diffElevation means decline from source
            val diffElevation = startElevation - endElevation
            val multipleMeters = kotlin.math.abs(diffElevation) / 50
            if (diffElevation >= 50) {
                acceptableDistance = acceptableDistance?.plus((5 * multipleMeters))
            } else if (diffElevation <= -50) {
                acceptableDistance = acceptableDistance?.minus((5 * multipleMeters))
            }
        }, Response.ErrorListener {

        }){}
        requestQueue.add(elevationRequest)
        Log.i("Test4", acceptableDistance.toString())
        val newRoute: MutableSet<LatLng> = LinkedHashSet()
        val urlDirections =
            "https://maps.googleapis.com/maps/api/directions/json?origin=${srcLat},${srcLng}&destination=${dstLat},${dstLng}&key=$MAPS_API_KEY"
        //val urlDirections1 = "https://maps.googleapis.com/maps/api/directions/json?origin=${chargerLat},${chargerLng}&destination=${dstLat},${dstLng}&key=$MAPS_API_KEY"
        val directionsRequest1 = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> { response ->
            val jsonResponse = JSONObject(response)
                // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")

            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }

            var indexNewRoute = 0
            var prevElevation = 0.0
            var currentElevation = 0.0
            var lastStop = LatLng(0.0, 0.0)
            if (srcLat != null && srcLng != null) {
                newRoute.add(LatLng(srcLat, srcLng))
                lastStop = LatLng(srcLat, srcLng)
            }

            if (markers != null) {
//                println("path size ###################################")
//                println(path.size)
//                println(path[0].size)
//                var acceptableDistance = viewModel.calRemainRange()
//                Log.i("jane", "acceptable distance: $acceptableDistance")
//            Log.i("Test", "test")
//            Log.i("Test2", path.size.toString())
                for (i in 0 until path.size) {
                    for (j in 0 until path[i].size) {
//                        println("coordinate is ${path[i][j].latitude} ${path[i][j].longitude}")

                        // Possibly look around the path after every 5 or so

                        // Use the Elevation API to find the elevation of each point on the route
                        // If it is increasing, subtract a certain amount from acceptableDistance
                        // If it is decreasing, add a certain amount to acceptableDistance
                        // Simple Model: Uses 1.5 kwh every 305 meters and 1.5kwh, a car can go approximately 5 miles

//                        val pathLat = path[i][j].latitude
//                        val pathLng = path[i][j].longitude
//                        val elevationApi = "https://maps.googleapis.com/maps/api/elevation/json?locations=${pathLat}%2C${pathLng}&key=$MAPS_API_KEY"
////                        val elevationTest = "https://maps.googleapis.com/maps/api/elevation/json?locations=39.7391536%2C-104.9847034&key=$MAPS_API_KEY"
//
//                        // Store the elevation in the array, 2 different pointers
//                        val elevationRequest = object : StringRequest(Request.Method.GET, elevationApi, Response.Listener<String> {
//                                elevResponse ->
//                            val elevJSONResponse = JSONObject(elevResponse)
//                            val results = elevJSONResponse.getJSONArray("results")
//                            val elevation = results.getJSONObject(0).getDouble("elevation")
//                            // Log.i("Test", elevation.toString())
//
//                            // Look at the start and the end and find the elevation and use
//                            // a sample size and do the calculation
//                            if (prevElevation != 0.0) {
//                                // Elevation is all over the place, hard to determine when to make the adjustment on
//                                // acceptableDistance since elevation sometimes increases & sometimes decreases
//                                val diffElevation = elevation - prevElevation // How to determine if we climbed 300 meters
//
//                                // Issues: Cannot really take into account the variation between
//                                // incline and decline, could incline for a bit and then decline
//
//                                // A flaw with this code, basically this would only work if there is
//                                // consistent increase or decrease in elevation
//                                currentElevation += diffElevation
//
//                                // Log.i("Test", currentElevation.toString())
//                                // Issues: Seems like route gets plotted too fast and doesn't take
//                                // into account the changes of acceptableDistance based on elevation
//
//                                // This if check has flaws as well, for example, it will only work when
//                                // currentElevation starts from 0. If it goes up to 299 and then decline,
//                                // it will never meet the if check and vice versa
//                                // Based on tests and a lot of thought, feels like the logic to incorporate
//                                // elevation might not be possible with our current algorithm
//                                if (kotlin.math.abs(currentElevation) >= 300) {
////                                    val multiple305 = kotlin.math.abs(currentElevation) / 300
//                                    if (currentElevation < 0) {
//                                        acceptableDistance = acceptableDistance?.minus(5)
//                                    } else if (currentElevation > 0) {
//                                        acceptableDistance = acceptableDistance?.plus(5)
//                                    }
//                                    currentElevation = 0.0
//                                    Log.i("Test", acceptableDistance.toString())
//                                }
//                            }
//
//                            // Log.i("Test", elevation.toString())
//                            prevElevation = elevation
//                        }, Response.ErrorListener {
//
//                        }){}
//                        requestQueue.add(elevationRequest)

                        // Time stamp here in the routing to see if routing is happening faster
                        val metersDriven = FloatArray(1) // miles since last stop
                        Location.distanceBetween(
                            lastStop.latitude,
                            lastStop.longitude,
                            path[i][j].latitude,
                            path[i][j].longitude,
                            metersDriven
                        )
                        var milesDriven = MetersToMiles(metersDriven[0])
                        if (milesDriven <= acceptableDistance!!) {
                            continue
                        }
                        Log.i("Test", acceptableDistance.toString())
                        var closestCharger = viewModel.getClosestMarker(path[i][j])
                        lastStop = closestCharger.location
                        newRoute.add(closestCharger.location)
//                        println("added marker" + closestCharger.stationName)
                        googleMap.addMarker(
                            MarkerOptions().position(closestCharger.location)
                                .title(closestCharger.stationName)
                                .snippet("connector type: ${closestCharger.chargerType}")
                        )
                        acceptableDistance = viewModel.calFullRange()
                        val elevationApiCharger =
                            "https://maps.googleapis.com/maps/api/elevation/json?path=${closestCharger.location.latitude}%2C${closestCharger.location.longitude}%7C${dstLat}%2C${dstLng}&samples=5&key=$MAPS_API_KEY"
                        val elevationChargerRequest = object : StringRequest(
                            Request.Method.GET,
                            elevationApiCharger,
                            Response.Listener<String> { elevResponse ->
                                val elevJSONResponse = JSONObject(elevResponse)
                                val results = elevJSONResponse.getJSONArray("results")
                                val startElevation = results.getJSONObject(0).getDouble("elevation")
                                val endElevation = results.getJSONObject(1).getDouble("elevation")
                                // Negative diffElevation means incline, positive diffElevation means decline from source
                                val diffElevation = startElevation - endElevation
                                Log.i("Test2", diffElevation.toString())
                                val multipleMeters = kotlin.math.abs(diffElevation) / 50
                                if (diffElevation >= 50) {
                                    acceptableDistance = acceptableDistance?.plus((5 * multipleMeters))
                                } else if (diffElevation <= -50) {
                                    acceptableDistance = acceptableDistance?.minus((5 * multipleMeters))
                                }
                                Log.i("Test5", acceptableDistance.toString())
                            },
                            Response.ErrorListener {

                            }) {}
                        Log.i("Test3", acceptableDistance.toString())
                        requestQueue.add(elevationChargerRequest)
////                        Log.i("jane", "acceptable distance: $acceptableDistance")
                    }
                }
            }
            if (dstLat != null && dstLng != null) {
                newRoute.add(LatLng(dstLat, dstLng))
            }
            plotRoute(newRoute, googleMap)
        }, Response.ErrorListener {

        }) {}
        requestQueue.add(directionsRequest1)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 1000, 1000, 100))

        // Loop over marker
        // Loop over each list of LatLng and check in a range
        // If falls in a range, add LatLng to another list
        // Run another function that plots each source and destination for each to connect

    }

    override fun onMapReady(p0: GoogleMap) {

    }

    private fun plotRoute(newRoute: MutableSet<LatLng>, googleMap: GoogleMap) {
        val path: MutableList<List<LatLng>> = ArrayList()
        val requestQueue = Volley.newRequestQueue(context)
        for (i in 0 until (newRoute.size - 1)) {
            val srcLat = newRoute.elementAt(i).latitude
            val srcLng = newRoute.elementAt(i).longitude
            val dstLat = newRoute.elementAt(i + 1).latitude
            val dstLng = newRoute.elementAt(i + 1).longitude
            val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin=${srcLat},${srcLng}&destination=${dstLat},${dstLng}&key=$MAPS_API_KEY"
            val directionsRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                    response ->
                val jsonResponse = JSONObject(response)
                // Get routes
                val routes = jsonResponse.getJSONArray("routes")
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val steps = legs.getJSONObject(0).getJSONArray("steps")

                for (j in 0 until steps.length()) {
                    val points =
                        steps.getJSONObject(j).getJSONObject("polyline").getString("points")
                    path.add(PolyUtil.decode(points))
                }
                for (j in 0 until path.size) {
                    googleMap.addPolyline(PolylineOptions().addAll(path[j]).color(Color.BLUE))
                }
            }, Response.ErrorListener {

            }){}
            requestQueue.add(directionsRequest)
        }
    }

}