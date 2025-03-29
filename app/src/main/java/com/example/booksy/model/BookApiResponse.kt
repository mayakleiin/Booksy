package com.example.booksy.model

data class BookApiResponse(
    val docs: List<BookDoc>
)

data class BookDoc(
    val title: String?,
    val author_name: List<String>?,
    val cover_i: Int?
) {
    fun getCoverUrl(): String {
        return "https://covers.openlibrary.org/b/id/${cover_i}-L.jpg"
    }
}
