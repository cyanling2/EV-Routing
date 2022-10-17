package com.cmu.evplan

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import kotlin.math.abs

/**
 * Saves the state of source and destination information
 * @param src Source gathered by system location
 * @param dst Destination passed from search ƒragment
 * @param markers   Marker spots for charging stations
 * @param battery   Percentage for battery levels
 * */
class RoutingViewModel: ViewModel() {
    private var src: MutableLiveData<Place> = MutableLiveData()
    private var dst: MutableLiveData<Place> = MutableLiveData()
    private var markers: MutableLiveData<MutableList<LatLng>> = MutableLiveData()
    private var battery: MutableLiveData<Double> = MutableLiveData()
    private var temps: MutableLiveData<MutableList<Double>> = MutableLiveData()

    fun setSrc(place : Place) {
        src.value = place
    }

    fun setDst(place: Place) {
        dst.value = place
    }

    fun setMarkers(listMarkers: MutableList<LatLng>) {
        markers.value = listMarkers
    }

    fun setBattery(per: Double) {
        battery.value = per
    }

    fun addTemp(temp: Double) {
        temps.value?.add(temp)
    }

    fun clearTemp() {
        temps.value?.clear()
    }

    fun getSrc(): Place? {
        return src.value
    }

    fun getDst(): Place? {
        return dst.value
    }

    fun getMarkers(): MutableList<LatLng>? {
        return markers.value
    }

    /**
     * According to the remaining battery level and car mileage assumption,
     * calculate the remaining distance to go without charging.
     *
     * y - pHeat * time = b * distance
     * y = b * distance + pHeat * distance / speed
     *   = (b + pHeat / speed) * distance
     *
     * y = battery capacity
     *      assume using 2022 Tesla Model 3, average full battery range = 165 mi, the usable capacity is 40 kWh (estimate)
     *      @reference https://ev-database.uk/car/1060/Tesla-Model-3-Standard-Range#:~:text=Battery%20and%20Charging,on%20a%20fully%20charged%20battery.
     *
     * b = pure driving consumption efficiency
     * pHeat = delta temperature consumption
     *      P heat = 90W * ΔC
     *      assume inner-car climate: 20 celsius
     *      @reference https://link.springer.com/article/10.1007/s12053-020-09900-5
     *
     * y = p * e
     * p = percentage of the battery remained
     * e = efficiency for 1% of battery
     *      40kWh = 100 * e, e = 40/100 = 0.4
     *
     * p * e = (b + pHeat / speed) * distance
     * distance = (b + pHeat / speed) / (p * e)
     * */
    fun calRemainRange(): Double? {
        val b = 40.toDouble()/1.65
        var pHeat = 0.0
        if (!temps.value.isNullOrEmpty())
            pHeat = 0.09 * abs(temps.value!![0] - 20.0) * 0.5 + 0.09 * abs(temps.value!!.last() - 20.0) * 0.5
        return (b + pHeat/60)/(battery.value?.times(0.4)!!)
    }
}