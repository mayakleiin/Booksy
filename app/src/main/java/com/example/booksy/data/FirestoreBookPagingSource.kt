package com.example.booksy.data

import kotlinx.coroutines.tasks.await
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.booksy.model.Book
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirestoreBookPagingSource(
    private val query: Query
) : PagingSource<Query, Book>() {

    override suspend fun load(params: LoadParams<Query>): LoadResult<Query, Book> {
        return try {
            val currentQuery = params.key ?: query.limit(params.loadSize.toLong())
            val snapshot = currentQuery.get().await()
            val books = snapshot.toObjects(Book::class.java)

            val nextKey = if (snapshot.size() < params.loadSize) {
                null
            } else {
                val lastDoc = snapshot.documents.last()
                query.startAfter(lastDoc).limit(params.loadSize.toLong())
            }

            LoadResult.Page(
                data = books,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Query, Book>): Query? = null
}
