package com.planet.gpstracker.interfaces

import android.location.Location
import java.util.*

/**
 * This is a functional interface used to notify listeners of new
 * locations retrieved by the location service.
 * Since it's a functional interface it can be instantiated on the spot with a lambda
 */
@FunctionalInterface
interface IListenerLocationUpdate{
    fun onLocationListUpdate(data : LinkedList<Location>)
}