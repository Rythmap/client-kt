package com.mvnh.rythmap.retrofit.yandex.entities

import com.google.gson.annotations.SerializedName

data class YandexTrack(
    @SerializedName("track_id") val trackId: String,
    val title: String,
    val artist: String,
    val img: String,
    val duration: Int,
    val minutes: Int,
    val seconds: Int,
    val album: String,
    @SerializedName("download_link") val downloadLink: String,
)
