package com.medicaltranslator.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    
    const val PERMISSION_REQUEST_CODE = 1000
    
    /**
     * Get all required permissions for the app
     */
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        
        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        // Network permission for local Termux services
        permissions.add(Manifest.permission.INTERNET)
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        
        // Manage external storage for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        }
        
        return permissions
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(context: Context): List<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request permissions from activity
     */
    fun requestPermissions(activity: Activity) {
        val missingPermissions = getMissingPermissions(activity)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * Check if specific permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if storage permissions are granted
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(context, Manifest.permission.READ_MEDIA_IMAGES) &&
            isPermissionGranted(context, Manifest.permission.READ_MEDIA_VIDEO) &&
            isPermissionGranted(context, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            isPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE) &&
            isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Check if network permissions are granted
     */
    fun isNetworkPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.INTERNET) &&
               isPermissionGranted(context, Manifest.permission.ACCESS_NETWORK_STATE)
    }
}

