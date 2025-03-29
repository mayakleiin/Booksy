package com.example.booksy.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.booksy.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileViewModel : ViewModel() {

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _userBooks = MutableLiveData<List<Book>>()
    val userBooks: LiveData<List<Book>> get() = _userBooks

    private val _requestedBooks = MutableLiveData<List<RequestedBook>>()
    val requestedBooks: LiveData<List<RequestedBook>> get() = _requestedBooks

    private val _incomingRequests = MutableLiveData<List<RequestedBook>>()
    val incomingRequests: LiveData<List<RequestedBook>> get() = _incomingRequests

    fun loadCurrentUser() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    _user.postValue(user)
                } else {
                    _toastMessage.postValue("User not found.")
                }
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to load user: ${it.message}")
            }
    }

    fun loadUserBooks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("books")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->
                val books = result.toObjects(Book::class.java)
                _userBooks.postValue(books)
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to load user's books: ${it.message}")
            }
    }

    fun loadRequestedBooks() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("requests")
            .whereEqualTo("fromUserId", currentUserId)
            .get()
            .addOnSuccessListener { requestDocs ->
                val requests = requestDocs.toObjects(Request::class.java)
                if (requests.isEmpty()) {
                    _requestedBooks.postValue(emptyList())
                    return@addOnSuccessListener
                }

                val bookIds = requests.map { it.bookId }

                db.collection("books")
                    .whereIn("id", bookIds)
                    .get()
                    .addOnSuccessListener { bookDocs ->
                        val books = bookDocs.toObjects(Book::class.java)
                        val combined = books.mapNotNull { book ->
                            val matchingRequest = requests.find { it.bookId == book.id }
                            matchingRequest?.let { request ->
                                RequestedBook(book, request)
                            }
                        }
                        _requestedBooks.postValue(combined)
                    }
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to load requested books: ${it.message}")
            }
    }

    fun loadIncomingRequests() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("requests")
            .whereEqualTo("toUserId", currentUserId)
            .get()
            .addOnSuccessListener { requestDocs ->
                val requests = requestDocs.toObjects(Request::class.java)
                if (requests.isEmpty()) {
                    _incomingRequests.postValue(emptyList())
                    return@addOnSuccessListener
                }

                val bookIds = requests.map { it.bookId }

                db.collection("books")
                    .whereIn("id", bookIds)
                    .get()
                    .addOnSuccessListener { bookDocs ->
                        val books = bookDocs.toObjects(Book::class.java)
                        val combined = books.mapNotNull { book ->
                            val matchingRequest = requests.find { it.bookId == book.id }
                            matchingRequest?.let { request ->
                                RequestedBook(book, request)
                            }
                        }
                        _incomingRequests.postValue(combined)
                    }
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to load incoming requests: ${it.message}")
            }
    }

    fun updateUserProfile(newName: String, newImageUrl: String?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>("name" to newName)
        newImageUrl?.let { updates["imageUrl"] = it }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(updates)
            .addOnSuccessListener {
                loadCurrentUser()
                _toastMessage.value = "Profile updated successfully"
            }
            .addOnFailureListener {
                _toastMessage.value = "Failed to update profile"
            }
    }
}
