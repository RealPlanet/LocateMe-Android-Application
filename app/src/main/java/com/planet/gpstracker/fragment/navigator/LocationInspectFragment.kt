package com.planet.gpstracker.fragment.navigator

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.planet.gpstracker.R
import com.planet.gpstracker.Utils

class LocationInspectFragment : Fragment() {
    private lateinit var location : Location

    override fun onCreateView(
        inflater : LayoutInflater, container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View? {
        // Inflate the layout for this fragment
        val inflatedView = inflater.inflate(R.layout.fragment_location_inspect, container, false)

        // region Fragment references
        // The back button only needs to pop back the fragment stack to go back to the main fragment
        val backButton : FloatingActionButton = inflatedView.findViewById(R.id.btnBack)
        backButton.setOnClickListener{
            requireActivity().onBackPressed()
        }

        val latitudeText :TextView = inflatedView.findViewById(R.id.latitudeInspectionView)
        val latText = getString(R.string.hintLatitudeColon)

        val longitudeText :TextView = inflatedView.findViewById(R.id.longitudeInspectionView)
        val longText = getString(R.string.hintLatitudeColon)

        val altitudeText :TextView = inflatedView.findViewById(R.id.altitudeInspectionView)
        val altText = getString(R.string.hintLatitudeColon)

        val bearingText : TextView = inflatedView.findViewById(R.id.bearingInspectionView)
        val bearText = getString(R.string.hintBearingColon)

        val speedText :TextView = inflatedView.findViewById(R.id.speedInspectionView)
        val spdText = getString(R.string.hintSpeedColon)

        val accuracyText :TextView = inflatedView.findViewById(R.id.accuracyInspectionView)
        val accText = getString(R.string.hintAccuracyColon)

        val timeStampText : TextView = inflatedView.findViewById(R.id.timestampTextView)
        val timeText = getString(R.string.hintString)
        //endregion

        //region Bundle restore
        // Grab the location data, if its present in the instance bundle then it is assumed it was put there following a screen rotation change.
        // If no data is present in the savedInstance bundle then it is assumed this fragment was just created and received the location as an argument.
        if(savedInstanceState != null && savedInstanceState.containsKey("inspectedLocation")){
            location = savedInstanceState.getParcelable("inspectedLocation")!!
            savedInstanceState.clear()
        } else{
            location = arguments?.getParcelable<Location>("location") as Location
        }

        // If no location is defined then hide the view and return an empty screen
        // by default the textViews are empty so user will be indirectly notified that something is not working
        if(!this::location.isInitialized){
            Log.e("INSPECT LOCATION", "No location was defined")
            return inflatedView
        }

        // endregion
        //region Location gather data
        // Some values are checked as location might contain null values, and some getters such as accuracy will
        // throw an exception instead of returning null when used.
        var accuracy = "N/A"
        var bearing = "N/A"
        var speed = "N/A"

        if(location.hasAccuracy())
            accuracy = location.accuracy.toString()

        if(location.hasBearing())
            bearing = location.bearing.toString()

        if(location.hasSpeed())
            speed = location.speed.toString()

        //endregion
        timeStampText.text = timeText.format(Utils.formatFromEpochTime(location.time))

        latitudeText.text = latText.format(location.latitude)
        longitudeText.text = longText.format(location.longitude)
        altitudeText.text = altText.format(location.altitude)

        bearingText.text = bearText.format(bearing)
        speedText.text = spdText.format(accuracy)
        accuracyText.text = accText.format(speed)

        return inflatedView
    }

    override fun onSaveInstanceState(outState : Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("inspectedLocation", location)
    }
}