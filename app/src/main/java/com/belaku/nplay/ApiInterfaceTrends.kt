package com.belaku.nplay

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterfaceTrends {

    @GET("https://api.deezer.com/chart/0/tracks")
    fun getTrending(): Call<MusicData>
    fun getData(@Query("q") query: String) : Call<MusicData>
}