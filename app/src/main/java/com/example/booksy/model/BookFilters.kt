package com.example.booksy.model

data class BookFilters(
    val maxDistanceKm: Float = 10f,
    val selectedGenres: List<Genre> = emptyList(),
    val selectedLanguages: List<Language> = emptyList()
)
