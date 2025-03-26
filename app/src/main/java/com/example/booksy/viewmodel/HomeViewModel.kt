package com.example.booksy.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.booksy.model.Book
import kotlinx.coroutines.*
import kotlin.random.Random

class HomeViewModel : ViewModel() {

    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun loadBooks() {
        _isLoading.postValue(true)


        viewModelScope.launch {
            delay(1500)

            val demoBooks = listOf(
                Book(
                    id = "1",
                    title = "Atomic Habits",
                    author = "James Clear",
                    genre = "Self-help",
                    imageUrl = "",
                    lat = 32.109333,
                    lng = 34.855499
                ),
                Book(
                    id = "2",
                    title = "1984",
                    author = "George Orwell",
                    genre = "Fiction",
                    imageUrl = "",
                    lat = 32.0600,
                    lng = 34.7800
                ),
                Book(
                    id = "3",
                    title = "Sapiens",
                    author = "Yuval Noah Harari",
                    genre = "History",
                    imageUrl = "",
                    lat = 32.0661,
                    lng = 34.7778
                )
            )

            _books.postValue(demoBooks)
            _isLoading.postValue(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}
