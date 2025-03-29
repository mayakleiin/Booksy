package com.example.booksy.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface BookApiService {
    @GET("search.json")
    suspend fun searchBooks(@Query("q") query: String): BookSearchResponse
}
