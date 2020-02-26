package ru.psu.places.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import ru.psu.places.R
import ru.psu.places.model.MarkedMarker
import ru.psu.places.utils.RealmUtility

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        realm = Realm.getInstance(RealmUtility().getDefaultConfig()!!)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapClickListener {coordinates -> onMapClick(coordinates)}
        map.setOnMarkerClickListener {marker -> onMarkerClick(marker)}
        realm.where<MarkedMarker>().findAll().forEach { marker -> map.addMarker(
            MarkerOptions().position(LatLng(marker.latitude, marker.longitude))) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK){
            val modifiedPosition: LatLng? = data?.extras?.getParcelable<LatLng>("position")
            if (modifiedPosition != null)
                realm.executeTransaction { realm ->
                    getMarker(modifiedPosition, realm)
                        ?.photoURI = data.extras?.getString("photoUri")
                }
        }
    }

    private fun addMarker(coordinates: LatLng){
        realm.executeTransaction {
            realm ->
            run {
                val marker = realm.createObject<MarkedMarker>()
                marker.latitude = coordinates.latitude
                marker.longitude = coordinates.longitude
            }
        }
        map.addMarker(MarkerOptions().position(coordinates))
    }

    private fun callPhotoActivity(marker: Marker?) {
        startActivityForResult(
            Intent(this, PhotoActivity::class.java)
                .putExtra("position", marker?.position)
                .putExtra("photoUri", getMarker(marker!!.position, realm)?.photoURI), 1
        )
    }

    private fun getMarker(position: LatLng, realm: Realm):MarkedMarker?{
        return realm.where<MarkedMarker>()
            .equalTo("latitude", position.latitude)
            .and().equalTo("longitude", position.longitude)
            .findFirst()
    }

    private fun onMapClick(coordinates: LatLng) {
        AlertDialog.Builder(this)
            .setTitle(R.string.new_marker_dialog_title)
            .setMessage(R.string.new_marker_dialog_message)
            .setPositiveButton(android.R.string.yes) { _, _ -> addMarker(coordinates)}
            .setNegativeButton(android.R.string.no) { _, _->}
            .show()
    }

    private fun onMarkerClick(clickedMarker: Marker?): Boolean {
        AlertDialog.Builder(this)
            .setTitle(R.string.marker_photo_title)
            .setMessage(R.string.marker_photo_message)
            .setPositiveButton(android.R.string.yes) { _, _ -> callPhotoActivity(clickedMarker)}
            .setNeutralButton(R.string.remove_marker) { _, _ -> removeMarker(clickedMarker)}
            .setNegativeButton(android.R.string.no) { _, _->}
            .show()
        return true
    }

    private fun removeMarker(marker: Marker?){
        realm.executeTransaction { realm ->
            getMarker(marker!!.position, realm)?.deleteFromRealm()
        }
        marker!!.remove()
    }
}
