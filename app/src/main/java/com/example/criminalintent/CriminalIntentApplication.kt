package com.example.criminalintent

import android.app.Application

/**
 * Application class registered in the AndroidManifest.xml
 */
class CriminalIntentApplication : Application() {

    // Called when the application is starting, before any activity, service,
    // or receiver objects (excluding content providers) have been created.
    override fun onCreate() {
        super.onCreate()
        // instantiate the CrimeRepository singleton
        CrimeRepository.initialize(this)
    }
}
