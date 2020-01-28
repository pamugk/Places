package ru.psu.places

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_photo.*
import java.io.File
import java.io.IOException
import java.text.DateFormat.getDateTimeInstance

class PhotoActivity: AppCompatActivity() {
    private lateinit var position: LatLng
    private var photoUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)
        newImageBtn.setOnClickListener {makeNewPhoto()}
        saveBtn.setOnClickListener {saveChanges()}
        cancelBtn.setOnClickListener {cancelChanges()}
        val extraThingies = intent.extras
        if (savedInstanceState != null){
            position = savedInstanceState.getParcelable("position")!!
            photoUri = savedInstanceState.getString("photoUri")
        }
        if (extraThingies != null) {
            if (extraThingies.getParcelable<LatLng>("position") != null)
                position = extraThingies.getParcelable("position")!!
            photoUri = extraThingies.getString("photoUri")
        }
        if (photoUri != null)
            setPhoto()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("position", position)
        outState.putString("photoUri", photoUri)
    }

    private fun makeNewPhoto(){
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (camera.resolveActivity(packageManager) != null){
            val photoFile: File?
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                println("IOException")
                return
            }
            camera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
            startActivityForResult(camera, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            try {
                setPhoto()
                saveBtn.isEnabled = true
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    @Throws(IOException::class)
    private fun setPhoto(){
        val mImageBitmap = MediaStore.Images.Media.getBitmap(
            this.contentResolver,
            Uri.parse(photoUri)
        )
        markerPhoto.setImageBitmap(mImageBitmap)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = getDateTimeInstance()
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            DIRECTORY_PICTURES
        )
        val image = File.createTempFile(imageFileName,".jpg",storageDir)
        photoUri = "file:" + image.absolutePath
        return image
    }

    private fun saveChanges() {
        startActivity(
            Intent(this, MapsActivity::class.java)
                .putExtra("changed", true)
                .putExtra("position", position)
                .putExtra("photoUri", photoUri)
        )
    }

    private fun cancelChanges() {
        startActivity(
            Intent(this, MapsActivity::class.java)
                .putExtra("changed", false)
        )
    }
}