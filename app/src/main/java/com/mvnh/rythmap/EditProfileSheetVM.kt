package com.mvnh.rythmap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EditProfileSheetVM : ViewModel() {
    val triggerAccountInfoRetrieval = MutableLiveData<Boolean>()
}