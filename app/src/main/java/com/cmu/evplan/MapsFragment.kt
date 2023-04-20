package com.cmu.evplan

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cmu.evplan.databinding.CardChargingStationBinding
import com.cmu.evplan.databinding.FragmentMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject


class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private lateinit var map: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var _binding: FragmentMapsBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _dialog_binding: CardChargingStationBinding? = null
    private val dialogBinding get() = _dialog_binding!!
    private lateinit var dialogView: View
    private lateinit var dialog: BottomSheetDialog
    private val viewModel: RoutingViewModel by activityViewModels()

    private val cache: LruCache<Int, BitmapDescriptor> = LruCache(128)

//    /**
//     * Gets the BitMap to be able draw a charging map marker.
//     */
//    private fun getBitMap(): BitmapDescriptor? {
//        if (cache.size() != 0){
//            val cachedIcon: BitmapDescriptor = cache.get(0)
//            return cachedIcon
//        }
//
//        val icon = context?.let { bitmapDescriptorFromVector(it, R.drawable.map_marker_charging) }
//        cache.put(0, icon)
//        return icon
//    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    /**
     * Initializes and creates the view of the MapsFramgent, check the permission for location
     * services, initializes current location, and sets navigation for clicking on the search
     * bar.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        val view = binding.root
        _dialog_binding = CardChargingStationBinding.inflate(inflater, container, false)
        dialogView = dialogBinding.root

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        dialog = context?.let { BottomSheetDialog(it) }!!
        dialogBinding.idBtnDismiss.setOnClickListener {
            if (dialog != null) {
                dialog.dismiss()
            }
        }

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

    /**
     * When map is ready, it will process the EV API and plot the charging stations on the map.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
//        processEVJson(map)
    }

    /**
     * Used to get current location of a user based on the location services.
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
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

    /**
     * Checks permissions for location services and prompts user if has not been set yet.
     */
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

    /**
     * Click on the search bar to go to the search clicked page.
     */
    private fun clickSearchView(view: View) {
        val searchView = view.findViewById<AppCompatButton>(R.id.map_search_view)
        searchView.setOnClickListener {
            view.findNavController().navigate(R.id.searchFragment)
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            // val mBmpSize=bitmap.byteCount /1024;
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

//    /**
//     * Pulls from an EV Station API and parses it to plot all EV stations in California on the map..
//     */
//    private fun processEVJson(googleMap: GoogleMap) {
//        var markers: MutableList<MarkerType> = ArrayList()
//        val queue = Volley.newRequestQueue(context)
//        var to_add = 0
//        var fuelType = FuelTypeCode.ELEC
//        var state = "CA"
//        var access = AccessCode.public
//        var status = StatusCode.E
//        var connector = ConnecterTypeCode.J1772
////        val api_key = "pOkGMTMxyM7ypA6K8w7aR8CIcXJgzkE9Kw3qno6X"
//        val api_key = "Zb014s3euv2UZ50pFOtDelWARXvRodwJ5YaBuwAl"
//        val evURL = "https://developer.nrel.gov/api/alt-fuel-stations/v1.json?fuel_type=${fuelType.name}&state=$state&access=${access.name}&ev_connector_type=${connector.name}&status=${status.name}&api_key=$api_key"
//        val evStationRequest = object : StringRequest(Request.Method.GET, evURL, Response.Listener<String> {
//                response ->
//            val evJsonResponse = JSONObject(response)
//            // Get EV Stations
//            val evStations = evJsonResponse.getJSONArray("fuel_stations")
//            for (i in 0 until evStations.length()) {
//                val markerType = MarkerType()
//                var stationDetails = ""
//                val latitude = evStations.getJSONObject(i).getDouble("latitude")
//                val longitude = evStations.getJSONObject(i).getDouble("longitude")
//                var latLong = LatLng(latitude, longitude)
//                markerType.location = latLong
//                markerType.stationName = evStations.getJSONObject(i).getString("station_name")
//
//                val jsonArrayConnectors = evStations.getJSONObject(i).getJSONArray("ev_connector_types")
//                for (j in 0 until jsonArrayConnectors.length()) {
//                    markerType.connectors.add(ConnecterTypeCode.valueOf(jsonArrayConnectors[j] as String))
//                }
//                if (markerType.connectors.size > 0)
//                    stationDetails = "connector type: ${markerType.connectors}\n"
//
//                markerType.phone = evStations.getJSONObject(i).getString("station_phone")
//                if (!markerType.phone.equals("null") && !markerType.phone.equals("null")) stationDetails += "phone: ${markerType.phone}\n"
//
//                markerType.cardsAccepted = evStations.getJSONObject(i).getString("cards_accepted")
//                if (markerType.cardsAccepted != null && !markerType.cardsAccepted.equals("null")) stationDetails += "acceptable card type: ${markerType.cardsAccepted}\n"
//
//                markerType.accessDaysTime = evStations.getJSONObject(i).getString("access_days_time")
//                if (markerType.accessDaysTime != null && !markerType.accessDaysTime.equals("null")) stationDetails += "access ${markerType.accessDaysTime}\n"
//
//                markerType.streetAddress = evStations.getJSONObject(i).getString("street_address")
//                if (markerType.streetAddress != null && !markerType.streetAddress.equals("null")) stationDetails += "${markerType.streetAddress}\n"
//
//                markerType.network = evStations.getJSONObject(i).getString("ev_network")
//                if (markerType.network != null && !markerType.network.equals("null")) stationDetails += "belongs to ${markerType.network}\n"
//
//                markers.add(markerType)
//
//                markerType.stationDetails = stationDetails
//
//                if (to_add != 5){
//                    to_add += 1
//                    continue
//                }
//                googleMap.addMarker(MarkerOptions()
//                    .position(latLong)
//                    .title(markerType.stationName)
//                    .snippet("$stationDetails")
//                    .alpha(0.9f)
//                    .icon(getBitMap())
//                )
//                to_add = 0
//            }
//            viewModel.setMarkers(markers)
//        }, Response.ErrorListener {
//        }){}
//        queue.add(evStationRequest)
//    }

    override fun onInfoWindowClick(marker: Marker) {
        if (dialog != null) {
            dialog.setCancelable(false)
            dialogBinding.chargingStationDetail.setText(marker.snippet)
            dialog.setContentView(dialogView)
            dialog.show()
        }
    }
}