package com.cmu.evplan

import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location

import android.graphics.Color
import android.media.Image
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import android.util.Log
import android.widget.ImageButton
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import org.json.JSONArray

class MapsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mapFragment.requireActivity())
        checkPermissions()
        getCurrentLocation()
        clickSearchView(view)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        //processEVJson(map)
        val bundle = arguments
        val name = bundle?.getString(PLACE_NAME)
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

    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener(mapFragment.requireActivity()) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(mapFragment.requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mapFragment.requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
                return
        }
    }

    private fun clickSearchView(view: View) {
        // Click on search to go to search clicked page, but only works
        // if you click on the search icon
        val searchView = view.findViewById<SearchView>(R.id.map_search_view)
        searchView.setOnClickListener {
            view.findNavController().navigate(R.id.searchClicked)
        }

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

    // Pulls from an EV Station API and parses it to plot all EV stations in the US on the map
    /* private fun processEVJson(googleMap: GoogleMap) {
        val queue = Volley.newRequestQueue(context)
        val evURL = "https://geo.dot.gov/mapping/rest/services/NTAD/Alternative_Fueling_Stations/MapServer/0/query?where=FUEL_TYPE_CODE%20%3D%20'ELEC'%20AND%20COUNTRY%20%3D%20'US'&outFields=FUEL_TYPE_CODE,STATION_NAME,STREET_ADDRESS,CITY,STATE,ZIP,EV_LEVEL1_EVSE_NUM,EV_LEVEL2_EVSE_NUM,EV_DC_FAST_COUNT,EV_OTHER_INFO,EV_NETWORK,EV_NETWORK_WEB,LATITUDE,LONGITUDE,ID,EV_CONNECTOR_TYPES,COUNTRY,EV_PRICING,LATDD,LONGDD,EV_ON_SITE_RENEWABLE_SOURCE&outSR=4326&f=json"
        val evStationRequest = object : StringRequest(Request.Method.GET, evURL, Response.Listener<String> {
                response ->
            val evJsonResponse = JSONObject(response)
            // Get EV Stations
            val evStations = evJsonResponse.getJSONArray("features")
            // Log.e("Test", evStations.getJSONObject(0).getJSONObject("attributes").getString("LATITUDE"))
            for (i in 0 until evStations.length()) {
                val latitude = evStations.getJSONObject(i).getJSONObject("attributes").getDouble("LATITUDE")
                val longitude = evStations.getJSONObject(i).getJSONObject("attributes").getDouble("LONGITUDE")
                val stationName = evStations.getJSONObject(i).getJSONObject("attributes").getString("STATION_NAME")
                val latLong = LatLng(latitude, longitude)
                googleMap.addMarker(MarkerOptions().position(latLong).title(stationName))
            }
        }, Response.ErrorListener {

        }){}
        queue.add(evStationRequest)
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