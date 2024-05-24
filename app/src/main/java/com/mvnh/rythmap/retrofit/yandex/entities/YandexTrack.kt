package com.mvnh.rythmap.retrofit.yandex.entities

data class YandexTrack(
    val title: String,
    val artists: List<Artist>,
    val image: String,
)

data class Artist(
    val name: String
)
