package com.example.criminalintent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*

/**
 * ViewModel of the CrimeFragment
 */
class CrimeDetailViewModel : ViewModel() {

    // inject the CrimeRepository singleton
    private val crimeRepository = CrimeRepository.get()

    // LiveData of the crimeId to be displayed
    private val crimeIdLiveData = MutableLiveData<UUID>()

    // CrimeFragment will observe this property once and will get notified each time
    // loadCrime(crimeId) is called with a new crimeId and the transformation exposes
    // a new Crime.
    var crimeLiveData: LiveData<Crime?> =
        Transformations.switchMap(crimeIdLiveData) { crimeId ->
            crimeRepository.getCrime(crimeId)
        }

    // This method is called onCreate of the CrimeFragment.
    // It initiates the loading thread from the database by setting the MutableLiveData value
    // of crimeIdLiveData which triggers the Transformation to crimeLiveData which in turn
    // is observed by the lambda defined in CrimeFragment.onViewCreated().
    fun loadCrime(crimeId: UUID) {
        crimeIdLiveData.value = crimeId
    }

    // This method is called by onStop of the CrimeFragment and saves the Crime to the database
    fun saveCrime(crime: Crime) {
        crimeRepository.updateCrime(crime)
    }

    // get the java.io.File for a crime
    fun getPhotoFile(crime: Crime): File {
        return crimeRepository.getPhotoFile(crime)
    }

}