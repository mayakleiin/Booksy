package com.example.booksy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.booksy.model.Book
import com.example.booksy.model.BookStatus
import com.example.booksy.model.Request
import com.example.booksy.model.RequestStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _book = MutableLiveData<Book>()
    val book: LiveData<Book> get() = _book

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _requestSent = MutableLiveData<Boolean>()
    val requestSent: LiveData<Boolean> get() = _requestSent

    private val _hasPendingRequest = MutableLiveData<Boolean>()
    val hasPendingRequest: LiveData<Boolean> get() = _hasPendingRequest

    fun loadBookDetails(bookId: String) {
        firestore.collection("books")
            .document(bookId)
            .get()
            .addOnSuccessListener { document ->
                val book = document.toObject(Book::class.java)
                book?.let {
                    _book.postValue(it)
                    checkIfRequestExists(it.id)
                } ?: _toastMessage.postValue("Book not found")
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to load book")
            }
    }

    fun requestToBorrow(book: Book) {
        val userId = auth.currentUser?.uid ?: return

        val requestId = firestore.collection("borrowRequests").document().id
        val request = Request(
            id = requestId,
            bookId = book.id,
            fromUserId = userId,
            toUserId = book.ownerId,
            status = RequestStatus.PENDING
        )

        firestore.collection("borrowRequests")
            .document(requestId)
            .set(request)
            .addOnSuccessListener {
                _toastMessage.postValue("Request sent!")
                _requestSent.postValue(true)
                _hasPendingRequest.postValue(true)
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to send request: ${it.message}")
                _requestSent.postValue(false)
            }
    }

    fun checkIfRequestExists(bookId: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("borrowRequests")
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("fromUserId", userId)
            .get()
            .addOnSuccessListener { requests ->
                val activePendingRequest = requests.documents.any { doc ->
                    val status = doc.getString("status")
                    status == RequestStatus.PENDING.name || status == RequestStatus.APPROVED.name
                }
                _hasPendingRequest.postValue(activePendingRequest)
            }
    }

    fun cancelRequest(bookId: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("borrowRequests")
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("fromUserId", userId)
            .get()
            .addOnSuccessListener { requests ->
                for (doc in requests.documents) {
                    firestore.collection("borrowRequests")
                        .document(doc.id)
                        .update("status", RequestStatus.REJECTED.name)
                }
                _toastMessage.postValue("Request cancelled")
                _hasPendingRequest.postValue(false)
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to cancel request")
            }
    }

    fun returnBook(bookId: String) {
        firestore.collection("books")
            .document(bookId)
            .update("status", BookStatus.AVAILABLE.name)
            .addOnSuccessListener {
                _toastMessage.postValue("Book returned successfully")
                loadBookDetails(bookId) // רענון!
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to return book")
            }
    }

}


