package com.mvnh.rythmap.vm

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountUpdateInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileSheetVM : ViewModel() {
    private val accountApi = ServiceGenerator.createService(AccountApi::class.java)

    val accountInfoUpdated = MutableLiveData<Boolean>()

    fun updateAccountInfo(accountUpdateInfo: AccountUpdateInfo) {
        val call = accountApi.updateInfo(accountUpdateInfo)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Account info updated")
                    accountInfoUpdated.value = true
                } else {
                    Log.e(TAG, response.raw().toString())
                    accountInfoUpdated.value = false
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, t.toString())
                accountInfoUpdated.value = false
            }
        })
    }
}