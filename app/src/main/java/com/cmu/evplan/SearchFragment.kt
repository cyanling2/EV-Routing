package com.cmu.evplan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
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

    /**
     * Initializes and creates view for the search fragment, allows the user to input their current
     * batter level and battery percent they would like to preserve, and initializes the autocomplete
     * search. 
     */
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

        _binding!!.editBatteryPercentToReserve.addTextChangedListener (object : TextWatcher {
            override fun afterTextChanged(e: Editable?) {
                if (e == null) {
                    viewModel.setBottomLine(0.00)
                    return
                }
                var str = e.toString()
                if (str.isEmpty())  str = "0.00"
                viewModel.setBottomLine(str.toDouble())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        val historyList = viewModel.getHistoryList()
        if (historyList != null) {
            val arrayAdapter = context?.let { ArrayAdapter(it, R.layout.history_list, historyList) }
            val historyListView = view.findViewById<ListView>(R.id.history_list)
            historyListView.adapter = arrayAdapter
        }

        // set listener for the search tab auto completion
        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.autocomplete) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.NAME, Place.Field.LAT_LNG,Place.Field.ID,
            Place.Field.ADDRESS, Place.Field.PHONE_NUMBER,Place.Field.PHOTO_METADATAS, Place.Field.WEBSITE_URI ))
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                if (viewModel.getStatus() == SearchStatus.Destination)
                    viewModel.setDst(place)
                else
                    viewModel.setSrc(place)

                val latLong = place.latLng
                val name = place.name
                if (name != null) {
                    viewModel.setHistoryList(name)
                }
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