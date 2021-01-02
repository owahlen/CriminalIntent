package com.example.criminalintent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.criminalintent.databinding.FragmentCrimeListBinding

class CrimeListFragment : Fragment() {

    // TAG for logging
    private val TAG = javaClass.simpleName

    // binding object to fragment_crime.xml
    private var _binding: FragmentCrimeListBinding? = null
    private val binding get() = _binding!!

    // Reference to the CrimeAdapter inner class
    private var adapter: CrimeAdapter = CrimeAdapter(emptyList())

    // Instantiate crimeListViewModel upon first access using the "lazy" delegated property.
    // The ViewModelProvider instantiates the CrimeListViewModel
    // and configures its lifecycle management.
    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this).get(CrimeListViewModel::class.java)
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
    ): View {
        // inflate fragment_crime_list.xml into the fragment_container of the MainActivity
        _binding = FragmentCrimeListBinding.inflate(inflater, container, false)
        // retrieve the RecyclerView from the inflated fragment_crime_list.xml
        // and configure it's LayoutManager which by default arranges items vertically
        binding.crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.crimeRecyclerView.adapter = adapter

        return binding.root
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
            viewLifecycleOwner,
            Observer { crimes ->
                crimes?.let {
                    Log.i(TAG, "Got crimes ${crimes.size}")
                    updateUI(crimes)
                }
            }
        )
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Instantiate and populate the CrimeAdapter with the list of crimes
     * stored in the crimeListViewModel. The CrimeAdapter is then used
     * as adapter of the RecyclerView of the fragment_crime_list.xml.
     */
    private fun updateUI(crimes: List<Crime>) {
        adapter = CrimeAdapter(crimes)
        binding.crimeRecyclerView.adapter = adapter
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
            val action = CrimeListFragmentDirections.actionCrimeListToCrime(crime.id)
            findNavController().navigate(action)
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

}