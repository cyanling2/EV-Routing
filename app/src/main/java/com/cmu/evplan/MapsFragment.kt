package com.cmu.evplan

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.cmu.evplan.databinding.FragmentMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import com.google.android.gms.maps.model.MarkerOptions

class MapsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var _binding: FragmentMapsBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: RoutingViewModel by activityViewModels()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        val view = binding.root

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mapFragment.requireActivity())
        checkPermissions()
        getCurrentLocation()
        clickSearchView(view)
        viewModel.clearTemp()

        _binding!!.mapSearchView.setOnClickListener{
            findNavController().navigate(R.id.action_mapsFragment_to_searchFragment)
        }
        return view
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        processEVJson(map)
        val bundle = arguments
        val name = bundle?.getString("PLACE_NAME")
        val long = bundle?.getDouble("LONGITUDE")
        val lat = bundle?.getDouble("LATITUDE")
        /*if (name != null) {
            Log.i("Test", name)
            Log.i("Test", long.toString())
            Log.i("Test", lat.toString())
        } */
        /*if (name != null && long != null && lat != null) {
            val chosenPlace = LatLng(lat, long)
            map.addMarker(MarkerOptions().position(chosenPlace).title(name))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(chosenPlace, 12.0f))
            Log.i("Test", name)
        } */
        //val latLong = intent.getSerializableExtra(LAT_LNG)
        // val name = intent.getStringExtra(PLACE_NAME)
        /*val sanjose = LatLng(37.3361663, -121.890591)
        googleMap.addMarker(MarkerOptions().position(sanjose).title("sanjose"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sanjose, 12.0f)) */
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener(mapFragment.requireActivity()) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

                val src: Place = Place.builder()
                    .setLatLng(currentLatLng)
                    .setName("My Current Location")
                    .build()
                viewModel.setSrc(src)

                // retrieve temperature info before passing to routing page
                val urlDirections = "https://api.openweathermap.org/data/2.5/weather?lat=${src.latLng?.latitude}&lon=${src.latLng?.latitude}&units=metric&APPID=237153eb4823ee8b72040b580065dd22"
                val srcWeatherRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                        response ->
                    val jsonResponse = JSONObject(response)
                    val temp = jsonResponse.getJSONObject("main").getDouble("temp")
                    viewModel.addTemp(temp)
                }, Response.ErrorListener {
                }){}
                val requestQueue = Volley.newRequestQueue(context)
                requestQueue.add(srcWeatherRequest)
            }
        }
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(mapFragment.requireContext(), permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Make sure fine location's permission is issued", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(mapFragment.requireActivity(),
                arrayOf(permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
        if (ActivityCompat.checkSelfPermission(mapFragment.requireContext(), permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mapFragment.requireActivity(),
                arrayOf(permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    // Click on search to go to search clicked page, but only works
    // if you click on the search icon
    private fun clickSearchView(view: View) {
        val searchView = view.findViewById<SearchView>(R.id.map_search_view)
        searchView.setOnClickListener {
            view.findNavController().navigate(R.id.searchFragment)
        }
    }




    // Pulls from an EV Station API and parses it to plot all EV stations in the US on the map
    private fun processEVJson(googleMap: GoogleMap) {
        // val markers: MutableList<LatLng> = ArrayList()
        // val markers = mutableMapOf<String, LatLng>()
        var markers: MutableList<MarkerType> = ArrayList()
        var connectorType = "J1772"
        if (viewModel.getConnectorType() != null) {
            connectorType = viewModel.getConnectorType()!!
        }
        // Log.i("Test", connectorType)

        val queue = Volley.newRequestQueue(context)
        val evURL = "https://services.arcgis.com/xOi1kZaI0eWDREZv/arcgis/rest/services/Alternative_Fueling_Stations/FeatureServer/0/query?where=state%20%3D%20%27CA%27%20AND%20fuel_type_code%20%3D%20%27ELEC%27&outFields=fuel_type_code,id,station_name,facility_type,city,state,street_address,zip,country,ev_connector_types,ev_network,ev_network_web,ev_other_evse,ev_pricing,ev_renewable_source,longitude,latitude,ev_level1_evse_num,ev_dc_fast_num,ev_level2_evse_num&outSR=4326&f=json"
        val evStationRequest = object : StringRequest(Request.Method.GET, evURL, Response.Listener<String> {
                response ->
            val evJsonResponse = JSONObject(response)
            // Get EV Stations
            val evStations = evJsonResponse.getJSONArray("features")
            // Log.e("Test", evStations.getJSONObject(0).getJSONObject("attributes").getString("LATITUDE"))
            for (i in 0 until evStations.length()) {
                val markerType: MarkerType = MarkerType()
                val latitude = evStations.getJSONObject(i).getJSONObject("attributes").getDouble("latitude")
                val longitude = evStations.getJSONObject(i).getJSONObject("attributes").getDouble("longitude")
                val stationName = evStations.getJSONObject(i).getJSONObject("attributes").getString("station_name")
                val connector = evStations.getJSONObject(i).getJSONObject("attributes").getString("ev_connector_types")
                val connectorArray = arrayOf(connector)
                for (element in connectorArray) {
                    if (element.contains(connectorType)) {
                        var chargeOutput = "connector type: $connector"
                        var latLong = LatLng(latitude, longitude)
                        markerType.chargerType = connector
                        markerType.stationName = stationName
                        markerType.location = latLong
                        markers.add(markerType)
                        // println("adding marker" + markerType.location.latitude)
                        googleMap.addMarker(MarkerOptions().position(latLong).title(stationName).snippet(chargeOutput))
                    }
                }

            }
            viewModel.setMarkers(markers)
        }, Response.ErrorListener {

        }){}
        queue.add(evStationRequest)
    }

    /*private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */

        val sanjose = LatLng(37.3361663, -121.890591)
        val chicago = LatLng(41.8755616,-87.6244212)
        googleMap.addMarker(MarkerOptions().position(sanjose).title("sanjose"))
        googleMap.addMarker(MarkerOptions().position(chicago).title("chicago"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sanjose))
        val path: MutableList<List<LatLng>> = ArrayList()
        val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin=37.3361663,-121.890591&destination=41.8755616,-87.6244212&key=AIzaSyBJlKiBqCmM6Xf8v9AGg7nQ_4TEiOXF8bY"
        val directionsRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                response ->
            val jsonResponse = JSONObject(response)
            // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")
            Log.i("myTag", "my message")

            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }
            for (i in 0 until path.size) {
                googleMap.addPolyline(PolylineOptions().addAll(path[i]).color(Color.BLUE))
            }
        }, Response.ErrorListener {

        }){}
        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(directionsRequest)
        // processEVJson(googleMap)
    } */

    /*override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    } */
}