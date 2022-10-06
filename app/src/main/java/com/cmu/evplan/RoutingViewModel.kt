package com.cmu.evplan

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.libraries.places.api.model.Place

/**
 * Saves the state of source and destination information
 * Source gathered by system location
 * Destination passed from search ƒragment
 * */
class RoutingViewModel: ViewModel() {
    private var src: MutableLiveData<Place> = MutableLiveData()
    private var dst: MutableLiveData<Place> = MutableLiveData()

    fun setSrc(place : Place) {
        src.value = place
    }

    fun setDst(place: Place) {
        dst.value = place
    }

    fun getSrc(): Place? {
        return src.value
    }

    fun getDst(): Place? {
        return dst.value
    }
}