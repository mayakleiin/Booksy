package com.example.booksy.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.booksy.model.Book
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadBooks() {
        _isLoading.value = true

        db.collection("books")
            .get()
            .addOnSuccessListener { result ->
                val bookList = result.toObjects(Book::class.java)
                _books.value = bookList
                _isLoading.value = false
            }
            .addOnFailureListener {
                _books.value = emptyList()
                _isLoading.value = false
            }
    }
}

