package com.cmu.evplan

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cmu.evplan.databinding.FragmentMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng


class MapsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var _binding: FragmentMapsBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

        _binding!!.mainSearchView.setOnClickListener{
            // FIXME: use nav graph
//            val intent = Intent(activity, SearchClicked::class.java)
//            startActivity(intent)
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

    @SuppressLint("MissingPermission")
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
}