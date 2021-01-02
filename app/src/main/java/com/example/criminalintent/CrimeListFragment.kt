package com.example.criminalintent

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class CrimeListFragment : Fragment() {

    // TAG for logging
    private val TAG = javaClass.simpleName

    // Interface to be implemented by any hosting activity (e.g. MainActivity).
    // Its implementation enforces that MainActivity implements a behavior for
    // the case when a Crime is selected in the list.
    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    // callbacks is set/unset in onAttach()/onDetach() of this CrimeListFragment()
    // to the hosting MainActivity.
    private var callbacks: Callbacks? = null

    // Reference to the crimeRecyclerView that is part of the fragment_crime_list.xml
    // The RecyclerView provides a limited window into a large data set.
    private lateinit var crimeRecyclerView: RecyclerView

    // Reference to the CrimeAdapter inner class
    private var adapter: CrimeAdapter = CrimeAdapter(emptyList())

    // Instantiate crimeListViewModel upon first access using the "lazy" delegated property.
    // The ViewModelProvider instantiates the CrimeListViewModel
    // and configures its lifecycle management.
    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this).get(CrimeListViewModel::class.java)
    }

    /**
     * Called when the fragment is first attached to the MainActivity.
     * Since CrimeListFragment is hosted in an activity, the Context parameter
     * is the activity instance hosting the fragment.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Activity)} and before
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the FragmentManager to call "onCreateOptionsMenu" for this Fragment
        setHasOptionsMenu(true)
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
        // inflate fragment_crime_list.xml into the fragment_container of the MainActivity
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)

        // retrieve the RecyclerView from the inflated fragment_crime_list.xml
        // and configure it's LayoutManager which by default arranges items vertically
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = adapter

        return view
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, signaling that the fragment’s view hierarchy is in place.
     * This ensures that any views are ready to display the crime data.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Register an Observer on the crimeListViewModel.crimeListLiveData LiveData instance.
        // The LiveData instance will unregister the Observer as long as the lifetime of the
        // viewLifecycleOwner (i.e. the Fragment's view) is no longer in a valid state.
        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner, { crimes ->
                crimes?.let {
                    Log.i(TAG, "Got crimes ${crimes.size}")
                    updateUI(crimes)
                }
            }
        )
    }

    /**
     * Called when the fragment is no longer attached to the MainActivity.
     */
    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // populate the Menu instance with the properties defined in menu/fragment_crime_list.xml
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                // the new_crime menu item has been selected
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Instantiate and populate the CrimeAdapter with the list of crimes
     * stored in the crimeListViewModel. The CrimeAdapter is then used
     * as adapter of the RecyclerView of the fragment_crime_list.xml.
     */
    private fun updateUI(crimes: List<Crime>) {
        adapter = CrimeAdapter(crimes)
        crimeRecyclerView.adapter = adapter
    }

    /**
     * A list of these ViewHolders are built by the CrimeAdapter and referenced by the RecyclerView.
     * They hold the reference to the inflated list_item_crime and also provide the
     * reference to the contained TextViews.
     */
    private inner class CrimeHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private lateinit var crime: Crime

        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)

        init {
            // The CrimeHolder holds the view it is responsible for in the property "itemView".
            // The CrimeHolder registers itself as OnClickListener of this itemView.
            itemView.setOnClickListener(this)
        }

        /**
         * Bind the CrimeHolder to its Crime model
         */
        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text = this.crime.date.toString()
            solvedImageView.visibility = if (crime.isSolved) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        override fun onClick(v: View?) {
            // call onCrimeSelected() in the hosting MainActivity
            callbacks?.onCrimeSelected(crime.id)
        }
    }

    /**
     * The RecyclerView has no knowledge of the associated Views (i.e. list_item_crime)
     * or their model (i.e. Crime). Instead it delegates the management of these
     * items to the CrimeAdapter. It creates a CrimeHolder that inflates the list_item_crime
     * view and holds a reference to it. It also binds the crime model (title and date) to
     * the TextViews of the list_item_crime.
     */
    private inner class CrimeAdapter(var crimes: List<Crime>) :
        RecyclerView.Adapter<CrimeHolder>() {

        // For the visible items (and some more backup items) managed by the CrimeAdapter
        // the RecyclerView will call onCreateViewHolder to inflate the list_item_crime view
        // and hold it. It will recycle the CrimeHolders if they are out of the visible area
        // of the RecyclerView.
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            Log.d(TAG, "onCreateViewHolder()")
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        // number of crimes the CrimeAdapter is responsible for.
        override fun getItemCount(): Int = crimes.size

        // bind the CrimeHolder at the given position to its crime model
        // This method is called every time the view associated with
        // the ViewHolder needs to be rendered. Thus this code is expensive!
        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            Log.d(TAG, "onBindViewHolder() for item $position")
            val crime = crimes[position]
            holder.bind(crime)
        }
    }

    companion object {
        /**
         * Factory method of the CrimeListFragment
         */
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}