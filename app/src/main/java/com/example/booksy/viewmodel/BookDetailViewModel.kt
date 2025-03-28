package com.example.booksy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.booksy.model.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _book = MutableLiveData<Book>()
    val book: LiveData<Book> get() = _book

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    fun loadBookDetails(bookId: String) {
        FirebaseFirestore.getInstance()
            .collection("books")
            .document(bookId)
            .get()
            .addOnSuccessListener { document ->
                val book = document.toObject(Book::class.java)
                book?.let {
                    _book.postValue(it)
                } ?: _toastMessage.postValue("Book not found")
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to load book")
            }
    }

    fun requestToBorrow(book: Book) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val request = hashMapOf(
            "bookId" to book.id,
            "ownerId" to book.ownerId,
            "requesterId" to userId,
            "status" to "Pending",
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("borrowRequests")
            .add(request)
            .addOnSuccessListener {
                _toastMessage.postValue("Request sent!")
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to send request")
            }
    }
}
