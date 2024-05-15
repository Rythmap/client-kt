package com.mvnh.rythmap.responses.account.entities

import com.google.gson.annotations.SerializedName

data class AccountUpdateInfo(
    val token: String?,
    @SerializedName("visible_name") val visibleName: AccountVisibleName? = null,
    @SerializedName("music_preferences") val musicPreferences: List<String>? = null,
    @SerializedName("other_preferences") val otherPreferences: List<String>? = null,
    val about: String? = null,
)
