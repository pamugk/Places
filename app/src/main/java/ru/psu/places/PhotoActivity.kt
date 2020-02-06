package ru.psu.places

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_photo.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PhotoActivity: AppCompatActivity() {
    private lateinit var position: LatLng
    private var photoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)
        newImageBtn.setOnClickListener {makeNewPhoto()}
        saveBtn.setOnClickListener {saveChanges()}
        cancelBtn.setOnClickListener {cancelChanges()}
        val extraThingies = intent.extras
        if (savedInstanceState != null){
            position = savedInstanceState.getParcelable("position")!!
            photoPath = savedInstanceState.getString("photoUri")
        }
        if (extraThingies != null) {
            if (extraThingies.getParcelable<LatLng>("position") != null)
                position = extraThingies.getParcelable("position")!!
            photoPath = extraThingies.getString("photoUri")
        }
        if (photoPath != null)
            setPic()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("position", position)
        outState.putString("photoUri", photoPath)
    }

    val REQUEST_TAKE_PHOTO = 1

    private fun makeNewPhoto(){
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "ru.psu.places.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK){
            setPic()
            saveBtn.isEnabled = true
        }
    }

    private fun setPic() {
        val targetW: Int = markerPhoto.maxWidth
        val targetH: Int = markerPhoto.maxHeight
        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            val photoW: Int = outWidth
            val photoH: Int = outHeight
            val scaleFactor: Int = Math.min(photoW / targetW, photoH / targetH)
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }
        BitmapFactory.decodeFile(photoPath, bmOptions)?.also { bitmap ->
            markerPhoto.setImageBitmap(bitmap)
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply { photoPath = absolutePath }
    }


    private fun saveChanges() {
        setResult(Activity.RESULT_OK,
            Intent(this, MapsActivity::class.java)
            .putExtra("position", position)
            .putExtra("photoUri", photoPath))
        finish()
    }

    private fun cancelChanges() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}