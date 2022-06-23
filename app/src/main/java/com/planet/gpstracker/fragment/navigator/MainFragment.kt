package com.planet.gpstracker.fragment.navigator

import android.content.ComponentName
import android.content.ServiceConnection
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.planet.gpstracker.R
import com.planet.gpstracker.fragment.base.GPSFragment
import com.planet.gpstracker.service.ServiceGPS
import java.util.*
import kotlin.math.abs


class MainFragment : GPSFragment() {

    // region Fragment variables
    private lateinit var latitudeTView : TextView
    private lateinit var longitudeTView : TextView
    private lateinit var altitudeTView : TextView
    private lateinit var inflatedView : View
    private lateinit var latitudeVisualIndicator : ImageView
    private lateinit var longitudeVisualIndicator : ImageView
    private lateinit var altitudeVisualIndicator : ImageView
    private var lastRecordedLatitude : Double = -1.0
    private var lastRecordedLongitude : Double = -1.0
    private var lastRecordedAltitude : Double = -1.0
    // endregion

    override var serviceConnection  = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder){
            isBoundToService = true
            binderGPSService = service as ServiceGPS.GPSServiceBinder
            binderGPSService.registerForLocationUpdate(this@MainFragment)

            // Gather the latest data if it's available
            // This removed the need to setup bundles on pause or resume for the fragment
            if(binderGPSService.getLocationList().size > 0){
                updateLocationDisplay(binderGPSService.getLocationList().first)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBoundToService = false
            binderGPSService.unregisterForLocationUpdate(this@MainFragment)
        }
    }

    /**
     * This method handles updating the UI with relevant information from the given location
     *
     * @param latestLocation The location from which to take the data
     */
    private fun updateLocationDisplay(latestLocation : Location){
        // Prevent app from crashing if var are not initialized for any reason
        if(!this::latitudeTView.isInitialized){
            return
        }

        if(determineVarianceIcon(latitudeVisualIndicator, latestLocation.latitude, lastRecordedLatitude))
            lastRecordedLatitude = latestLocation.latitude

        if(determineVarianceIcon(longitudeVisualIndicator, latestLocation.longitude, lastRecordedLongitude))
            lastRecordedLongitude = latestLocation.longitude

        if(determineVarianceIcon(altitudeVisualIndicator, latestLocation.altitude, lastRecordedAltitude))
            lastRecordedAltitude = latestLocation.altitude

        latitudeTView.text = getString(R.string.hintNumeric).format(latestLocation.latitude)
        longitudeTView.text = getString(R.string.hintNumeric).format(latestLocation.longitude)
        altitudeTView.text = getString(R.string.hintNumeric).format(latestLocation.altitude)
    }

    /**
     *  Location callback invoked by the gps service.
     *  On a received update this fragment will update the UI by showing the user the latest location data
     *  and indicating if it remains unchanged/increases/decreases.
     * @param data
     */
    override fun onLocationListUpdate(data : LinkedList<Location>) {
        if(!isBoundToService) return
        if(activity == null) return


        updateLocationDisplay(data.first)
    }

    /**
     * This method edits the displayed image of a ImageView to visually indicate if a difference in value is:
     *
     *      -Increasing
     *
     *      -Invariant
     *
     *      -Decreasing
     *
     *
     *  And returns a boolean:
     *
     *      - true == Value is increasing or decreasing
     *
     *      - false == Value is not changing or not big enough to be noticeable
     *
     *
     * @param view The ImageView object reference to modify
     * @param newValue The new recorded value
     * @param lastValue The latest recorded value
     * @return True if the newValue was big enough to be considered different from the lastValue otherwise false
     */
    private fun determineVarianceIcon(view : ImageView, newValue : Double, lastValue : Double) : Boolean{
        val diff = newValue - lastValue

        // Magic number, if difference between new and last values is smaller than 10^-6 then its considered to be the same
        if(abs(diff) < 10E-6){
            view.setImageResource(R.drawable.ui_approx)
            view.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow))
            view.rotation = 0f
            return false
        }

        view.setImageResource(R.drawable.ui_arrow)

        // if the coordinates are going down then point arrow down by rotating the imageView
        if(diff < 0){
            view.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
            view.rotation = 180f
            return true
        }

        view.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))
        view.rotation = 0f
        return true
    }

    // region Fragment callbacks
    override fun onCreateView(
        inflater : LayoutInflater, container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View {

        inflatedView = inflater.inflate(R.layout.fragment_main, container, false)

        // region "Setup references"
        // Inflate the layout for this fragment

        //val historyButton : Button = inflatedView.findViewById(R.id.historyButton)
        latitudeTView = inflatedView.findViewById(R.id.latitudeView)
        longitudeTView = inflatedView.findViewById(R.id.longitudeView)
        altitudeTView  = inflatedView.findViewById(R.id.altitudeView)

        val defaultLocationValue = getString(R.string.hintNumeric).format(0f)
        latitudeTView.text = defaultLocationValue
        longitudeTView.text = defaultLocationValue
        altitudeTView.text = defaultLocationValue

        Log.i("FRAGMENT UPDATE", "Initialized References")
        // endregion

        // region Navigator set-up

        // These indicators visually show the user the general variation of a specific coordinate (up / down / still)
        latitudeVisualIndicator = inflatedView.findViewById(R.id.latitudeVisualIndicator)
        longitudeVisualIndicator = inflatedView.findViewById(R.id.longitudeVisualIndicator)
        altitudeVisualIndicator = inflatedView.findViewById(R.id.altitudeVisualIndicator)

        Log.i("FRAGMENT UPDATE", "Navigator setup completed")
        // endregion

        // At the end of the setup bind to the gps service to receive update
        // Even if on resume binds fragment is bound here to avoid small delay in connection. BindService has a guard against double binding
        bindService(requireContext())

        return inflatedView
    }

    override fun onPause() {
        super.onPause()
        unBindService(requireContext())
    }

    override fun onResume() {
        super.onResume()
        bindService(requireContext())
    }
    // endregion
}