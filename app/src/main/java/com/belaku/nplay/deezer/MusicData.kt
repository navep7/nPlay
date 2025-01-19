package com.belaku.nplay.deezer

data class MusicData(
    val `data`: List<Data>,
    val next: String,
    val total: Int
)