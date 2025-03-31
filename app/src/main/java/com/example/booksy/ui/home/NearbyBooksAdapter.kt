package com.example.booksy.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.booksy.databinding.ItemNearbyBookBinding
import com.example.booksy.model.Book
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import com.example.booksy.R

class NearbyBooksAdapter(
    private var books: List<Pair<Book, Float>>,
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<NearbyBooksAdapter.BookViewHolder>() {

    inner class BookViewHolder(val binding: ItemNearbyBookBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemNearbyBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val (book, distance) = books[position]

        holder.binding.titleTextView.text = book.title
        holder.binding.authorTextView.text = book.author
        holder.binding.distanceTextView.text = "${distance.toInt()}m"

        if (!book.imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder)
                .transform(RoundedCornersTransformation(24, 0))
                .into(holder.binding.bookImageView)
        } else {
            holder.binding.bookImageView.setImageResource(R.drawable.ic_book_placeholder)
        }


        holder.itemView.setOnClickListener {
            onItemClick(book)
        }
    }

    override fun getItemCount(): Int = books.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateBooks(newBooks: List<Pair<Book, Float>>) {
        books = newBooks
        notifyDataSetChanged()
    }
}
