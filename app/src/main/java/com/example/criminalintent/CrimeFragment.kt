package com.example.criminalintent

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.util.*

private const val ARG_CRIME_ID = "crime_id"

class CrimeFragment private constructor() : Fragment() {

    // TAG for logging
    private val TAG = javaClass.simpleName

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    // Lazy initialization of the crimeDetailViewModel associated with this CrimeFragment
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    /**
     * Called to do initial creation of a fragment. This is called after
     * {@link #onAttach(Activity)} and before
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        // load the "crime_id" from the arguments of the CrimeFragment.
        // This has been put in there by the "newInstance" factory method which was called
        // by MainActivity.onCrimeSelected()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * {@link #onCreate(Bundle)} and
     * {@link #onActivityCreated(Bundle)}.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // inflate the fragment into the parent ViewGroup
        // but let the Activity handle adding the view later
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        // wire up widgets
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox

        dateButton.apply {
            text = crime.date.toString()
            isEnabled = false
        }

        return view
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * It registers an Observer to update the UI in case associated LiveData changes.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner, { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            }
        )
    }

    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to {@link Activity#onStart() Activity.onStart} of the containing
     * Activity's lifecycle.
     */
    override fun onStart() {
        super.onStart()

        // Use Kotlin's object expression to create an instance of an anonymous class
        // implementing the TextWatcher interface
        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // This implementation is required by the interface but not used.
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // This implementation is required by the interface but not used.
            }
        }

        // add the titleWatcher to the title field to update the crime model's title
        // in case the user enters a text into the titleField
        titleField.addTextChangedListener(titleWatcher)

        // bind the "isChecked" property of the "solvedCheckBox"
        // to the "isSolved" property of the cirme model.
        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }
    }

    /**
     * Called when the Fragment is no longer started.
     */
    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    /**
     * updateUI() is called by the Observer defined in OnViewCreated()
     */
    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            // Setting the checkbox is slightly delayed by LiveData fetching data from the database.
            // Nevertheless when showing this screen no animations should be shown and
            // the UI should jump to the correct state.
            jumpDrawablesToCurrentState()
        }
    }

    companion object {

        /**
         * Factory method of CrimeFragment that populates an arguments bundle
         */
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}
