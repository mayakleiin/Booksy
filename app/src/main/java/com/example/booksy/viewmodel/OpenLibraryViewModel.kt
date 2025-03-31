package com.example.booksy.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.booksy.model.OpenLibraryBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class OpenLibraryViewModel : ViewModel() {

    private val _selectedBook = MutableLiveData<OpenLibraryBook?>()
    val selectedBook: LiveData<OpenLibraryBook?> = _selectedBook

    private val _noResultFound = MutableLiveData<Boolean>()
    val noResultFound: LiveData<Boolean> = _noResultFound

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun searchBook(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _isLoading.postValue(true)
                val url = "https://openlibrary.org/search.json?title=${query.replace(" ", "+")}"
                val connection = URL(url).openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val docs = json.getJSONArray("docs")

                if (docs.length() > 0) {
                    val first = docs.getJSONObject(0)
                    val book = OpenLibraryBook(
                        title = first.optString("title"),
                        author_name = first.optJSONArray("author_name")?.let {
                            0.until(it.length()).map { i -> it.getString(i) }
                        },
                        cover_i = if (first.has("cover_i")) first.getInt("cover_i") else null,
                        number_of_pages_median = if (first.has("number_of_pages_median")) first.getInt("number_of_pages_median") else null,
                        first_sentence = first.optJSONObject("first_sentence")?.let { obj ->
                            obj.keys().asSequence().associateWith { key -> obj.getString(key) }
                        },
                        first_publish_year = first.optInt("first_publish_year"),
                        key = first.optString("key"),
                        publish_place = null,
                        subject = null,
                        isbn = null,
                        publisher = null,
                        language = null,
                        edition_count = null,
                        publish_year = null
                    )
                    _selectedBook.postValue(book)
                    _noResultFound.postValue(false)
                } else {
                    _selectedBook.postValue(null)
                    _noResultFound.postValue(true)
                }
            } catch (e: Exception) {
                _selectedBook.postValue(null)
                _noResultFound.postValue(true)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}