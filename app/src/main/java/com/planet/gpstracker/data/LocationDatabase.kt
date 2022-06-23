package com.planet.gpstracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.planet.gpstracker.data.entity.LocationEntity

@Database(
        entities = [
            LocationEntity::class
        ],
        version = 1,
        exportSchema = false
)

abstract class LocationDatabase : RoomDatabase(){
    abstract val locationDao: LocationDAO

    companion object {
        @Volatile
        private var INSTANCE : LocationDatabase? = null

        fun getInstance(context : Context) : LocationDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                        context.applicationContext,
                        LocationDatabase::class.java,
                        "location_db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}