package com.cmu.evplan

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.widget.SearchView
import android.text.Layout
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: RoutingViewModel

    /**
     * Creates the MainAcitivty and intializes the navigation for the bottom navigation bar.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Remove title bar
        getSupportActionBar()?.hide()

        //Initialize the bottom navigation view
        //create bottom navigation view object
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigatin_view)
        bottomNavigationView.setupWithNavController(navController)
    }

    fun startNavigation(view: View) {

        viewModel = ViewModelProvider(this).get(RoutingViewModel::class.java)

        val dstLat = viewModel.getDst()?.latLng?.latitude
        val dstLng = viewModel.getDst()?.latLng?.longitude
        val gmmIntentUri =
            Uri.parse("https://www.google.com/maps/dir/?api=1&dir_action=navigate&destination=${dstLat}%2C${dstLng}&waypoints=37.3243862%2C-122.030635") //santacruz: 36.9741171,-122.0307963
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
//        mapIntent.setPackage("com.google.android.apps.maps")

        // Attempt to start an activity that can handle the Intent
        startActivity(mapIntent)
    }
}