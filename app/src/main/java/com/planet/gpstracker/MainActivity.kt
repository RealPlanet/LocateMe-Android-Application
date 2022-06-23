package com.planet.gpstracker

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.planet.gpstracker.service.ServiceGPS

class MainActivity : AppCompatActivity() {
    // region Activity variables
    private var serviceActive = false
    private var permissionRequestCount = 0
    private lateinit var permissionWarningToast : Toast
    private val locationPermissionRequest = registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions() ) { reqPermissions ->
        run {
            val permGranted = reqPermissions.all { it.value }

            if(permGranted){
                enableTracker()
            }
            else{
                if(permissionRequestCount < 2){
                    // Show the toast only once every application execution
                    if(permissionRequestCount == 1)
                        permissionWarningToast.show()

                    permissionRequestCount++
                    requestPermissions()
                }

                Log.d("PERMISSION REQUEST", "Permissions were not granted for GPS location")
            }
        }
    }
    private lateinit var serviceBinder : ServiceGPS.GPSServiceBinder
    /**
     *  This service connection is used by the activity as a wrapper to save the tracked positions when it needs to restart (most common cause would be the change of the phone's orientation)
     */
    private var serviceConnection = object : ServiceConnection {

        var isBound = false

        override fun onServiceConnected(className: ComponentName, service: IBinder){
            // Once the service is connected save a reference to the binder
            serviceBinder = service as ServiceGPS.GPSServiceBinder
            isBound = true
        }

        // Not needed in this use case
        override fun onServiceDisconnected(p0 : ComponentName?) {}
    }
    // endregion

    /**
     * This method handles all the required setup to execute the GPS service.
     *
     */
    private fun enableTracker(){
        val service = Intent(applicationContext, ServiceGPS::class.java)
        serviceActive = true
        startService(service)
        bindService(service, serviceConnection, 0)
    }

    private fun requestPermissions(){
        // For tracking we need both COARSE (Wifi / Cellphone data / Cell-tower) and FINE (GPS)
        locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    // region Activity callbacks
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionWarningToast = Toast.makeText(this, getString(R.string.hintRequiredPermissions), Toast.LENGTH_LONG)

        // Set the application in fullscreen mode when possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
        else {
            window.setFlags(
                    // Support for older systems
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Initialized bottom bar navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment?
        if (navHostFragment != null) {
            val navController = navHostFragment.navController
            val bottomNavigationView : BottomNavigationView = findViewById(R.id.bottom_navigation)
            bottomNavigationView.setupWithNavController(navController)
        }

        // If locations were previously given then skip the request and start the application
        if(ServiceGPS.hasLocationPermissions(this)){
            enableTracker()
            return
        }

        requestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(serviceConnection.isBound){
            serviceConnection.isBound = false
            unbindService(serviceConnection)
        }
    }

    // This override is done in order to send the application back into the background applications instead of closing it,
    // this allows the gps service to keep running and the application to exist.
    // The application can be shut down by swiping it in the application list (home button) OR rebooting the phone.
    override fun onBackPressed() {
        val controller = findNavController(R.id.navHostFragment)
        if(controller.backQueue.size > 2){
            super.onBackPressed()
            return
        }

        // Prevent closing app on back pressed
        val homeIntent = Intent()
        homeIntent.action = Intent.ACTION_MAIN
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        startActivity(homeIntent)
    }
    // endregion
}