package com.cmu.evplan

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
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
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds

class RoutingFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentRoutingBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: RoutingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRoutingBinding.inflate(inflater, container, false)
        val view = binding.root

        val mapFragment = childFragmentManager.findFragmentById(R.id.routeMap) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        return view
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
//        val sanjose = LatLng(37.3361663, -121.890591)
//        val chicago = LatLng(41.8755616,-87.6244212)
        val boundsBuilder = LatLngBounds.Builder()
        viewModel.getSrc()?.latLng?.let { MarkerOptions().position(it).title(viewModel.getSrc()?.name) }
            ?.let { googleMap.addMarker(it) }
        viewModel.getDst()?.latLng?.let { MarkerOptions().position(it).title(viewModel.getDst()?.name) }
            ?.let { googleMap.addMarker(it) }
        viewModel.getSrc()?.latLng?.let { CameraUpdateFactory.newLatLng(it)}
            ?.let { googleMap.moveCamera(it)}
        viewModel.getSrc()?.latLng?.let {
            CameraUpdateFactory.newLatLngZoom(
                it, 12f)
        }?.let { googleMap.animateCamera(it) }
        val path: MutableList<List<LatLng>> = ArrayList()
        val srcLat = viewModel.getSrc()?.latLng?.latitude
        val srcLng = viewModel.getSrc()?.latLng?.longitude
        val dstLat = viewModel.getDst()?.latLng?.latitude
        val dstLng = viewModel.getDst()?.latLng?.longitude
        val markers = viewModel.getMarkers()
        // Log.i("Test:", markers.toString())
        if (srcLat != null && dstLat != null && srcLng != null && dstLng != null) {
            boundsBuilder.include(LatLng(srcLat, srcLng))
            boundsBuilder.include(LatLng(dstLat, dstLng))
        }

        var remainRange = viewModel.calRemainRange()
//        Log.e("jane", "remain range $remainRange miles")

        val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin=${srcLat},${srcLng}&destination=${dstLat},${dstLng}&key=$MAPS_API_KEY"
        //val directionsRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
        //Hard Code Charger
        /*val chargerLat=37.3243862
        val chargerLng=-122.030635
        val charger = LatLng(37.3243862, -122.030635)
        googleMap.addMarker(MarkerOptions().position(charger).title("charger")) */
        //val urlDirections1 = "https://maps.googleapis.com/maps/api/directions/json?origin=${chargerLat},${chargerLng}&destination=${dstLat},${dstLng}&key=$MAPS_API_KEY"
        val directionsRequest1 = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                response ->
            val jsonResponse = JSONObject(response)
            // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")

            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }
            // Loop over marker
            // Loop over each list of LatLng and check in a range
            // If falls in a range, add LatLng to another list
            // Run another function that plots each source and destination for each to connect

            // Issues: Can't do calculation because the charging stations data is so big
            // Unsure how to just choose a single charging station rather than having a bunch near
            // the route
            val newRoute: MutableSet<LatLng> = HashSet()
            val distance = FloatArray(1)
            val near = FloatArray(1)
            var indexStations = 0
            if (srcLat != null && srcLng != null) {
                newRoute.add(LatLng(srcLat, srcLng))
            }
            if (markers != null) {
                for (i in 0 until markers.size) {
                    for (j in 0 until path.size) {
                        for (k in 0 until path[j].size) {
                            Location.distanceBetween(markers[i].latitude, markers[i].longitude,
                            path[j][k].latitude, path[j][k].longitude, distance)
                            // Log.i("Test", distance[0].toString()
                            // Close to Route: 5 miles?
                            // Test: 275 miles max distance?
                            if (distance[0] < 100) {
                                // Check the distance of the current charging station with the previous
                                // station and if it is close by a certain distance, do not add to the
                                // new route list
                                if (newRoute.size == 3) {
                                    Location.distanceBetween(newRoute.elementAt(indexStations).latitude,
                                    newRoute.elementAt(indexStations).longitude, markers[i].latitude,
                                    markers[i].longitude, near)
                                }
                                if (newRoute.size != 3 || near[0] > 3000) {
                                    newRoute.add(markers[i])
                                    if (newRoute.size > 2) {
                                        indexStations++
                                    }
                                    googleMap.addMarker(MarkerOptions().position(markers[i]))
                                    Log.i("Test", newRoute.toString())
                                }
                            }
                        }
                    }
                }
            }
            if (dstLat != null && dstLng != null) {
                newRoute.add(LatLng(dstLat, dstLng))
            }
            // Log.i("Test", newRoute.toString())
            for (i in 0 until path.size) {
                googleMap.addPolyline(PolylineOptions().addAll(path[i]).color(Color.BLUE))
                // Log.i("Test:", path[i].toString())
            }
        }, Response.ErrorListener {

        }){}
        // end Hard code
        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(directionsRequest1)

        /*val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin=${srcLat},${srcLng}&destination=${chargerLat},${chargerLng}&key=$MAPS_API_KEY"
        val directionsRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                response ->
            val jsonResponse = JSONObject(response)
            // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")

            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }
            for (i in 0 until path.size) {
                googleMap.addPolyline(PolylineOptions().addAll(path[i]).color(Color.BLUE))
            }
        }, Response.ErrorListener {

        }){}

        requestQueue.add(directionsRequest)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 1000, 1000, 100)) */
    }

    override fun onMapReady(p0: GoogleMap) {

    }

}