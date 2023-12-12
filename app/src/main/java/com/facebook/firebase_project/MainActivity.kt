package com.facebook.firebase_project

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.facebook.firebase_project.databinding.ActivityMainBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private  lateinit var binding: ActivityMainBinding
    private val STORAGE_PERMISSION_CODE = 1
    val imageRef = Firebase.storage.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.StartButton.setOnClickListener {
            // Check for permission before accessing storage
            if (isStoragePermissionGranted()) {
                accessStorage()
            }
        }
    }

    private fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
                return false
            }
        } else {
            // Permissions are automatically granted below Android M
            return true
        }
    }

    private fun accessStorage() {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val cameraFolder = File(root, "Camera")

        if (cameraFolder.exists() && cameraFolder.isDirectory) {
            val imageFiles = getImageFiles(cameraFolder)

            if (imageFiles.isNotEmpty()) {
                // Do something with the list of image files
                for (imageFile in imageFiles) {
                    println("Image File: ${imageFile.name}")
                }
               Log.d("MainActivity",imageFiles.size.toString())
                uploadImagesToStorage(imageFiles)
            } else {
                Toast.makeText(this, "No image files found in Camera folder", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Camera folder does not exist or is not a directory", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImagesToStorage(imageFiles: ArrayList<File>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            for (imageFile in imageFiles) {
                // Generate a unique filename for each image
                val filename = UUID.randomUUID().toString()

                imageRef.child("images/$filename").putFile(imageFile.toUri()).await()

                withContext(Dispatchers.Main) {
                    Log.d("MainActivity", "Successfully uploaded image: $filename")
                    // You can add additional logic here if needed
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("MainActivity", "Error uploading images: ${e.message}")
                // Handle the error if needed
            }
        }
    }


    private fun getImageFiles(folder: File): ArrayList<File> {
        val imageFiles = ArrayList<File>()
        val files = folder.listFiles()

        if (files != null) {
            for (file in files) {
                if (file.isFile && isImageFile(file)) {
                    imageFiles.add(file)
                }
            }
        }

        return imageFiles
    }

    private fun isImageFile(file: File): Boolean {
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.toLowerCase())
        return mimeType != null && mimeType.startsWith("image")
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessStorage()
                } else {
                    Toast.makeText(
                        this,
                        "Permission denied. Cannot access storage.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

