package com.example.booksy.viewmodel

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.*
import com.example.booksy.data.FirestoreBookPagingSource
import com.example.booksy.local.AppDatabase
import com.example.booksy.local.entity.BookEntity
import com.example.booksy.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class UserProfileViewModel(private val context: Context) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _requestedBooks = MutableLiveData<List<RequestedBook>>()
    val requestedBooks: LiveData<List<RequestedBook>> get() = _requestedBooks

    private val _incomingRequests = MutableLiveData<List<RequestedBook>>()
    val incomingRequests: LiveData<List<RequestedBook>> get() = _incomingRequests

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun setIsLoading(value: Boolean) {
        _isLoading.postValue(value)
    }

    fun updateUserProfile(newName: String, newImageUrl: String?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>("name" to newName)
        newImageUrl?.let { updates["imageUrl"] = it }

        setIsLoading(true)
        db.collection("users")
            .document(uid)
            .update(updates)
            .addOnSuccessListener {
                setIsLoading(false)
                loadCurrentUser()
                _toastMessage.value = "Profile updated successfully"
            }
            .addOnFailureListener {
                setIsLoading(false)
                _toastMessage.value = "Failed to update profile"
            }
    }

    fun approveRequest(requestedBook: RequestedBook, onComplete: () -> Unit = {}) {
        setIsLoading(true)
        db.collection("borrowRequests")
            .document(requestedBook.request.id)
            .update("status", RequestStatus.APPROVED.name)
            .addOnSuccessListener {
                db.collection("books")
                    .document(requestedBook.book.id)
                    .update("status", BookStatus.BORROWED.name)
                    .addOnSuccessListener {
                        _toastMessage.postValue("Request approved and book marked as borrowed!")
                        loadIncomingRequests()
                        setIsLoading(false)
                        onComplete()
                    }
                    .addOnFailureListener {
                        _toastMessage.postValue("Request approved, but failed to update book status.")
                        loadIncomingRequests()
                        setIsLoading(false)
                        onComplete()
                    }
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to approve request.")
                setIsLoading(false)
                onComplete()
            }
    }

    fun rejectRequest(requestedBook: RequestedBook, onComplete: () -> Unit = {}) {
        setIsLoading(true)
        db.collection("borrowRequests")
            .document(requestedBook.request.id)
            .update("status", RequestStatus.REJECTED.name)
            .addOnSuccessListener {
                _toastMessage.postValue("Request rejected.")
                loadIncomingRequests()
                setIsLoading(false)
                onComplete()
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to reject request.")
                setIsLoading(false)
                onComplete()
            }
    }

    fun cancelRequest(requestedBook: RequestedBook, onComplete: () -> Unit = {}) {
        setIsLoading(true)
        db.collection("borrowRequests")
            .document(requestedBook.request.id)
            .update("status", RequestStatus.REJECTED.name)
            .addOnSuccessListener {
                _toastMessage.postValue("Request cancelled.")
                loadRequestedBooks()
                setIsLoading(false)
                onComplete()
            }
            .addOnFailureListener {
                _toastMessage.postValue("Failed to cancel request.")
                setIsLoading(false)
                onComplete()
            }
    }
    fun loadCurrentUser() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        setIsLoading(true)
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                setIsLoading(false)
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    _user.postValue(user)
                } else {
                    _toastMessage.postValue("User not found.")
                }
            }
            .addOnFailureListener {
                setIsLoading(false)
                _toastMessage.postValue("Failed to load user: ${it.message}")
            }
    }

    fun loadRequestedBooks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        setIsLoading(true)

        db.collection("borrowRequests")
            .whereEqualTo("fromUserId", userId)
            .get()
            .addOnSuccessListener { requestDocs ->
                val requests = requestDocs.toObjects(Request::class.java)
                if (requests.isEmpty()) {
                    setIsLoading(false)
                    _requestedBooks.postValue(emptyList())
                    return@addOnSuccessListener
                }

                val bookIds = requests.map { it.bookId }
                db.collection("books").whereIn("id", bookIds).get()
                    .addOnSuccessListener { bookDocs ->
                        setIsLoading(false)
                        val books = bookDocs.toObjects(Book::class.java)
                        val combined = books.mapNotNull { book ->
                            requests.find { it.bookId == book.id }?.let { request ->
                                RequestedBook(book, request)
                            }
                        }
                        _requestedBooks.postValue(combined)
                    }
                    .addOnFailureListener {
                        setIsLoading(false)
                        _toastMessage.postValue("Failed to load requested books")
                    }
            }
            .addOnFailureListener {
                setIsLoading(false)
                _toastMessage.postValue("Failed to load requests: ${it.message}")
            }
    }

    fun loadIncomingRequests() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        setIsLoading(true)

        db.collection("borrowRequests")
            .whereEqualTo("toUserId", userId)
            .get()
            .addOnSuccessListener { requestDocs ->
                val requests = requestDocs.toObjects(Request::class.java)
                if (requests.isEmpty()) {
                    setIsLoading(false)
                    _incomingRequests.postValue(emptyList())
                    return@addOnSuccessListener
                }

                val bookIds = requests.map { it.bookId }
                db.collection("books").whereIn("id", bookIds).get()
                    .addOnSuccessListener { bookDocs ->
                        setIsLoading(false)
                        val books = bookDocs.toObjects(Book::class.java)
                        val combined = books.mapNotNull { book ->
                            requests.find { it.bookId == book.id }?.let { request ->
                                RequestedBook(book, request)
                            }
                        }
                        _incomingRequests.postValue(combined)
                    }
                    .addOnFailureListener {
                        setIsLoading(false)
                        _toastMessage.postValue("Failed to load incoming books")
                    }
            }
            .addOnFailureListener {
                setIsLoading(false)
                _toastMessage.postValue("Failed to load incoming requests: ${it.message}")
            }
    }

    // --- Paging & Local Caching ---

    private val _refreshTrigger = MutableLiveData(Unit)

    val pagedUserBooks: Flow<PagingData<Book>> = _refreshTrigger.asFlow().flatMapLatest {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@flatMapLatest flowOf()
        val query: Query = db.collection("books")
            .whereEqualTo("ownerId", userId)

        Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = { FirestoreBookPagingSource(query) }
        ).flow
    }

    fun refreshPagedBooks() {
        _refreshTrigger.value = Unit
    }

    private val bookDao = AppDatabase.getDatabase(context).bookDao()

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
}