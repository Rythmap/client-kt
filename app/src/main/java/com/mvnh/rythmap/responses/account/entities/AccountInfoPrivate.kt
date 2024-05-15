package com.mvnh.rythmap.responses.account.entities

import com.google.gson.annotations.SerializedName

data class AccountInfoPrivate(
    @SerializedName("account_id") val accountId: String,
    @SerializedName("token") val token: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("visible_name") val visibleName: AccountVisibleName? = null,
    val about: String? = null,
    val email: String,
    @SerializedName("music_preferences") val musicPreferences: List<String>? = null,
    @SerializedName("other_preferences") val otherPreferences: List<String>? = null,
    @SerializedName("last_tracks") val lastTracks: AccountLastTracks? = null,
    val friends: List<String>? = null,
    @SerializedName("created_at") val createdAt: String,
)