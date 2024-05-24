package com.mvnh.rythmap.retrofit.map

data class MapWSResponse(
    val nickname: String,
    val location: MapLocation,
    val status: String,
    val command: String? = null
)
