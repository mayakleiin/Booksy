package com.example.booksy.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.booksy.databinding.ItemNearbyBookBinding
import com.example.booksy.model.Book

class NearbyBooksAdapter(private var books: List<Pair<Book, Float>>) :
    RecyclerView.Adapter<NearbyBooksAdapter.BookViewHolder>() {

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
    }

    override fun getItemCount(): Int = books.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateBooks(newBooks: List<Pair<Book, Float>>) {
        books = newBooks
        notifyDataSetChanged()
    }
}
