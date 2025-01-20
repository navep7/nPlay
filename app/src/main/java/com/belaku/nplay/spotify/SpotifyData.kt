package com.belaku.nplay.spotify

data class SpotifyData(
    val albums: Albums,
    val artists: ArtistsX,
    val episodes: Episodes,
    val genres: Genres,
    val playlists: Playlists,
    val podcasts: Podcasts,
    val topResults: TopResults,
    val tracks: Tracks,
    val users: Users
)