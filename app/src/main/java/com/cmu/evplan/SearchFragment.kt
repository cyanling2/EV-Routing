package com.cmu.evplan

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.cmu.evplan.databinding.FragmentSearchBinding
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener


const val LAT_LNG = "LAT_LNG"
const val PLACE_NAME = "PLACE_NAME"

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var placesClient: PlacesClient

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
//        autocompleteSearch()

        return view
    }

    private fun autocompleteSearch() {
        val autocompleteFragment = parentFragmentManager.findFragmentById(R.id.autocomplete) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val latLong = place.latLng
                val name = place.name
                val bundle = Bundle()
                bundle.putString(PLACE_NAME, name)
                bundle.putDouble("LATITUDE", latLong!!.latitude)
                bundle.putDouble("LONGITUDE", latLong.longitude)
                val fragment = MapsFragment()
                fragment.arguments = bundle
                val fragmentManager = parentFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.add(R.id.frameLayout, fragment).commit()

                // FIXME
//                val intent = Intent(this@SearchClicked, MainActivity::class.java)
//                startActivity(intent)
            }

            override fun onError(error: Status) {
                Log.e("Error:", "$error")
            }
        })

    }
}