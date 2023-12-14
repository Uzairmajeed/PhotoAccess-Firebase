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
                // Check for existing images in cloud storage
                CoroutineScope(Dispatchers.IO).launch {
                val existingImages = getExistingImages(storageFolderName).toSet()

                // Filter out images that already exist in cloud storage
                val newImages = imageFiles.filter { !existingImages.contains(it.name) }

                if (newImages.isNotEmpty()) {
                    // Do something with the list of new image files
                    for (imageFile in newImages) {
                        println("New Image File: ${imageFile.name}")
                    }
                    Log.d("MainActivity", "Number of new images in ${folder.name}: ${newImages.size}")
                    uploadImagesToStorage(newImages, storageFolderName)
                }
                else {
                    Log.d("MainActivity", "No new images found in ${folder.name}")
                }
                }
            } else {
                Toast.makeText(applicationContext, "No image files found in ${folder.name}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(applicationContext, "${folder.name} folder does not exist or is not a directory", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun getExistingImages(storageFolderName: String): List<String> {
        // Retrieve a list of items in the specified storage folder
        return try {
            imageRef.child(storageFolderName).listAll().await().items.map { it.name }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting existing images: ${e.message}")
            emptyList()
        }
    }

    private fun uploadImagesToStorage(imageFiles: List<File>, storageFolderName: String) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (imageFile in imageFiles) {
                    // Use the original filename for each image
                    val filename = imageFile.name

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

