package com.planet.gpstracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.planet.gpstracker.data.entity.LocationEntity

@Dao
interface LocationDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location : LocationEntity)

    @Query("SELECT * FROM LocationEntity ORDER BY acquisitionTime DESC")
    suspend fun getLocations() : List<LocationEntity>

    @Query("DELETE FROM LocationEntity")
    suspend fun deleteAllLocations()
}