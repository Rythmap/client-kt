package com.mvnh.rythmap.retrofit.account.entities

import com.google.gson.annotations.SerializedName

data class AccountInfoBasic(
    @SerializedName("account_id") val accountId: String,
    val nickname: String,
    @SerializedName("visible_name") val visibleName: AccountVisibleName? = null,
    @SerializedName("created_at") val createdAt: String,
)