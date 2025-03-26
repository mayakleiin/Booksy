package com.example.booksy.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.booksy.R
import com.example.booksy.databinding.ItemBookBinding
import com.example.booksy.model.Book
import coil.load

class BookAdapter(private var books: List<Book>) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(private val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            binding.bookTitle.text = book.title
            binding.bookAuthor.text = book.author
            binding.bookGenre.text = book.genre
            binding.bookStatus.text = book.status.name

            // loading picture if exist url
            if (book.imageUrl.isNotEmpty()) {
                binding.bookImage.load(book.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_book_placeholder)
                    error(R.drawable.ic_book_placeholder)
                }
            } else {
                binding.bookImage.setImageResource(R.drawable.ic_book_placeholder)
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun getItemCount() = books.size

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    // function for updating the booklist from outside
    fun updateBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }
}
