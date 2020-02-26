package ru.psu.places

import android.app.Application
import io.realm.Realm

class MarkerApp: Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
    }
}