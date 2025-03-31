package com.example.booksy.data

import android.location.Location
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.booksy.model.Book
import com.example.booksy.model.BookStatus

class NearbyBooksPagingSource(
    private val allBooks: List<Book>,
    private val currentLocation: Location,
    private val maxDistanceMeters: Float,
    private val currentUserId: String?
) : PagingSource<Int, Book>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Book> {
        val page = params.key ?: 0
        val pageSize = params.loadSize

        val filteredBooks = allBooks.filter { book ->
            book.lat != null &&
                    book.lng != null &&
                    book.status == BookStatus.AVAILABLE &&
                    book.ownerId != currentUserId &&
                    calculateDistance(book) <= maxDistanceMeters
        }.sortedBy { calculateDistance(it) }

        val fromIndex = page * pageSize
        val toIndex = minOf(fromIndex + pageSize, filteredBooks.size)

        val pageBooks = if (fromIndex < filteredBooks.size) {
            filteredBooks.subList(fromIndex, toIndex)
        } else emptyList()

        return LoadResult.Page(
            data = pageBooks,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (toIndex < filteredBooks.size) page + 1 else null
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Book>): Int? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(1)
        }
    }

    private fun calculateDistance(book: Book): Float {
        val bookLocation = Location("").apply {
            latitude = book.lat!!
            longitude = book.lng!!
        }
        return currentLocation.distanceTo(bookLocation)
    }
}
