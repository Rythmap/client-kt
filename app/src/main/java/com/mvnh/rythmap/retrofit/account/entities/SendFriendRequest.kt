package com.mvnh.rythmap.retrofit.account.entities

data class SendFriendRequest(
    val fromToken: String,
    val toNickname: String,
    val message: String? = null
)
