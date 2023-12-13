package com.facebook.firebase_project

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.facebook.firebase_project.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private  lateinit var binding: ActivityMainBinding
    private val STORAGE_PERMISSION_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            MyWorker::class.java,
            15, TimeUnit.MINUTES // Interval to run the worker
        ).build()
        WorkManager.getInstance(applicationContext).enqueue(periodicWorkRequest)
        // Check for permission before accessing storage
           isStoragePermissionGranted()


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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val periodicWorkRequest = PeriodicWorkRequest.Builder(
                        MyWorker::class.java,
                        15, TimeUnit.MINUTES // Interval to run the worker
                    ).build()
                    WorkManager.getInstance(applicationContext).enqueue(periodicWorkRequest)
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

