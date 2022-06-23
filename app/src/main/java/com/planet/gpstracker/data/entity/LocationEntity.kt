package com.planet.gpstracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int,
    @ColumnInfo
    val acquisitionTime : Long, // Time from epoch of location acquisition
    @ColumnInfo
    val latitude : Double,
    @ColumnInfo
    val longitude : Double,
    @ColumnInfo
    val altitude : Double,
    @ColumnInfo
    val bearing : String,
    @ColumnInfo
    val accuracy : String,
    @ColumnInfo
    val speed : String,
    )