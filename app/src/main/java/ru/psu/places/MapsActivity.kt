package ru.psu.places

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var positions: MutableList<LatLng>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        positions = mutableListOf()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapClickListener(this)
        map.setOnMarkerClickListener(this)
    }

    private fun addMarker(coordinates: LatLng){
        positions.add(coordinates)
        map.addMarker(MarkerOptions().position(coordinates))
    }

    override fun onMapClick(coordinates: LatLng) {
        AlertDialog.Builder(this)
            .setTitle(R.string.new_marker_dialog_title)
            .setMessage(R.string.new_marker_dialog_message)
            .setPositiveButton(android.R.string.yes) { _, _ -> addMarker(coordinates)}
            .setNegativeButton(android.R.string.no) { _, _->}
            .show()
    }

    override fun onMarkerClick(clickedMarker: Marker?): Boolean {
        AlertDialog.Builder(this)
            .setTitle(R.string.marker_photo_title)
            .setMessage(R.string.marker_photo_message)
            .setPositiveButton(android.R.string.yes) { _, _ -> }
            .setNeutralButton(R.string.remove_marker) {_,_ -> clickedMarker?.remove()}
            .setNegativeButton(android.R.string.no) { _, _->}
            .show()
        return true
    }
}
