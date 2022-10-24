package com.cmu.evplan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cmu.evplan.databinding.FragmentSearchBinding
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

const val PLACE_NAME = "PLACE_NAME"

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var placesClient: PlacesClient

    private val viewModel: RoutingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val view = binding.root

        activity?.let { Places.initialize(it, BuildConfig.MAPS_API_KEY) }
        placesClient = activity?.let { Places.createClient(it) }!!

        _binding!!.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_mapsFragment)
        }

        _binding!!.editBatteryPercentReserved.addTextChangedListener (object : TextWatcher {
            override fun afterTextChanged(e: Editable?) {
                if (e == null) {
                    viewModel.setBattery(100.00)
                    return
                }
                var str = e.toString()
                if (str.isEmpty())  str = "100.00"
                viewModel.setBattery(str.toDouble())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        // set listener for the search tab auto completion
        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.autocomplete) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                viewModel.setDst(place);

                // retrieve temperature info before passing to routing page
                val urlDirections = "https://api.openweathermap.org/data/2.5/weather?lat=${place.latLng?.latitude}&lon=${place.latLng?.latitude}&units=metric&APPID=237153eb4823ee8b72040b580065dd22"
                val dstWeatherRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                        response ->
                    val jsonResponse = JSONObject(response)
                    val temp = jsonResponse.getJSONObject("main").getDouble("temp")
                    viewModel.addTemp(temp)
                }, Response.ErrorListener {
                }){}
                val requestQueue = Volley.newRequestQueue(context)
                requestQueue.add(dstWeatherRequest)


                val latLong = place.latLng
                val name = place.name
                val bundle = Bundle()
                bundle.putString(PLACE_NAME, name)
                bundle.putDouble("LATITUDE", latLong!!.latitude)
                bundle.putDouble("LONGITUDE", latLong.longitude)
                val fragment = MapsFragment()
                fragment.arguments = bundle
                val fragmentManager = childFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.add(R.id.searchView, fragment).commit()

                findNavController().navigate(R.id.action_searchFragment_to_routingFragment)
            }

            override fun onError(error: Status) {
                Log.e("Error:", "$error")
            }
        })

        return view
    }
}