package com.mvnh.rythmap.responses.account.entities

import com.google.gson.annotations.SerializedName

data class AccountAuth(
    @SerializedName("account_id") val accountId: String,
    val token: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
)