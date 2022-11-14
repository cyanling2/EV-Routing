package com.cmu.evplan

import com.google.android.gms.maps.model.LatLng

enum class AccessCode {
    private,
    public
}

enum class StatusCode {
    E /** Available */,
    P /** Planned */,
    T /** Temporarily Unavailable */
}

enum class FuelTypeCode () {
    BD /** Biodiesel */,
    CNG /** Compressed Natural Gas */,
    ELEC /** Electric */,
    E85 /** Ethanol */,
    HY /** Hydrogen */,
    LNG /** Liquefied Natural Gas */,
    LPG /** Propane */
}

enum class ConnecterTypeCode {
    all,
    NEMA1450,
    NEMA515,
    NEMA520,
    J1772,
    J1772COMBO,
    CHADEMO,
    TESLA
}

class MarkerType {
    lateinit var stationName: String
//    lateinit var chargerType: String    /** Deprecated */
    var connectors: ArrayList<ConnecterTypeCode> = ArrayList()
    lateinit var location: LatLng
    lateinit var phone: String
    lateinit var streetAddress: String
    lateinit var accessDaysTime: String
    lateinit var cardsAccepted: String
    lateinit var price: String
    lateinit var network: String
}