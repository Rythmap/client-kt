package com.mvnh.rythmap.retrofit.account.entities

import com.google.gson.annotations.SerializedName

data class AccountRegister(
    val nickname: String,
    @SerializedName("visible_name") val visibleName: AccountVisibleName? = null,
    val password: String,
    val email: String,
    @SerializedName("music_preferences") val musicPreferences: List<String>? = null,
    @SerializedName("other_preferences") val otherPreferences: List<String>? = null,
    val about: String? = null,
)