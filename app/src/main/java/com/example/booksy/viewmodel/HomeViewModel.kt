package com.example.booksy.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.booksy.model.Book
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    private val _nearbyBooks = MutableLiveData<List<Pair<Book, Float>>>()
    val nearbyBooks: LiveData<List<Pair<Book, Float>>> = _nearbyBooks

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentLocation: Location? = null

    private val _filterDistanceMeters = MutableLiveData(2000f)
    val filterDistanceMeters: LiveData<Float> = _filterDistanceMeters

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
                val bookList = result.toObjects(Book::class.java)
                _books.value = bookList
                _isLoading.value = false
                calculateNearbyBooks()
            }
            .addOnFailureListener {
                _books.value = emptyList()
                _isLoading.value = false
            }
    }

    private fun calculateNearbyBooks() {
        val location = currentLocation ?: return
        val list = _books.value ?: return

        val nearby = list.mapNotNull { book ->
            if (book.lat != null && book.lng != null) {
                val bookLocation = Location("").apply {
                    latitude = book.lat!!
                    longitude = book.lng!!
                }
                val distance = location.distanceTo(bookLocation) // in meters
                Pair(book, distance)
            } else null
        }.sortedBy { it.second }
            .filter { it.second <= (_filterDistanceMeters.value ?: 2000f) }


        _nearbyBooks.value = nearby
    }
}
