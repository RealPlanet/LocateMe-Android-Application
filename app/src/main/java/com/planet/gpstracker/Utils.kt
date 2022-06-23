package com.planet.gpstracker

import java.util.Calendar.*

class Utils {
    companion object{
        // Small util method to parse epoch time into HH::MM:SS
        fun formatFromEpochTime(time : Long) : String{
            val c = getInstance()
            c.timeInMillis = time
            val hour = c.get(HOUR_OF_DAY)
            val minutes = c.get(MINUTE)
            val seconds = c.get(SECOND)
            return String.format("%02d:%02d:%02d", hour, minutes, seconds)
        }
    }
}