package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseApp.initializeApp(this)
                Log.d("FirebaseInit", "Firebase auto-initialized successfully in MainApplication")
            } catch (e: Exception) {
                Log.e("FirebaseInit", "Firebase init failed: ${e.message}")
                try {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:797877783285:android:e36878d63438cedf1293bc")
                        .setApiKey("AIzaSyBUkX8-8mJSbhXHDr0WT6LVLPvLmigikBw")
                        .setProjectId("meowkimi-c9a59")
                        .setStorageBucket("meowkimi-c9a59.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.d("FirebaseInit", "Firebase initialized with fallback options")
                } catch (fe: Exception) {
                    Log.e("FirebaseInit", "Firebase fallback init failed: ${fe.message}")
                }
            }
        }
    }
}
