package com.cmu.evplan

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
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


    }
}