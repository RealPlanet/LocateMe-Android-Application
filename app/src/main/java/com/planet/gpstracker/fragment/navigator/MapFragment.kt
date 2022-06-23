package com.planet.gpstracker.fragment.navigator

import android.content.ComponentName
import android.content.ServiceConnection
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.planet.gpstracker.R
import com.planet.gpstracker.fragment.base.GPSFragment
import com.planet.gpstracker.service.ServiceGPS
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.math.abs

class MapFragment : GPSFragment(), OnMapReadyCallback{

    override var serviceConnection = object: ServiceConnection{
        override fun onServiceConnected(className: ComponentName, service: IBinder){
            isBoundToService = true
            binderGPSService = service as ServiceGPS.GPSServiceBinder

            if(this@MapFragment::gMap.isInitialized && binderGPSService.getLocationList().size >0)
                onLocationListUpdate(binderGPSService.getLocationList())

            binderGPSService.registerForLocationUpdate(this@MapFragment)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binderGPSService.unregisterForLocationUpdate(this@MapFragment)
            isBoundToService = false
        }
    }

    private lateinit var gMap : GoogleMap
    // These three objects are instantiated at fragment startup and kept around to reduce memory allocation
    private var lastLocation = Location("default")
    private var lastLatLng = LatLng(0.0, 0.0)
    private var mapMarker = MarkerOptions()

    override fun onLocationListUpdate(data : LinkedList<Location>) {
        if(!isBoundToService) return
        if(activity == null) return
        if(!this::gMap.isInitialized) return

        // To reduce the number of map updates a new marker is only positioned if coordinates changed enough
        // in this case the difference between the two coordinates needs to be higher than 10R-6
        val location = data.first
        val diffLatitude = abs(location.latitude - lastLocation.latitude)
        val diffLongitude = abs(location.longitude - lastLocation.longitude)
        if (diffLongitude < 10E-6 && diffLatitude < 10E-6) {
            return
        }

        lastLocation = location
        lastLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
        mapMarker.position( lastLatLng)

        gMap.clear()
        gMap.addMarker(mapMarker)
    }

    override fun onCreateView(
        inflater : LayoutInflater, container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View? {
        // Inflate the layout for this fragment
        val inflatedView = inflater.inflate(R.layout.fragment_map, container, false)

        val centerButton : Button = inflatedView.findViewById(R.id.btnCenter)
        centerButton.setOnClickListener{
            if(activity != null && this::gMap.isInitialized){
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 15f))
            }
        }

        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        // Even if on resume binds fragment is bound here to avoid small delay in connection. BindService has a guard against double binding
        bindService(requireContext())
        return inflatedView
    }

    // Once the map is ready set it's default position to the "center of europe"
    override fun onMapReady(p0 : GoogleMap) {
        gMap = p0

        val centerEU = LatLng(50.0, 7.59)
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerEU, 5f))
    }

    override fun onPause() {
        super.onPause()
        unBindService(requireContext())
    }

    override fun onResume() {
        super.onResume()
        bindService(requireContext())
    }
}