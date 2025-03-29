package com.example.booksy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.booksy.data.remote.RetrofitInstance
import com.example.booksy.model.BookDoc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookSearchViewModel : ViewModel() {

    private val _searchResults = MutableStateFlow<List<BookDoc>>(emptyList())
    val searchResults: StateFlow<List<BookDoc>> = _searchResults

    fun searchBooks(query: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.searchBooks(query)
                _searchResults.value = response.docs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
