package com.example.criminalintent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity(), CrimeListFragment.Callbacks {

    // TAG for logging
    private val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // currentFragment is not null if the activity is re-created (on rotation / reclaim memory)
        if (currentFragment == null) {
            val fragment = CrimeListFragment.newInstance() // CrimeFragment()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }

    /**
     * replace the CrimeListFragment currently shown in the fragment_container
     * with a CrimeFragment detail view.
     */
    override fun onCrimeSelected(crimeId: UUID) {
        val fragment = CrimeFragment.newInstance(crimeId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // put the CrimeFragment on the navigation stack
            .commit()
    }

}
