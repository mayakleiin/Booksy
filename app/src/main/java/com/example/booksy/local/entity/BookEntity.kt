package com.example.booksy.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val genres: String,     // CSV
    val languages: String,  // CSV
    val pages: Int,
    val description: String,
    val imageUrl: String,
    val status: String,     // AVAILABLE / BORROWED
    val ownerId: String,
    val lat: Double?,
    val lng: Double?
)
