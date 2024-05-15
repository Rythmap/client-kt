package com.mvnh.rythmap.responses.yandex

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface YandexApi {
    @GET("music/yandex/track/get_and_save_current")
    fun getAndSaveCurrent(
        @Query("rythmapToken") rythmapToken: String,
        yandexToken: String
    ): Call<ResponseBody>

    @GET("music/yandex/track/info")
    fun getTrackInfo(
        @Query("trackID") trackID: String
    ): Call<ResponseBody>
}