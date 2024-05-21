package com.mvnh.rythmap.responses.account.entities

data class SendFriendRequest(
    val fromToken: String,
    val toNickname: String,
    val message: String? = null
)
