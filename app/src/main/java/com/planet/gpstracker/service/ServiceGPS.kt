package com.planet.gpstracker.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.planet.gpstracker.R
import com.planet.gpstracker.data.LocationDAO
import com.planet.gpstracker.data.LocationDatabase
import com.planet.gpstracker.data.entity.LocationEntity
import com.planet.gpstracker.interfaces.IListenerLocationUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.collections.ArrayList

/**
 *  Provides a list of locations to bounded objects
 *  The list is a LinkedList containing locations within a span of 5 minutes
 *  L -> L -> L -> L
 *
 *  The head of the linkedList represents the newest recorded location.
 *  The tail of the linkedList represents the oldest recorded location.
 */
class ServiceGPS : Service() {
    companion object{
        var CHANNEL_ID = "pl_gps_tracker"
        var NOTIFICATION_ID = 154123

        // time in milliseconds
        private var LOCATION_UPDATE_INTERVAL_MS : Long = 2000
        private var LOCATION_UPDATE_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY

        private var LOG_TAG = "GPS SERVICE"

        private var SERVICE_LOCATION_REQUEST = LocationRequest.create()
                                                .setInterval(LOCATION_UPDATE_INTERVAL_MS)
                                                .setFastestInterval(LOCATION_UPDATE_INTERVAL_MS)
                                                .setPriority(LOCATION_UPDATE_PRIORITY)
                                                .setMaxWaitTime(0) // Prevent location updates from being buffered to receive data as soon as possible

        /**
         * Checks if this application has been granted the ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions
         *
         * @return Boolean
         */
         fun hasLocationPermissions(ctx : Context) : Boolean{
            val hasFinePermission = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED
            val hasCoarsePermission = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED

            return  hasCoarsePermission &&
                    hasFinePermission
        }
    }

    // region Service variables
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var dao : LocationDAO

    private var fragmentListeners = ArrayList<IListenerLocationUpdate>()
    private var isServiceActive = false
    private var trackedLocations : LinkedList<Location> = LinkedList<Location>()


    private var time : Long = System.currentTimeMillis()
    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            Log.i(LOG_TAG, "Received location update ${(System.currentTimeMillis() - time) / 1000}")
            time = System.currentTimeMillis()

            // Guard -- technically not needed as callback is only called with valid locations
            if(locationResult.locations.size == 0){
                Log.i(LOG_TAG, "No locations available")
                return
            }

            addLocationToList(locationResult.lastLocation)
            for(fragment in fragmentListeners){
                fragment.onLocationListUpdate(trackedLocations)
            }
        }

        // This is always called
        override fun onLocationAvailability(p0 : LocationAvailability) {
            super.onLocationAvailability(p0)
            Log.i(LOG_TAG, "Location unavailable")
            if(!p0.isLocationAvailable){
                Log.i(LOG_TAG, "Adding fake location")
                // In the event of no available locations (for example GPS is turned off) generate a fake location with no data
                val location = Location("default")
                location.time = System.currentTimeMillis()
                addLocationToList(location)
            }
        }

        private fun addLocationToList(location : Location){
            val iterator = trackedLocations.asReversed().iterator()

            // This iterator scrolls the position list backwards and deletes stale positions. As soon as a valid one is found the
            // scan is stopped
            while(iterator.hasNext()) {
                val oldLocation = iterator.next()
                if (isLocationTooOld(oldLocation.time, 5 * 60 * 1000)){
                    Log.i("LOCATION LIST UPDATE", "Removed stale location, there are ${trackedLocations.count() - 1} in the tracked list")
                    iterator.remove()
                    continue
                }
                // Starting from the oldest location in the list. If it's valid then all future locations will be valid too
                break
            }

            trackedLocations.addFirst(location)
            // Always write new locations to database as contingency if the application closes without the ability to save the latest list.
            writeLocationToDatabase(location)
        }
    }
    // endregion

    // region Service Binder
    inner class GPSServiceBinder : Binder(){
        fun registerForLocationUpdate( listener : IListenerLocationUpdate){
            // Silently fail if listener is present in list
            if(fragmentListeners.contains(listener)){
                return
            }

            fragmentListeners.add(listener)
        }

        fun unregisterForLocationUpdate( listener : IListenerLocationUpdate){
            // Silently fail if listener not present in list
            if(!fragmentListeners.contains(listener)){
                return
            }

            fragmentListeners.remove(listener)
        }

        /**
         * Returns the stored positions within the service as a LinkedList
         * An empty array will be returned if this connection is not bound to the service
         * @return Array<Location>
         */
        fun getLocationList() : LinkedList<Location> {
            return trackedLocations
        }

        /**
         * Gets a reference to the application database of locations
         *
         * @return the DAO reference
         */
        fun getDatabase() : LocationDAO {
            return dao
        }
    }

    private val serviceBinder = GPSServiceBinder()
    override fun onBind(p0 : Intent?) : IBinder {
        return serviceBinder
    }
    // endregion

    // region Service methods
    override fun onStartCommand(intent : Intent, flags : Int, startId : Int) : Int {
        Log.i(LOG_TAG, "onStartCommand service")
        // We want the service to restart if it's stopped for any reason by the OS but we do not need the context
        return START_STICKY
    }

    override fun onCreate() {
        Log.i(LOG_TAG, "onCreate service")
        super.onCreate()
        // region Service setup
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name : CharSequence = "Background Application GPS Tracking"
            val description = "Background location updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system
            val notificationManager = getSystemService(
                    NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }

        setupService()
        // endregion
    }

    override fun onDestroy(){
        Log.i(LOG_TAG, "onDestroy() service")
        super.onDestroy()

        fragmentListeners.clear()
        if (isServiceActive) {
            Log.i(LOG_TAG, "Stopping location service")
            writeSavedLocations()

            isServiceActive = false
            fusedLocationClient.removeLocationUpdates(locationCallback)
            stopForeground(true)
        }
    }

    /**
     *  When the application is swiped in the applications list this vent is fired
     *  causing the service to close itself
     * @param rootIntent sent by system
     */
    override fun onTaskRemoved(rootIntent : Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf() // Remove the service on application close-swipe
    }
    // endregion

    // Permissions are request if this method is called
    @SuppressLint("MissingPermission")
    private fun setupService(){
        // region Service guard
        if(isServiceActive){
            return
        }

        isServiceActive = true
        // endregion

        // region Notification builder
        val notificationBuilder : Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(applicationContext, CHANNEL_ID)
        else
            Notification.Builder(applicationContext)

        notificationBuilder.setContentTitle(getString(R.string.hintGPSServiceTitle))
        notificationBuilder.setContentText(getString(R.string.hintGPSServiceContent))
        notificationBuilder.setSmallIcon(R.drawable.location_ico)
        val serviceNotification = notificationBuilder.build()
        // endregion

        // region Init location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try{
            fusedLocationClient.requestLocationUpdates(SERVICE_LOCATION_REQUEST, locationCallback, Looper.getMainLooper())
        }
        catch (ex : SecurityException){
            Log.e(LOG_TAG, ex.toString())
        }
        // endregion

        //region Database setup
        dao = LocationDatabase.getInstance(baseContext).locationDao
        loadSavedLocations()
        //endregion

        startForeground(NOTIFICATION_ID, serviceNotification)
    }

    // region DB Operations
    /**
     * Converts the location data stored into the database into a Location Object
     *
     * @param locationEntry The database entry of a specific location
     * @return the new Location
     */
    private fun parseLocation(locationEntry : LocationEntity) : Location{
        val location = Location("fused")
        location.latitude = locationEntry.latitude
        location.longitude = locationEntry.longitude
        location.altitude = locationEntry.altitude
        location.time = locationEntry.acquisitionTime

        val bearing = locationEntry.bearing.toFloatOrNull()
        if( bearing != null){
            location.bearing = bearing
        }

        val accuracy = locationEntry.accuracy.toFloatOrNull()
        if( accuracy != null){
            location.accuracy = accuracy
        }

        val speed = locationEntry.speed.toFloatOrNull()
        if( speed != null){
            location.speed = speed
        }

        return location
    }

    /**
     * This method writes to the database all of the currently stored locations after clearing the table of all previously existing locations
     *
     */
    private fun writeSavedLocations(){
        runBlocking {
            dao.deleteAllLocations()
        }

        for(location in trackedLocations)
            writeLocationToDatabase(location)
    }

    private fun writeLocationToDatabase(location : Location){
        runBlocking{
            var bearing = ""
            var accuracy = ""
            var speed = ""
            val time = location.time
            val latitude = location.latitude
            val longitude = location.longitude
            val altitude = location.altitude

            if (location.hasBearing()){
                bearing = location.bearing.toString()
            }

            if (location.hasAccuracy()){
                accuracy = location.accuracy.toString()
            }

            if (location.hasSpeed()){
                speed = location.speed.toString()
            }

            launch{
                dao.insertLocation(LocationEntity(0,
                        time,
                        latitude,
                        longitude,
                        altitude,
                        bearing,
                        accuracy,
                        speed))
            }
        }
    }

    /**
     * Loads all saved locations from the database and does the following:
     *  -for each location check if it was saved more than 5 minutes ago.
     *  -if it's still valid parse the entity data and add it to both the service list
     *  -as contingency, add it back into the database in case onDestroy is not called
     */
    private fun loadSavedLocations(){
        // Take the saved locations from the database and put them into memory
        runBlocking {
            val storedLocations = dao.getLocations()
            dao.deleteAllLocations()
            // This for must read the database list

            for (locationEntry in storedLocations){
                // All positions after this one are "useless" as older than 5 minutes
                if(isLocationTooOld(locationEntry.acquisitionTime, 5 * 60 * 1000)){
                    Log.i("DB UPDATE", "Stopping parsing due to finding stale location")
                    break
                }

                // The getLocations database query returns the locations ordered from newest to oldest
                trackedLocations.addLast(parseLocation(locationEntry))
                dao.insertLocation(locationEntry)
            }
        }
    }

    /**
     * This methods confronts the given time and the given limit and returns a boolean indicating
     * if the given time is under the time limit or not
     * The formula used is epoch_time - recorded_epoch_time >= timeLimitMillis
     * @param recordedTimeMillis    The recorded time in milliseconds (epoch)
     * @param timeLimitMillis       The time limit in milliseconds (not epoch, so 5 minutes would be 5 * 60 * 1000)
     * @return
     */
    private fun isLocationTooOld(recordedTimeMillis : Long, timeLimitMillis : Long) : Boolean {
        return System.currentTimeMillis() - recordedTimeMillis >= timeLimitMillis
    }
    //endregion
}