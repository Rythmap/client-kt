package com.mvnh.rythmap.responses.map

data class MapWSResponse(
    val nickname: String,
    val location: MapLocation,
    val status: String,
    val command: String? = null
)
