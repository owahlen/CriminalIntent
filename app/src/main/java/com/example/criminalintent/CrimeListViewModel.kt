package com.example.criminalintent

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for the CrimeListFragment
 */
class CrimeListViewModel : ViewModel() {

    // inject the CrimeRepository singleton
    private val crimeRepository: CrimeRepository = CrimeRepository.get()

    // LiveData of the List of all Crime instances in the database.
    // When the CrimeListViewModel is lazily created the crimeListLiveData is observed
    // for data coming from the underlying crimeRepository and the UI is updated accordingly
    val crimeListLiveData: LiveData<List<Crime>> = crimeRepository.getCrimes()
}