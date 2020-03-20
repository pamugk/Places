package ru.psu.places.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
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
import ru.psu.places.utils.PermissionUtils
import ru.psu.places.utils.RealmUtility
import kotlin.math.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val KILOMETERS_PER_DEGREE = 111.1

    private lateinit var map: GoogleMap
    private lateinit var realm: Realm
    private lateinit var locationCallback: LocationCallback

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationClient: FusedLocationProviderClient

    fun calculateDistance(marker:MarkedMarker, location:Location):Double {
        val dl = marker.longitude - location.longitude
        val cosF1 = cos(marker.latitude)
        val sinF1 = sin(marker.latitude)
        val cosF2 = cos(location.latitude)
        val sinF2 = sin(location.latitude)
        val tmp1 = cosF2 * sin(dl)
        val tmp2 = cosF1 * sinF2 - sinF1 * cosF2 * cos(dl)

        return KILOMETERS_PER_DEGREE * atan(
            sqrt(tmp1 * tmp1 + tmp2 * tmp2) /
                    (sinF1 * sinF2 + cosF1 * cosF2 * cos(dl))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        realm = Realm.getInstance(RealmUtility().getDefaultConfig()!!)
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 20 * 1000
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations)
                    if (realm.where<MarkedMarker>().findAll().any {
                            marker -> calculateDistance(marker, location) <= 1.0
                    })
                        with(NotificationManagerCompat.from(baseContext)) {
                            notify(123,  NotificationCompat.Builder(baseContext, "12345")
                                .setContentTitle(getString(R.string.placeNotificationTitle))
                                .setContentText(getString(R.string.placeNotificationText))
                                .setStyle(NotificationCompat.BigTextStyle()
                                    .bigText(getString(R.string.placeNotificationText)))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .build())
                        }
                println("Some point is nigh")
            }
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }


    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (PermissionUtils.isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
            map.isMyLocationEnabled = true
        else
            println("Location permissions are denied")
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
            map.setMyLocationEnabled(true)
        else
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true)
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
