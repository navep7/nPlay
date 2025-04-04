package com.parvatha.music.spotify

data class Data(
    val artists: Artists,
    val coverArt: CoverArt,
    val date: Date,
    val name: String,
    val uri: String
)