package com.example.booksy.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.booksy.R
import com.example.booksy.databinding.ItemBookBinding
import com.example.booksy.model.Book
import com.example.booksy.model.BookStatus

class BookAdapter(
    private var books: List<Book>,
    private val onItemClick: (Book) -> Unit,
    private val onEditClick: ((Book) -> Unit)? = null
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.bookTitle.text = book.title
            binding.bookAuthor.text = book.author
            binding.bookGenre.text = book.genres.joinToString(", ") { it.name }
            binding.bookLanguage.text = book.languages.joinToString(", ") { it.name }
            binding.bookStatus.text = book.status.name

            val colorRes = if (book.status == BookStatus.AVAILABLE) {
                android.R.color.holo_green_dark
            } else {
                android.R.color.holo_red_dark
            }

            binding.bookStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )

            if (book.imageUrl.isNotEmpty()) {
                binding.bookImage.load(book.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_book_placeholder)
                    error(R.drawable.ic_book_placeholder)
                }
            } else {
                binding.bookImage.setImageResource(R.drawable.ic_book_placeholder)
            }

            binding.root.setOnClickListener {
                onItemClick(book)
            }

            if (onEditClick != null) {
                binding.editButton.visibility = View.VISIBLE
                binding.editButton.setOnClickListener {
                    onEditClick.invoke(book)
                }
            } else {
                binding.editButton.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun getItemCount(): Int = books.size

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }
}
