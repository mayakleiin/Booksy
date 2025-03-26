package com.example.booksy.viewmodel

import androidx.lifecycle.*
import com.example.booksy.model.Book
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadBooks() {
        _isLoading.value = true

        firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                val booksList = result.documents.mapNotNull { doc ->
                    doc.toObject(Book::class.java)
                }
                _books.value = booksList
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false

            }
    }
}
