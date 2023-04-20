package com.cmu.evplan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cmu.evplan.BuildConfig.MAPS_API_KEY
import com.cmu.evplan.databinding.CardChargingStationBinding
import com.cmu.evplan.databinding.FragmentRoutingBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.*
import java.net.HttpURLConnection
import java.net.URL


class RoutingFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {
    private var _binding: FragmentRoutingBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _dialog_binding: CardChargingStationBinding? = null
    private val dialogBinding get() = _dialog_binding!!
    private lateinit var dialogView: View
    private lateinit var dialog: BottomSheetDialog

    private val viewModel: RoutingViewModel by activityViewModels()

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    private lateinit var placesClient: PlacesClient

    private var markerInit = false

    /**
     * Initializes and creates view for the routing fragment and creates the bottom information
     * box with additional information about destination.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRoutingBinding.inflate(inflater, container, false)
        val view = binding.root

        _dialog_binding = CardChargingStationBinding.inflate(inflater, container, false)
        dialogView = dialogBinding.root

        val mapFragment = childFragmentManager.findFragmentById(R.id.routeMap) as SupportMapFragment?
        if (viewModel.getSrc()?.name == null) {
            mapFragment?.getMapAsync { errorCatchCallback }
        } else if (viewModel.getDst()?.name == null) {
            viewModel.setDst(viewModel.getSrc()!!)
            mapFragment?.getMapAsync {errorCatchCallback}
        } else {
//            mapFragment?.getMapAsync(this)
            mapFragment?.getMapAsync(callback)
        }
        placesClient = activity?.let { Places.createClient(it) }!!

        //set destination details information
        _binding!!.destinationInfoBlock.setText(viewModel.getDst()?.name)
        _binding!!.destinationAddress.setText("Address:  "+viewModel.getDst()?.address)
        if (viewModel.getDst()?.phoneNumber != null) {
            _binding!!.destinationPhonenumber.setText("Phone Number:  "+viewModel.getDst()?.phoneNumber)
        } else {
            binding!!.destinationPhonenumber.setText("Phone Number: Unavailable")
        }

        if (viewModel.getDst()?.websiteUri != null) {
            _binding!!.destinationWebsite.setText("Website:  "+viewModel.getDst()?.websiteUri.toString())
        } else {
            binding!!.destinationWebsite.setText("Website: Unavailable")
        }
        setDestinationPhoto(viewModel.getDst()?.id)

        dialog = context?.let { BottomSheetDialog(it) }!!
        dialogBinding.idBtnDismiss.setOnClickListener {
            if (dialog != null) {
                dialog.dismiss()
            }
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

    /**
     * Sets the destination photo pulled from the Google Places API.
     */
    fun setDestinationPhoto(placeId:String?){
        // Specify fields. Requests for photos must always have the PHOTO_METADATAS field.
        val fields = listOf(Place.Field.PHOTO_METADATAS)
        // Get a Place object (this example uses fetchPlace(), but you can also use findCurrentPlace())
        val placeRequest = placeId?.let { FetchPlaceRequest.newInstance(it, fields) }

        if (placeRequest != null) {
            placesClient.fetchPlace(placeRequest)
                .addOnSuccessListener { response: FetchPlaceResponse ->
                    val place = response.place

                    // Get the photo metadata.
                    val metada = place.photoMetadatas
                    if (metada == null || metada.isEmpty()) {
                        return@addOnSuccessListener
                    }
                    val photoMetadata = metada.first()

                    // Get the attribution text.
                    val attributions = photoMetadata?.attributions

                    // Create a FetchPhotoRequest.
                    val photoRequest = FetchPhotoRequest.builder(photoMetadata)
                        .setMaxWidth(500) // Optional.
                        .setMaxHeight(300) // Optional.
                        .build()
                    placesClient.fetchPhoto(photoRequest)
                        .addOnSuccessListener { fetchPhotoResponse: FetchPhotoResponse ->
                            val bitmap = fetchPhotoResponse.bitmap
                            _binding!!.destinationPhoto.setImageBitmap(bitmap)
                        }.addOnFailureListener { exception: Exception ->
                            if (exception is ApiException) {
                                val statusCode = exception.statusCode
                            }
                        }
                }
        }

    }

    /**
     * Calculate meters to miles.
     */
    fun MetersToMiles(meters: Float) : Float {
        return (meters * 0.000621371192).toFloat()
    }

    /**
     * Check permissions of location services and prompts if it has not been set.
     */
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

    /**
     * Sets the current location.
     */
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

    @SuppressLint("MissingPermission")
    private val errorCatchCallback = OnMapReadyCallback { googleMap ->
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mapFragment.requireActivity())
        checkPermissions()
        getCurrentLocation(googleMap)
    }

    private fun sendHttpGetRequest(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000 // timeout in milliseconds

        connection.connect()
        val statusCode = connection.responseCode
        if (statusCode != HttpURLConnection.HTTP_OK) {
            throw Exception("HTTP error: $statusCode")
        }

        val inputStream = connection.inputStream.bufferedReader()
        val response = inputStream.use { it.readText() }

        connection.disconnect()
        return response
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to
     * install it inside the SupportMapFragment. This method will only be triggered once the
     * user has installed Google Play services and returned to the app.
     *
     * Calculates the route and plots charging stations along the route based on user's input
     * in battery level and battery % reserved,
     */
    private val callback = OnMapReadyCallback { googleMap ->
        val boundsBuilder = LatLngBounds.Builder()
        viewModel.getSrc()?.latLng?.let { it ->
            MarkerOptions().position(it).title(viewModel.getSrc()?.name)
                .icon(this.context?.let { context?.let { it1 -> bitmapDescriptorFromVector(it1,R.drawable.start_icon) } })
        }
            ?.let { googleMap.addMarker(it) }
        viewModel.getDst()?.latLng?.let {
            MarkerOptions().position(it).title(viewModel.getDst()?.name)
                .icon(this.context?.let { context?.let { it1 -> bitmapDescriptorFromVector(it1,R.drawable.destination_icon) } })
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
        val queue = Volley.newRequestQueue(context)

        if (srcLat != null && dstLat != null && srcLng != null && dstLng != null) {
            boundsBuilder.include(LatLng(srcLat, srcLng))
            boundsBuilder.include(LatLng(dstLat, dstLng))
        }

        var acceptableDistance = viewModel.calRemainRange()
        val requestQueue = Volley.newRequestQueue(context)
        val newRoute: MutableSet<LatLng> = LinkedHashSet()
        val urlDirections =
            "https://maps.googleapis.com/maps/api/directions/json?origin=${srcLat},${srcLng}&destination=${dstLat},${dstLng}&key=$MAPS_API_KEY"
        googleMap.setOnInfoWindowClickListener(this)
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
            var lastStop = LatLng(0.0, 0.0)
            if (srcLat != null && srcLng != null) {
                newRoute.add(LatLng(srcLat, srcLng))
                lastStop = LatLng(srcLat, srcLng)
            }

            if (markers != null || markers == null) {
                for (i in 0 until path.size) {
                    for (j in 0 until path[i].size) {
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

//                        kd tree approach. not used any more
//                        var closestCharger = viewModel.getClosestMarker(path[i][j])

                        val api_key = "Zb014s3euv2UZ50pFOtDelWARXvRodwJ5YaBuwAl"
                        val url = "https://developer.nrel.gov/api/alt-fuel-stations/v1/nearest.json?fuel_type=${FuelTypeCode.ELEC.name}&access=${AccessCode.public.name}&status=${StatusCode.E.name}&api_key=$api_key&latitude=${path[i][j].latitude}&longitude=${path[i][j].longitude}&limit=1&radius=20&ev_charging_level=dc_fast&ev_connector_type=${viewModel.getConnectorType()}"
                        val evStationRequest = object : StringRequest(Request.Method.GET, url, Response.Listener<String> {
                                response ->

                            val evJsonResponse = JSONObject(response)
                            Log.d("EVJSONRESPONSE", evJsonResponse.toString())
                            // Get EV Stations
                            val evStations = evJsonResponse.getJSONArray("fuel_stations")
                            for (i in 0 until evStations.length()) {
                                var stationDetails = ""
                                val latitude = evStations.getJSONObject(i).getDouble("latitude")
                                val longitude = evStations.getJSONObject(i).getDouble("longitude")
                                var latLong = LatLng(latitude, longitude)
                                var closestCharger = MarkerType()
                                closestCharger.location = latLong
                                closestCharger.stationName = evStations.getJSONObject(i).getString("station_name")

                                val jsonArrayConnectors = evStations.getJSONObject(i).getJSONArray("ev_connector_types")
                                for (j in 0 until jsonArrayConnectors.length()) {
                                    closestCharger.connectors.add(ConnecterTypeCode.valueOf(jsonArrayConnectors[j] as String))
                                }
                                if (closestCharger.connectors.size > 0)
                                    stationDetails = "connector type: ${closestCharger.connectors}\n"

                                closestCharger.phone = evStations.getJSONObject(i).getString("station_phone")
                                if (!closestCharger.phone.equals("null") && !closestCharger.phone.equals("null")) stationDetails += "phone: ${closestCharger.phone}\n"

                                closestCharger.cardsAccepted = evStations.getJSONObject(i).getString("cards_accepted")
                                if (closestCharger.cardsAccepted != null && !closestCharger.cardsAccepted.equals("null")) stationDetails += "acceptable card type: ${closestCharger.cardsAccepted}\n"

                                closestCharger.accessDaysTime = evStations.getJSONObject(i).getString("access_days_time")
                                if (closestCharger.accessDaysTime != null && !closestCharger.accessDaysTime.equals("null")) stationDetails += "access ${closestCharger.accessDaysTime}\n"

                                closestCharger.streetAddress = evStations.getJSONObject(i).getString("street_address")
                                if (closestCharger.streetAddress != null && !closestCharger.streetAddress.equals("null")) stationDetails += "${closestCharger.streetAddress}\n"

                                closestCharger.network = evStations.getJSONObject(i).getString("ev_network")
                                if (closestCharger.network != null && !closestCharger.network.equals("null")) stationDetails += "belongs to ${closestCharger.network}\n"


                                closestCharger.stationDetails = stationDetails

                                newRoute.add(closestCharger.location)
                                googleMap.addMarker(
                                    MarkerOptions().position(closestCharger.location)
                                        .title(closestCharger.stationName)
                                        .snippet(closestCharger.stationDetails)
                                        .alpha(0.9f)
                                        .icon(this.context?.let { bitmapDescriptorFromVector(it,R.drawable.map_marker_charging) })
                                )


                            }
                        }, Response.ErrorListener {

                        }){}
                        queue.add(evStationRequest)
                        lastStop = path[i][j]
                        acceptableDistance = viewModel.calFullRange()

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
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 900, 900, 100))
    }

    override fun onMapReady(googleMap: GoogleMap) {

    }

    /**
     * Plots the route from the source to the chosen destination.
     */
    private fun plotRoute(newRoute: MutableSet<LatLng>, googleMap: GoogleMap) {
        var route_color: Long = 0xff0096ff
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
                    googleMap.addPolyline(PolylineOptions()
                        .addAll(path[j])
                        .color(route_color.toInt())
                        .startCap(RoundCap())
                        .endCap(RoundCap())
                        .jointType(JointType.ROUND)
                        .width(17.toFloat())
                    )
                }
            }, Response.ErrorListener {

            }){}
            requestQueue.add(directionsRequest)
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    override fun onInfoWindowClick(marker: Marker) {
        if (dialog != null) {
            dialog.setCancelable(false)
            dialogBinding.chargingStationDetail.setText(marker.snippet)
            dialog.setContentView(dialogView)
            dialog.show()
        }
    }
}