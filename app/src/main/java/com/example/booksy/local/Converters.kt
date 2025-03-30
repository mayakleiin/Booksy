package com.example.booksy.local

import androidx.room.TypeConverter
import com.example.booksy.model.Genre
import com.example.booksy.model.Language
import com.example.booksy.model.BookStatus

class Converters {

    @TypeConverter
    fun fromGenreList(genres: List<Genre>): String =
        genres.joinToString(",") { it.name }

    @TypeConverter
    fun toGenreList(data: String): List<Genre> =
        data.split(",").mapNotNull { Genre.valueOf(it) }

    @TypeConverter
    fun fromLanguageList(languages: List<Language>): String =
        languages.joinToString(",") { it.name }

    @TypeConverter
    fun toLanguageList(data: String): List<Language> =
        data.split(",").mapNotNull { Language.valueOf(it) }

    @TypeConverter
    fun fromStatus(status: BookStatus): String = status.name

    @TypeConverter
    fun toStatus(data: String): BookStatus = BookStatus.valueOf(data)
}
