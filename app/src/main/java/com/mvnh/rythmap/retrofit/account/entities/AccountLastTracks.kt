package com.mvnh.rythmap.retrofit.account.entities

import com.google.gson.annotations.SerializedName
import com.mvnh.rythmap.retrofit.yandex.entities.YandexTrack

data class AccountLastTracks(
    @SerializedName("yandex_track") val yandexTrack: YandexTrack?,
    // @SerializedName("spotify_track") val spotifyTrack: String,
)