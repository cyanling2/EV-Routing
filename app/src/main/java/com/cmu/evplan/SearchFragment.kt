package com.cmu.evplan

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.activityViewModels
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

        // set listener for the search tab auto completion
        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.autocomplete) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                viewModel.setDst(place);

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

                findNavController().navigate(R.id.action_searchFragment_to_mapsFragment)
            }

            override fun onError(error: Status) {
                Log.e("Error:", "$error")
            }
        })

        // set listener to search finish-entering button
        _binding!!.searchView.setOnQueryTextListener(
            object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.e("jane", "query: $query")
                return true
            }
            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })

        return view
    }
}