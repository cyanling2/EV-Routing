package com.cmu.evplan

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.widget.SearchView
import android.content.Intent
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Remove title bar
        getSupportActionBar()?.hide()

        //Initialize the bottom navigation view
        //create bottom navigation view object
        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigatin_view)
        val navController = findNavController(R.id.nav_fragment)
//        val navController = findNavController(R.id.nav_fragment)
        bottomNavigationView.setupWithNavController(navController)

        // Click on search to go to search clicked page, but only works
        // if you click on the search icon
        val searchView = findViewById<SearchView>(R.id.main_search_view)
        searchView.setOnClickListener {
            val intent = Intent(this, SearchClicked::class.java)
            startActivity(intent)
        }


        /*val name = intent.getStringExtra(PLACE_NAME)
        val latLong = intent.getSerializableExtra(LAT_LNG)
        if (name != null) {
            Log.i("Test:", name)
            Log.i("Test:", latLong.toString())
        }  */
    }
}