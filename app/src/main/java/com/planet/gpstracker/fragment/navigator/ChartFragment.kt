package com.planet.gpstracker.fragment.navigator

import android.content.ComponentName
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.planet.gpstracker.R
import com.planet.gpstracker.fragment.base.GPSFragment
import com.planet.gpstracker.service.ServiceGPS
import java.util.*
import kotlin.collections.ArrayList

class ChartFragment : GPSFragment() {
    // region Fragment variables
    private lateinit var inflatedView : View
    private lateinit var latitudeChart : LineChart
    private lateinit var longitudeChart : LineChart
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

            binderGPSService.registerForLocationUpdate(this@ChartFragment)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binderGPSService.unregisterForLocationUpdate(this@ChartFragment)
            isBoundToService = false
        }
    }

    private fun generateLineDataSet(values : ArrayList<Entry>, label : String, color : Int) : LineDataSet{
        val result = LineDataSet(values, label)
        result.axisDependency = YAxis.AxisDependency.LEFT
        result.color = color
        result.fillColor = Color.WHITE
        result.highLightColor = Color.WHITE
        result.valueTextColor = Color.WHITE

        result.setDrawCircles(false)
        result.lineWidth = 2f
        result.circleRadius = 3f
        result.fillAlpha = 0
        result.setDrawFilled(false)

        result.setDrawCircleHole(false)

        return result
    }

    private fun setChartData(chart : LineChart, values : ArrayList<Entry>, valueLabel : String, valueColor : Int){
        if(chart.data != null && chart.data.dataSetCount > 0){
            val set = chart.data.getDataSetByIndex(0) as LineDataSet

            set.values = values

            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
            return
        }

        val lineData = LineData(generateLineDataSet(values, valueLabel, valueColor))
        lineData.setDrawValues(false)
        chart.data = lineData

        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.invalidate()
        chart.fitScreen()
    }

    private fun initChart(chart : LineChart){
        // X axis settings
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.xAxis.isGranularityEnabled = true
        chart.xAxis.typeface = Typeface.MONOSPACE
        chart.xAxis.textColor = Color.WHITE

        // Y axis settings
        chart.axisLeft.textColor = Color.WHITE
        chart.axisRight.isEnabled = false
        chart.description = Description()
        chart.description.text = ""
        chart.legend.textColor = Color.WHITE
        chart.legend.textSize = 15f
        chart.setNoDataText(getString(R.string.hintChartNoData))
        chart.setDrawBorders(true)
        chart.setBorderColor(Color.WHITE)
    }

    private val latitudeValueSet = arrayListOf<Entry>()
    private val longitudeValueSet = arrayListOf<Entry>()

    override fun onLocationListUpdate(data : LinkedList<Location>) {
        if(!isBoundToService) return
        if(activity == null) return

        latitudeValueSet.clear()
        longitudeValueSet.clear()

        for((index, location) in data.reversed().withIndex()){
            latitudeValueSet.add(Entry(index.toFloat() + 1, location.latitude.toFloat()))
            longitudeValueSet.add(Entry(index.toFloat() + 1, location.longitude.toFloat()))
        }

        setChartData(latitudeChart, latitudeValueSet, getString(R.string.hintLatitude), Color.RED)
        setChartData(longitudeChart, longitudeValueSet, getString(R.string.hintLongitude), Color.GREEN)
    }

    // region Fragment callbacks
    override fun onCreateView(
        inflater : LayoutInflater, container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View {

        inflatedView = inflater.inflate(R.layout.fragment_chart, container, false)

        latitudeChart = inflatedView.findViewById(R.id.latitude_chart)
        longitudeChart = inflatedView.findViewById(R.id.longitude_chart)

        initChart(latitudeChart)
        initChart(longitudeChart)

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