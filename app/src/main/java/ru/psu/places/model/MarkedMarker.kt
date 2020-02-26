package ru.psu.places.model

import io.realm.RealmObject

open class MarkedMarker(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var photoURI: String? = null
): RealmObject()