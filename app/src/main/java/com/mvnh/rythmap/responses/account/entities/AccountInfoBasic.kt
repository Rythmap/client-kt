package com.mvnh.rythmap.responses.account.entities

import com.google.gson.annotations.SerializedName

data class AccountInfoBasic(
    @SerializedName("account_id") val accountId: String,
    @SerializedName("visible_name") val visibleName: AccountVisibleName? = null,
    val nickname: String,
)
