package com.mvnh.rythmap.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mvnh.rythmap.retrofit.account.entities.AccountVisibleName

class AccountVM : ViewModel() {
    var nickname = MutableLiveData<String>()
    var visibleName = MutableLiveData<AccountVisibleName>()
    var about = MutableLiveData<String>()

    fun setNickname(nickname: String) {
        this.nickname.value = nickname
    }

    fun setVisibleName(visibleName: AccountVisibleName) {
        this.visibleName.value = visibleName
    }

    fun setAbout(about: String) {
        this.about.value = about
    }

    fun getNickname(): String? {
        return nickname.value
    }

    fun getVisibleName(): AccountVisibleName? {
        return visibleName.value
    }

    fun getAbout(): String? {
        return about.value
    }
}