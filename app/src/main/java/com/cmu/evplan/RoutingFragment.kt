package com.cmu.evplan

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        _binding!!.destination.setText(viewModel.getDst()?.name)
        _binding!!.current.setText("current location")
        _binding!!.routingBackButton.setOnClickListener {
            findNavController().navigate(R.id.action_routingFragment_to_searchFragment)}

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
            val newRoute: MutableSet<LatLng> = LinkedHashSet()
            val distance = FloatArray(1)
            val near = FloatArray(1)
            val closeToRoute = FloatArray(1)
            var indexNewRoute = 0
            var chargingStationsPointer = 0
            var breakTrue = false
            var tooClose = false
            if (srcLat != null && srcLng != null) {
                newRoute.add(LatLng(srcLat, srcLng))
            }
            if (markers != null) {
                // Unsure what the condition should be to stop looking at charging stations in a while loop
                // while ()
                // Using a distance map or defining a grid within our app

                // Checking close to route first and then distance from point to point
                // Takes a very long time to process, not sure if my algorithm is correct
                // Add the while loop inside the for loop, so loop over path first and then check
                // the distance
                for (i in 0 until path.size) {
                    for (j in 0 until path[i].size) {
                        for (k in 0 until markers.size) {
                            Location.distanceBetween(markers[k].latitude,
                                markers[k].longitude, path[i][j].latitude,
                                path[i][j].longitude, closeToRoute)
                            // If the charging station is approximately less than 15 miles from the route
                            if (closeToRoute[0] < 25000 && !newRoute.contains(markers[k])) {
                                for (l in 0 until newRoute.size) {
                                    Location.distanceBetween(markers[k].latitude,
                                        markers[k].longitude, newRoute.elementAt(l).latitude,
                                        newRoute.elementAt(l).longitude, near)
                                    // If a charging station near the route is less than 18 miles to any
                                    // of the points (i.e. added charging stations & source), don't add to
                                    // set.
                                    if (near[0] < 30000) {
                                        tooClose = true
                                        break
                                    }
                                }
                                // If a charging station isn't close to any in the set, check if the
                                // charging station is between approximately 93 and 155 miles from
                                // the previously added charging station or source, then add
                                // the charging station to set.
                                if (!tooClose) {
                                    Location.distanceBetween(markers[k].latitude,
                                        markers[k].longitude, newRoute.elementAt(indexNewRoute).latitude,
                                        newRoute.elementAt(indexNewRoute).longitude, distance)
                                    if (distance[0] > 150000 && distance[0] < 250000) {
                                        newRoute.add(markers[k])
                                        indexNewRoute++
                                        googleMap.addMarker(MarkerOptions().position(markers[k]))
                                        break
                                    }
                                }
                            }
                            tooClose = false
                        }
                    }
                }

                // Pre-processing/Routing algorithm: Looks through each charging station from query
                // and checks if the distance from either the source or the previously added charging
                // station is within the range. If it is, check if the charging station falls in the range
                // near the plotted route. If it does, add charging station to the set, increment the
                // set pointer and reset to look at all charging stations again.

                // Issue: Due to the data not being ordered, it causes issues.
                /* while (chargingStationsPointer < markers.size) {
                    Location.distanceBetween(markers[chargingStationsPointer].latitude,
                    markers[chargingStationsPointer].longitude, newRoute.elementAt(indexNewRoute).latitude,
                    newRoute.elementAt(indexNewRoute).longitude, distance)
                    // Currently set to be approximately greater than 93 miles but less than 155 miles
                    if (distance[0] > 150000 && distance[0] < 250000) {
                        for (j in 0 until path.size) {
                            if (!breakTrue) {
                                for (k in 0 until path[j].size) {
                                    Location.distanceBetween(markers[chargingStationsPointer].latitude,
                                        markers[chargingStationsPointer].longitude, path[j][k].latitude,
                                        path[j][k].longitude, closeToRoute)
                                    // Currently set to be approximately less than 10 miles, but greater than 3 miles
                                    if (closeToRoute[0] < 16000 &&
                                        !newRoute.contains(markers[chargingStationsPointer])) {
                                        newRoute.add(markers[chargingStationsPointer])
                                        indexNewRoute++
                                        Log.i("Test", chargingStationsPointer.toString())
                                        googleMap.addMarker(MarkerOptions().position(markers[chargingStationsPointer]))
                                        chargingStationsPointer = -1

                                        breakTrue = true
                                        break
                                    }
                                }
                            } else {
                                break
                            }
                        }
                    }
                    breakTrue = false
                    chargingStationsPointer++
                } */

                /* for (i in 0 until markers.size) {
                    Location.distanceBetween(markers[i].latitude, markers[i].longitude,
                    newRoute.elementAt(indexNewRoute).latitude, newRoute.elementAt(indexNewRoute).longitude,
                    distance)
                    // Run into issues because data is not in order of distance, so it can skip over a charging station
                    // I.e.: Since we don't constantly look at the markers and calculate from beginning to end
                    // charging stations won't be in order
                    // Tried using while loop, but it takes too long and the app can't handle it
                    if (distance[0] > 402336 && distance[0] < 442570) {
                        for (j in 0 until path.size) {
                            if (!breakTrue) {
                                for (k in 0 until path[j].size) {
                                    Location.distanceBetween(markers[i].latitude, markers[i].longitude,
                                        path[j][k].latitude, path[j][k].longitude, closeToRoute)
                                    if (closeToRoute[0] < 4828 && !newRoute.contains(markers[i])) {
                                        newRoute.add(markers[i])
                                        indexNewRoute++
                                        googleMap.addMarker(MarkerOptions().position(markers[i]))
                                        breakTrue = true
                                        break
                                    }
                                }
                            } else {
                                break
                            }
                        }
                    }
                    breakTrue = false
                    for (j in 0 until path.size) {
                        for (k in 0 until path[j].size) {
                            Location.distanceBetween(markers[i].latitude, markers[i].longitude,
                            path[j][k].latitude, path[j][k].longitude, closeToRoute)
                            // Close to Route: 5 miles?
                            // Test: 275 miles max distance?
                            // Check each charging station for the max distance from the previous charging station
                            if (closeToRoute[0] < 100) {
                                // Check the distance of the current charging station with the previous
                                // station and if it is close by a certain distance, do not add to the
                                // new route list
                                Location.distanceBetween(newRoute.elementAt(indexNewRoute).latitude,
                                newRoute.elementAt(indexNewRoute).longitude, markers[i].latitude,
                                markers[i].longitude, distance)
                                if (distance[0] > 402336 && !newRoute.contains(markers[i])) {
                                    newRoute.add(markers[i])
                                    indexNewRoute++
                                    googleMap.addMarker((MarkerOptions().position(markers[i])))
                                }
                                if (newRoute.size >= 3) {
                                    Location.distanceBetween(newRoute.elementAt(indexNewRoute).latitude,
                                    newRoute.elementAt(indexStations).longitude, markers[i].latitude,
                                    markers[i].longitude, near)
                                }
                                if (newRoute.size != 3 || near[0] > 321869) {
                                    newRoute.add(markers[i])
                                    if (newRoute.size > 2) {
                                        indexStations++
                                    }
                                    googleMap.addMarker(MarkerOptions().position(markers[i]))
                                    // Log.i("Test", newRoute.toString())
                                }
                            }
                        }
                    }
                } */
            }
            if (dstLat != null && dstLng != null) {
                newRoute.add(LatLng(dstLat, dstLng))
            }
            /*for (i in 0 until path.size) {
                googleMap.addPolyline(PolylineOptions().addAll(path[i]).color(Color.BLUE))
                // Log.i("Test:", path[i].toString())
            } */
            plotRoute(newRoute, googleMap)
            Log.i("Test", newRoute.toString())
        }, Response.ErrorListener {

        }){}
        // end Hard code
        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(directionsRequest1)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 1000, 1000, 100))
    }



    override fun onMapReady(p0: GoogleMap) {

    }

    private fun plotRoute(newRoute: MutableSet<LatLng>, googleMap: GoogleMap) {
        val path: MutableList<List<LatLng>> = ArrayList()
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
            val requestQueue = Volley.newRequestQueue(context)
            requestQueue.add(directionsRequest)
        }

    }

}