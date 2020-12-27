package com.example.criminalintent

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.example.criminalintent.database.CrimeDatabase
import java.util.*

private const val DATABASE_NAME = "crime-database"

/**
 * The CrimeRepository is a singleton that holds the reference to the CrimeDatabase.
 * The repository encapsulates where the model is stored (i.e. the database).
 */
class CrimeRepository private constructor(context: Context) {

    // Use Room to build the concrete CrimeDatabase and privately store it in the singleton
    private val database : CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    ).build()

    // the CrimeRepository adapts the access methods of the Dao
    private val crimeDao = database.crimeDao()

    // Repository operations
    fun getCrimes(): LiveData<List<Crime>> = crimeDao.getCrimes()

    fun getCrime(id: UUID): LiveData<Crime?> = crimeDao.getCrime(id)

    // companion to instantiate this class as a singleton
    companion object {
        private var INSTANCE: CrimeRepository? = null

        // initialization is called onCreate of the CriminalIntentApplication
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository {
            return INSTANCE ?: throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}
