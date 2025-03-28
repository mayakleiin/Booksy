package com.example.booksy.model

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val genres: List<Genre> = listOf(Genre.FICTION),
    val languages: List<Language> = listOf(Language.ENGLISH),
    val pages: Int = 0,
    val description: String = "",
    val imageUrl: String = "",
    val status: BookStatus = BookStatus.AVAILABLE,
    val ownerId: String = "",
    val lat: Double? = null,
    val lng: Double? = null
)

enum class BookStatus {
    AVAILABLE,
    BORROWED
}

// Enum for Books Genre
enum class Genre {
    FICTION,
    NON_FICTION,
    SCIENCE,
    FANTASY,
    HISTORY,
    BIOGRAPHY,
    MYSTERY,
    COMEDY,
    ROMANCE
}

// Enum for language
enum class Language {
    ENGLISH,
    HEBREW,
    RUSSIAN,
    FRENCH,
    ARABIC,
    SPANISH,
    GERMAN
}