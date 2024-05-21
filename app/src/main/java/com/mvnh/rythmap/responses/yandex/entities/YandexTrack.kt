package com.mvnh.rythmap.responses.yandex.entities

import com.google.gson.annotations.SerializedName

data class YandexTrack(
    val title: String,
    val artists: List<Artist>,
    val og_image: String,
)

data class Artist(
    val name: String
)
