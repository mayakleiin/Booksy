package com.example.booksy.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.booksy.R
import com.example.booksy.databinding.ItemBookBinding
import com.example.booksy.model.Book
import com.example.booksy.model.BookStatus

class BookAdapter(
    private val onItemClick: (Book) -> Unit,
    private val onEditClick: ((Book) -> Unit)? = null
) : PagingDataAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

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

            Glide.with(binding.root.context)
                .load(if (book.imageUrl.isNotEmpty()) book.imageUrl else R.drawable.ic_book_placeholder)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder)
                .into(binding.bookImage)

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

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        if (book != null) {
            holder.bind(book)
        }
    }
}

class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem == newItem
}
