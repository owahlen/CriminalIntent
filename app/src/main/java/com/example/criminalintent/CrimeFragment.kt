package com.example.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.util.*

private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2
private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks {

    // TAG for logging
    private val TAG = javaClass.simpleName

    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView

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
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView

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
                    // get the File object for the Crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    // get URI from File that is exposed to the camera app
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                        // the authority as defined in the AndroidManifest.xml
                        "com.example.criminalintent.fileprovider",
                        photoFile
                    )
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

        // When the dateButton is clicked show the DatePickerFragment
        dateButton.setOnClickListener {
            // Populate the DatePickerFragment with the current crime.date using
            // the FragmentManager of the CrimeFragment.
            // Set this CrimeFragment as a target in order to receive the adjusted date back.
            // The DatePickerFragment with call the "onDateSelected" callback on this target.
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.getParentFragmentManager(), DIALOG_DATE)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                // populate the newly created intent to send plain text
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject)
                )
            }.also { intent ->
                // start the activity associated by the OS with the Intent
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            // create an intent to pick a contact from the address book
            val pickContactIntent =
                Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                }

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }


            // use the PackageManager to find out if the phone has able
            // to handle a pickContactIntent
            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(
                    pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            if (resolvedActivity == null) {
                // disable to suspect button if there is no contact app
                isEnabled = false
            }

        }

        photoButton.apply {
            // create the Intent to capture an image by a camera app
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            // use the package manager to see if the intent would be resolved
            // Note that in Android 11 this intent must be listed in the queries section
            // of the AndroidManifest.xml
            val packageManager: PackageManager = requireActivity().packageManager
            val resolveActivity: ResolveInfo? = packageManager.resolveActivity(
                captureImage,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            if (resolveActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                // add the photoUri to the intent: This URI represents a java.io.File
                // to which the camera app will write the image
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                // query the activites this intent might trigger
                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(
                    captureImage, PackageManager.MATCH_DEFAULT_ONLY
                )

                // grant the camera app(s) the permission to write to the photoFile
                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        // use the cameraActivitie's package name to give write access
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                startActivityForResult(captureImage, REQUEST_PHOTO)
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
     * Called when the fragment is no longer attached to its activity.  This
     * is called after {@link #onDestroy()}.
     */
    override fun onDetach() {
        super.onDetach()
        // revoke any permission to write to the photoUri
        requireActivity().revokeUriPermission(
            photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    /**
     * Implementation of the DatePickerFragment.Callbacks interface.
     * This is called by the DatePickerFragment when the user changes the date.
     */
    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
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
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }
        updatePhotoView()
    }

    /**
     * If there is a photoFile scale it to the display of the Activity of this Fragment
     * and place it into the photoView
     */
    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        } else {
            // show a cleared ImageDrawable
            photoView.setImageDrawable(null)
        }
    }

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(Intent, int)}.
     * This method is called when a contact has been selected in a contact app
     * after pushing the suspectButton or when a camera app has written a picture to the photoUri
     * after pushing the photoButton .
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            // a contact has been selected
            requestCode == REQUEST_CONTACT -> {
                val contactUri: Uri = data?.data ?: return
                // Specify which fields you want your query to return values for
                val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                // Perform your query - the contactUri is like a "where" clause here
                val cursor = requireActivity().contentResolver
                    .query(contactUri, projection, null, null, null)
                cursor?.use {
                    // Verify cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }

                    // Pull out the first column of the first row of data -
                    // that is your suspect's name
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }

            // a photo has been taken
            requestCode == REQUEST_PHOTO -> {
                // revoke all write permissions from other apps to this photoUri
                requireActivity().revokeUriPermission(
                    photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                updatePhotoView()
            }
        }
    }

    /**
     * populate the crime_report string
     */
    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        // use the crime_report string from strings.xml and populate the placeholders
        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspect
        )
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
