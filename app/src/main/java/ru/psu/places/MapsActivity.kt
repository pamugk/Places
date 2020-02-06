package ru.psu.places

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var photos: HashMap<LatLng, String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val savedPhotos = savedInstanceState?.getSerializable("markers")
        photos = if (savedPhotos != null)
            savedPhotos as HashMap<LatLng, String?>
        else
            hashMapOf()
        val extra = intent.extras
        if (extra != null)
            photos[extra.getParcelable("position")!!] = extra.getString("photoUri")
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("markers", photos)
        super.onSaveInstanceState(outState)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapClickListener {coordinates -> onMapClick(coordinates)}
        map.setOnMarkerClickListener {marker -> onMarkerClick(marker)}
        photos.keys.forEach { position -> map.addMarker(MarkerOptions().position(position)) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK){
            val modifiedPosition: LatLng = data?.extras?.getParcelable<LatLng>("position") as LatLng
            photos[modifiedPosition] = data.extras?.getString("photoUri")
        }
    }

    private fun addMarker(coordinates: LatLng){
        photos[coordinates] = null
        map.addMarker(MarkerOptions().position(coordinates))
    }

    private fun removeMarker(marker: Marker?){
        photos.remove(marker!!.position)
        marker.remove()
    }

    private fun onMapClick(coordinates: LatLng) {
        AlertDialog.Builder(this)
            .setTitle(R.string.new_marker_dialog_title)
            .setMessage(R.string.new_marker_dialog_message)
            .setPositiveButton(android.R.string.yes) { _, _ -> addMarker(coordinates)}
            .setNegativeButton(android.R.string.no) { _, _->}
            .show()
    }

    private fun callPhotoActivity(marker: Marker?) {
        startActivityForResult(
            Intent(this, PhotoActivity::class.java)
                .putExtra("position", marker?.position)
                .putExtra("photoUri", photos[marker?.position]), 1
        )
    }

    private fun onMarkerClick(clickedMarker: Marker?): Boolean {
        AlertDialog.Builder(this)
            .setTitle(R.string.marker_photo_title)
            .setMessage(R.string.marker_photo_message)
            .setPositiveButton(android.R.string.yes) { _, _ -> callPhotoActivity(clickedMarker)}
            .setNeutralButton(R.string.remove_marker) {_,_ -> removeMarker(clickedMarker)}
            .setNegativeButton(android.R.string.no) { _, _->}
            .show()
        return true
    }
}
