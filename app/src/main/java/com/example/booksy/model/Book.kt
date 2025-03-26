package com.example.booksy.model

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val genre: String = "",
    val imageUrl: String = "",
    val status: Status = Status.AVAILABLE,
    val ownerId: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
) {
    enum class Status {
        AVAILABLE,
        BORROWED
    }
}
