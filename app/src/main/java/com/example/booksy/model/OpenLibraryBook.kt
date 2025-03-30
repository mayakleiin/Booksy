package com.example.booksy.model

data class OpenLibraryResponse(
    val docs: List<OpenLibraryBook>
)

data class OpenLibraryBook(
    val title: String?,
    val author_name: List<String>?,
    val cover_i: Int?,
    val number_of_pages_median: Int?,
    val first_publish_year: Int?,
    val key: String?,
    val publish_place: List<String>?,
    val subject: List<String>?,
    val isbn: List<String>?,
    val publisher: List<String>?,
    val language: List<String>?,
    val edition_count: Int?,
    val publish_year: List<Int>?,
    val first_sentence: Map<String, String>?
) {
    fun getAuthor(): String = author_name?.joinToString(", ") ?: ""
    fun getCoverUrl(): String =
        if (cover_i != null) "https://covers.openlibrary.org/b/id/${cover_i}-L.jpg"
        else ""
    fun getDescription(): String =
        first_sentence?.values?.firstOrNull() ?: ""
}
