package com.facebook.firebase_project

// MyWorker.kt
import android.content.Context
import android.media.ExifInterface
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.net.toUri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val imageRef = Firebase.storage.reference

    override fun doWork(): Result {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val cameraFolder = File(root, "Camera")
        val screenshotsFolder = File(root, "Screenshots")

        // Process Camera folder
        processImageFolder(cameraFolder, "CameraImages")

        // Process Screenshots folder
        processImageFolder(screenshotsFolder, "ScreenshotImages")

        return Result.success()
    }

    private fun processImageFolder(folder: File, storageFolderName: String) {
        if (folder.exists() && folder.isDirectory) {
            val imageFiles = getImageFiles(folder)

            if (imageFiles.isNotEmpty()) {
                // Call uploadImagesToStorage without checking for existing images
                Log.d("MainActivity", "${imageFiles.size}")
                uploadImagesToStorage(imageFiles, storageFolderName)
            } else {
                Toast.makeText(applicationContext, "No image files found in ${folder.name}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(applicationContext, "${folder.name} folder does not exist or is not a directory", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun getExistingImages(folderPath: String): List<String> {
        // Retrieve a list of items in the specified storage folder
        return try {
            imageRef.child(folderPath).listAll().await().items.map { it.name }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting existing images: ${e.message}")
            emptyList()
        }
    }

    private  fun uploadImagesToStorage(imageFiles: List<File>, storageFolderName: String) {
        try {
            for (imageFile in imageFiles) {
                // Use the original filename for each image
                val filename = imageFile.name.replace("_", "")

                // Get the date of the image file
               val dateTaken = getCaptureDate(imageFile)
                Log.d("Dates", "$dateTaken")
                // Construct folderPath without filename
                val folderPath = "$storageFolderName/Dated_$dateTaken/"

                // Check for existing images in the specified folder (without filename)
                CoroutineScope(Dispatchers.IO).launch {
                    val existingImages = getExistingImages(folderPath).toSet()
                    if (filename !in existingImages) {
                        // Upload to the specified storage folder (including filename in path)
                        imageRef.child("$folderPath$filename").putFile(imageFile.toUri()).await()
                        Log.d("MainActivity", "Successfully uploaded image to $folderPath$filename")
                    } else {
                        Log.d("MainActivity", "Image $filename already exists in $folderPath. Skipping upload.")
                    }
                }


            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error uploading images to $storageFolderName: ${e.message}")
            // Handle the error if needed
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
    private fun getCaptureDate(imageFile: File): String {
        try {
            val exif = ExifInterface(imageFile.path)
            val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME)

            if (!dateString.isNullOrEmpty()) {
                // Parse the date string to a Date object
                val date = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(dateString)

                // Format the date to your desired output format
                return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the case where the date cannot be extracted from metadata
            // You might want to log an error or return a default value.
        }
        // If date extraction fails, use the current date as a fallback
        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }



}
