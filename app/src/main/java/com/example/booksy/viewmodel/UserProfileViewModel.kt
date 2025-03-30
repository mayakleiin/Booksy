package com.example.booksy.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.booksy.data.FirestoreBookPagingSource
import com.example.booksy.local.AppDatabase
import com.example.booksy.local.entity.BookEntity
import com.example.booksy.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class UserProfileViewModel(private val context: Context) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val bookDao = AppDatabase.getDatabase(context).bookDao()

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _userBooks = MutableLiveData<List<Book>>()
    val userBooks: LiveData<List<Book>> get() = _userBooks

    private val _requestedBooks = MutableLiveData<List<RequestedBook>>()
    val requestedBooks: LiveData<List<RequestedBook>> get() = _requestedBooks

    private val _incomingRequests = MutableLiveData<List<RequestedBook>>()
    val incomingRequests: LiveData<List<RequestedBook>> get() = _incomingRequests

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadCurrentUser() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _isLoading.value = true
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                _isLoading.value = false
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    _user.postValue(user)
                } else {
                    _toastMessage.postValue("User not found.")
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
                _toastMessage.postValue("Failed to load user: ${it.message}")
            }
    }

    fun loadUserBooks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _isLoading.value = true
        FirebaseFirestore.getInstance()
            .collection("books")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->
                _isLoading.value = false
                val books = result.toObjects(Book::class.java)
                _userBooks.postValue(books)
            }
            .addOnFailureListener {
                _isLoading.value = false
                _toastMessage.postValue("Failed to load user's books: ${it.message}")
            }
    }

    fun loadRequestedBooks() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _isLoading.value = true
        val db = FirebaseFirestore.getInstance()

        db.collection("borrowRequests")
            .whereEqualTo("fromUserId", currentUserId)
            .get()
            .addOnSuccessListener { requestDocs ->
                val requests = requestDocs.toObjects(Request::class.java)
                if (requests.isEmpty()) {
                    _isLoading.value = false
                    _requestedBooks.postValue(emptyList())
                    return@addOnSuccessListener
                }

                val bookIds = requests.map { it.bookId }

                db.collection("books")
                    .whereIn("id", bookIds)
                    .get()
                    .addOnSuccessListener { bookDocs ->
                        _isLoading.value = false
                        val books = bookDocs.toObjects(Book::class.java)
                        val combined = books.mapNotNull { book ->
                            val matchingRequest = requests.find { it.bookId == book.id }
                            matchingRequest?.let { request ->
                                RequestedBook(book, request)
                            }
                        }
                        _requestedBooks.postValue(combined)
                    }
                    .addOnFailureListener {
                        _isLoading.value = false
                        _toastMessage.postValue("Failed to load requested books")
                    }
            }
            .addOnFailureListener {
                _isLoading.value = false
                _toastMessage.postValue("Failed to load requests: ${it.message}")
            }
    }

    fun loadIncomingRequests() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _isLoading.value = true
        val db = FirebaseFirestore.getInstance()

        db.collection("borrowRequests")
            .whereEqualTo("toUserId", currentUserId)
            .get()
            .addOnSuccessListener { requestDocs ->
                val requests = requestDocs.toObjects(Request::class.java)
                if (requests.isEmpty()) {
                    _isLoading.value = false
                    _incomingRequests.postValue(emptyList())
                    return@addOnSuccessListener
                }

                val bookIds = requests.map { it.bookId }

                db.collection("books")
                    .whereIn("id", bookIds)
                    .get()
                    .addOnSuccessListener { bookDocs ->
                        _isLoading.value = false
                        val books = bookDocs.toObjects(Book::class.java)
                        val combined = books.mapNotNull { book ->
                            val matchingRequest = requests.find { it.bookId == book.id }
                            matchingRequest?.let { request ->
                                RequestedBook(book, request)
                            }
                        }
                        _incomingRequests.postValue(combined)
                    }
                    .addOnFailureListener {
                        _isLoading.value = false
                        _toastMessage.postValue("Failed to load incoming books")
                    }
            }
            .addOnFailureListener {
                _isLoading.value = false
                _toastMessage.postValue("Failed to load incoming requests: ${it.message}")
            }
    }

    fun updateUserProfile(newName: String, newImageUrl: String?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>("name" to newName)
        newImageUrl?.let { updates["imageUrl"] = it }

        _isLoading.value = true
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(updates)
            .addOnSuccessListener {
                _isLoading.value = false
                loadCurrentUser()
                _toastMessage.value = "Profile updated successfully"
            }
            .addOnFailureListener {
                _isLoading.value = false
                _toastMessage.value = "Failed to update profile"
            }
    }

    fun setIsLoading(value: Boolean) {
        _isLoading.value = value
    }

    fun cacheUserBooksLocally(books: List<Book>) {
        viewModelScope.launch {
            val entities = books.map {
                BookEntity(
                    id = it.id,
                    title = it.title,
                    author = it.author,
                    genres = it.genres.joinToString(",") { g -> g.name },
                    languages = it.languages.joinToString(",") { l -> l.name },
                    pages = it.pages,
                    description = it.description,
                    imageUrl = it.imageUrl,
                    status = it.status.name,
                    ownerId = it.ownerId,
                    lat = it.lat,
                    lng = it.lng
                )
            }
            bookDao.deleteAllBooks()
            bookDao.insertBooks(entities)
        }
    }

    fun getCachedUserBooks(): Flow<List<BookEntity>> = bookDao.getBooksByOwner(
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    )


    fun getPagedUserBooks(): Flow<PagingData<Book>> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = {
                FirestoreBookPagingSource(
                    FirebaseFirestore.getInstance().collection("books")
                        .whereEqualTo("ownerId", "invalid")
                )
            }
        ).flow

        val query: Query = FirebaseFirestore.getInstance()
            .collection("books")
            .whereEqualTo("ownerId", userId)
            .orderBy("title")

        return Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = { FirestoreBookPagingSource(query) }
        ).flow
    }
}
