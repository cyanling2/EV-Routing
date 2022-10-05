package com.cmu.evplan

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteFragment
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

// const val LAT_LNG = "LAT_LNG"
const val PLACE_NAME = "PLACE_NAME"

class SearchClicked : Fragment(R.layout.activity_search_clicked) {

    private lateinit var placesClient: PlacesClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_search_clicked, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // setContentView(R.layout.activity_search_clicked)
        Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        placesClient = Places.createClient(requireContext())
        //searchSubmit()
        clickBackButton(view)
        // autocompleteSearch()
    }

    /* private fun searchSubmit() {
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val intent = Intent(this@SearchClicked, MainActivity::class.java)
                startActivity(intent)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })
    } */

    private fun clickBackButton(view: View) {
        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            view.findNavController().navigate(R.id.mapsFragment2)
        }
    }

    /*private fun autocompleteSearch() {
        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment
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
                val fragmentManager = supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.add(R.id.frameLayout, fragment).commit()
                /*if (name != null) {
                    Log.i("Test1:", name)
                    Log.i("Test2:", latLong.toString())
                }
                intent.putExtra(LAT_LNG, latLong)
                intent.putExtra(PLACE_NAME, name) */
                val intent = Intent(this@SearchClicked, MainActivity::class.java)
                startActivity(intent)
            }

            override fun onError(error: Status) {
                Log.e("Error:", "$error")
            }
        })

    } */
}