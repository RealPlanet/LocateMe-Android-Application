package com.planet.gpstracker.adapter

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.planet.gpstracker.R
import com.planet.gpstracker.Utils
import com.planet.gpstracker.fragment.navigator.ListFragmentDirections
import java.util.*

class GPSListAdapter(private val dataSet: LinkedList<Location>) : RecyclerView.Adapter<GPSListAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)
        val inspectButton : FloatingActionButton = view.findViewById(R.id.btnExploreLocation)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.text_row_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val location = dataSet[position]
        viewHolder.textView.text =  viewHolder.itemView.context.getString(R.string.hintItemLocationList)
            .format(Utils.formatFromEpochTime(location.time), location.latitude,  location.longitude,  location.altitude)

        viewHolder.inspectButton.setOnClickListener{
            val action = ListFragmentDirections.actionInspectLocation(dataSet[position])
            viewHolder.view.findNavController().navigate(action)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}