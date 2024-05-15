package com.mvnh.rythmap.responses.account.entities

import com.google.gson.annotations.SerializedName

data class AccountLastTracks(
    @SerializedName("yandex_track_id") val yandexTrackId: String,
    @SerializedName("spotify_track_id") val spotifyTrackId: String,
)