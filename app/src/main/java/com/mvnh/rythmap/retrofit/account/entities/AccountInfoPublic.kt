package com.mvnh.rythmap.retrofit.account.entities

import com.google.gson.annotations.SerializedName

data class AccountInfoPublic(
    @SerializedName("account_id") val accountId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("visible_name") val visibleName: AccountVisibleName? = null,
    val about: String,
    @SerializedName("music_preferences") val musicPreferences: List<String>,
    @SerializedName("other_preferences") val otherPreferences: List<String>,
    @SerializedName("last_tracks") val lastTracks: AccountLastTracks,
    val friends: List<String>,
    @SerializedName("created_at") val createdAt: String,
)
