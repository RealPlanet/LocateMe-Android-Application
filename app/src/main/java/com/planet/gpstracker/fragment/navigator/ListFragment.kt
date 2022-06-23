package com.planet.gpstracker.fragment.navigator

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection

import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.planet.gpstracker.R
import com.planet.gpstracker.adapter.GPSListAdapter
import com.planet.gpstracker.fragment.base.GPSFragment
import com.planet.gpstracker.service.ServiceGPS
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class ListFragment : GPSFragment() {
    // region Fragment variables
    private lateinit var inflatedView : View
    private lateinit var itemList : RecyclerView
    // endregion

    override var serviceConnection  = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder){
            isBoundToService = true
            binderGPSService = service as ServiceGPS.GPSServiceBinder

            // Gather the latest data if it's available
            // This removed the need to setup bundles on pause or resume for the fragment
            if(binderGPSService.getLocationList().size > 0){
                onLocationListUpdate(binderGPSService.getLocationList())
            }

            binderGPSService.registerForLocationUpdate(this@ListFragment)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binderGPSService.unregisterForLocationUpdate(this@ListFragment)
            isBoundToService = false
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onLocationListUpdate(data : LinkedList<Location>) {
        if(!isBoundToService) return
        if(activity == null) return

        // The reference to the service list is given ONCE, future event callbacks simply notify the UI that data has changed
        if(itemList.adapter!!.itemCount == 0){
            itemList.adapter = GPSListAdapter(data)
        }

        // 99% of the times only the last element changes, after the 5 minutes mark old elements start
        // being deleted and thus I believe it's faster to use this instead of keeping track of the items
        // changes
        itemList.adapter!!.notifyDataSetChanged()
    }

    // region Fragment callbacks

    override fun onCreateView(
        inflater : LayoutInflater, container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View {

        inflatedView = inflater.inflate(R.layout.fragment_list, container, false)
        val recycleButton : FloatingActionButton = inflatedView.findViewById(R.id.btnDeleteLoc)
        recycleButton.setOnClickListener{
            discardHistoryButton()
        }

        itemList = inflatedView.findViewById(R.id.history_gps)
        // Setup the listAdapter with an empty list so avoid adapter errors in the log
        val llm = LinearLayoutManager(context)
        llm.generateDefaultLayoutParams()
        llm.orientation = LinearLayoutManager.VERTICAL
        itemList.adapter = GPSListAdapter(LinkedList<Location>())
        itemList.layoutManager = llm
        itemList.setHasFixedSize(false)

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

    // This suppression is to remove the warning as the logic within
    // will clear the entirety of the RecylerView
    @SuppressLint("NotifyDataSetChanged")
    private fun discardHistoryButton(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.hintDiscardDialogTitle))
            .setMessage(resources.getString(R.string.hintDiscardDialogDesc))
            .setNegativeButton(resources.getString(R.string.hintNo)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.hintYes)) { dialog, _ ->

                run {
                    binderGPSService.getLocationList().clear()
                    runBlocking {
                        launch {
                            binderGPSService.getDatabase().deleteAllLocations()
                        }
                    }

                    itemList.adapter?.notifyDataSetChanged()
                }
                dialog.dismiss()
            }
            .show()
    }
}