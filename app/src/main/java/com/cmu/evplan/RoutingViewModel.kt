package com.cmu.evplan

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import kotlin.math.abs

/** Used to identify which place is the search page aiming for. Default as Destination. */
enum class SearchStatus {
    Destination, Source
}

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
    private var markers: MutableLiveData<MutableList<MarkerType>> = MutableLiveData()
    private var battery: MutableLiveData<Double> = MutableLiveData()
    private var temps: MutableLiveData<MutableList<Double>> = MutableLiveData()
    private var status: MutableLiveData<SearchStatus> = MutableLiveData()
    private var connectorType: MutableLiveData<String> = MutableLiveData()
    private var markersKDTree = KDTree()

    fun setSrc(place : Place) {
        src.value = place
    }

    fun setDst(place: Place) {
        dst.value = place
    }

    fun getStatus(): SearchStatus {
        if (status.value == null) status.value = SearchStatus.Destination
        return status.value!!
    }

    fun setStatus(s: SearchStatus) {
        status.value = s
    }

    fun setMarkers(listMarkers: MutableList<MarkerType>) {
        markers.value = listMarkers
        for (i in 0 until listMarkers.size) {
            markersKDTree.insert(listMarkers[i])
        }
    }

    fun setConnectorType(connector: String) {
        connectorType.value = connector
    }

    fun getConnectorType(): String? {
        return connectorType.value
    }

    fun setBattery(per: Double) {
        battery.value = per
    }

    fun getBattery(): Double? {
        return battery.value
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

    fun getMarkers(): MutableList<MarkerType>? {
        return markers.value
    }

    fun getClosestMarker(latlng: LatLng) : MarkerType {
        val station = markersKDTree.search(
            latlng,
            25000F
        )
        return station!!.marker
    }

    /**
     * According to the remaining battery level and car mileage assumption,
     * calculate the remaining distance to go without charging.
     *
     * y - pHeat * time = b * distance
     * y = b * distance + pHeat * time
     * y = b * distance + pHeat * distance / speed
     *   = (b + pHeat / speed) * distance
     *
     * y = battery capacity
     *      assume using 2022 Tesla Model 3, average full battery range = 165 mi, the usable capacity is 40 kWh (estimate)
     * @reference https://ev-database.uk/car/1060/Tesla-Model-3-Standard-Range#:~:text=Battery%20and%20Charging,on%20a%20fully%20charged%20battery.
     *
     * b = pure driving consumption efficiency
     *      unit = kWh/mile
     *
     * pHeat = delta temperature consumption
     *      P heat = 90W * ΔC
     *      assume inner-car climate: 20 celsius
     * @reference https://link.springer.com/article/10.1007/s12053-020-09900-5
     *
     * y = p * e
     * p = percentage of the battery remained (the user input)
     * e = efficiency for 1% of battery
     *      40kWh = 100 * e, e = 40/100 = 0.4kWh
     *
     * p * e = (b + pHeat / speed) * distance
     * distance = (p * e) / (b + pHeat / speed)
     * */
    fun calRemainRange(): Double? {
//        Log.e("jane", "enters calc")
        val b = 40.toDouble()/165
        var pHeat = 0.0
        if (!temps.value.isNullOrEmpty())
            pHeat = 0.09 * abs(temps.value!![0] - 20.0) * 0.5 + 0.09 * abs(temps.value!!.last() - 20.0) * 0.5
//        Log.e("jane", "b: $b, pHeat: $pHeat")
        if (battery.value == null) {
            battery.value = 100.00
        }
        return (battery.value?.times(0.4)!!)/(b + pHeat/60)
    }
}