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

// Genre with displayName
enum class Genre(val displayName: String) {
    FICTION("Fiction"),
    NON_FICTION("Non-Fiction"),
    SCIENCE("Science"),
    FANTASY("Fantasy"),
    HISTORY("History"),
    BIOGRAPHY("Biography"),
    MYSTERY("Mystery"),
    COMEDY("Comedy"),
    ROMANCE("Romance");

    companion object {
        fun fromDisplayName(name: String): Genre? =
            values().firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}

// Language with displayName
enum class Language(val displayName: String) {
    ENGLISH("English"),
    HEBREW("Hebrew"),
    RUSSIAN("Russian"),
    FRENCH("French"),
    ARABIC("Arabic"),
    SPANISH("Spanish"),
    GERMAN("German");

    companion object {
        fun fromDisplayName(name: String): Language? =
            values().firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}
