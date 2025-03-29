package com.example.booksy.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.booksy.model.Book
import com.example.booksy.model.BookFilters
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private var allBooks: List<Book> = emptyList()

    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    private val _nearbyBooks = MutableLiveData<List<Pair<Book, Float>>>()
    val nearbyBooks: LiveData<List<Pair<Book, Float>>> = _nearbyBooks

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentLocation: Location? = null
    private var currentFilters = BookFilters()

    private val _filterDistanceMeters = MutableLiveData(2000f)
    val filterDistanceMeters: LiveData<Float> = _filterDistanceMeters

    fun applyFilters(filters: BookFilters) {
        currentFilters = filters
        _filterDistanceMeters.value = filters.maxDistanceKm * 1000
        filterBooks()
        calculateNearbyBooks()
    }


    private fun filterBooks() {
        val location = currentLocation
        val maxDistanceMeters = currentFilters.maxDistanceKm * 1000

        val filtered = allBooks.filter { book ->
            val matchesGenre = currentFilters.selectedGenres.isEmpty() || book.genres.any { it in currentFilters.selectedGenres }
            val matchesLanguage = currentFilters.selectedLanguages.isEmpty() || book.languages.any { it in currentFilters.selectedLanguages }

            val distanceOk = if (location != null && book.lat != null && book.lng != null) {
                val bookLocation = Location("").apply {
                    latitude = book.lat
                    longitude = book.lng
                }
                location.distanceTo(bookLocation) <= maxDistanceMeters
            } else {
                // אם אין מיקום, פשוט תאפשר כניסה במקום לפסול
                true
            }

            matchesGenre && matchesLanguage && distanceOk
        }

        _books.value = filtered
    }


    fun setFilterDistance(distance: Float) {
        _filterDistanceMeters.value = distance
        calculateNearbyBooks()
    }

    fun updateCurrentLocation(location: Location) {
        currentLocation = location
        calculateNearbyBooks()
    }

    fun loadBooks() {
        _isLoading.value = true

        db.collection("books")
            .get()
            .addOnSuccessListener { result ->
                allBooks = result.toObjects(Book::class.java)
                _books.value = allBooks
                _isLoading.value = false
                calculateNearbyBooks()
            }
            .addOnFailureListener {
                allBooks = emptyList()
                _books.value = emptyList()
                _isLoading.value = false
            }
    }

    private fun calculateNearbyBooks() {
        val location = currentLocation ?: return
        val list = allBooks

        val nearby = list.mapNotNull { book ->
            if (book.lat != null && book.lng != null) {
                val bookLocation = Location("").apply {
                    latitude = book.lat
                    longitude = book.lng
                }
                val distance = location.distanceTo(bookLocation)
                Pair(book, distance)
            } else null
        }.sortedBy { it.second }
            .filter { it.second <= (_filterDistanceMeters.value ?: 10f) }

        _nearbyBooks.value = nearby
    }
}
