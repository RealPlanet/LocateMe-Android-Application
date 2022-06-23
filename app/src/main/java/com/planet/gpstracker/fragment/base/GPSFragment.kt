package com.planet.gpstracker.fragment.base

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.fragment.app.Fragment
import com.planet.gpstracker.interfaces.IListenerLocationUpdate
import com.planet.gpstracker.service.ServiceGPS

/**
 * This abstract class provides base implementation of the required structure to connect to the gps service
 * Including methods to handle binding and unbinding.
 */
abstract class GPSFragment : IListenerLocationUpdate, Fragment() {
    protected lateinit var binderGPSService : ServiceGPS.GPSServiceBinder
    protected abstract var serviceConnection : ServiceConnection
    protected var isBoundToService = false

    /**
     * Safely unbind from the service
     *
     */
    protected fun unBindService(ctx : Context){
        // Avoid multiple bind calls
        if (!isBoundToService)
            return

        // Check permissions
        if(!ServiceGPS.hasLocationPermissions(ctx))
            return

        isBoundToService = false
        // Remove the listener from the binder
        binderGPSService.unregisterForLocationUpdate(this)
        requireActivity().unbindService(serviceConnection)

        // Set this here to avoid multiple unbind call in the time frame between the first request and the actual unbind!

    }

    /**
     * Safely bind to the service @see com.planet.gpstracker.service.ServiceGPS
     *
     */
    protected fun bindService(ctx : Context){
        // Avoid multiple bind calls
        if (isBoundToService){
            return
        }

        // Check permissions
        if(!ServiceGPS.hasLocationPermissions(ctx))
            return

        // Bind
        requireActivity().bindService(
                Intent(requireActivity().applicationContext, ServiceGPS::class.java),
                serviceConnection,
                0)
    }
}