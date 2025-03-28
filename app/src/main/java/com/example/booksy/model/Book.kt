package com.example.booksy.model

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val genre: String = "",
    val imageUrl: String = "",
    val status: BookStatus = BookStatus.AVAILABLE,
    val ownerId: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val pages: Int = 0,
    val description: String = "",
    val language: String = "",
    )

enum class BookStatus {
    AVAILABLE,
    BORROWED
}
