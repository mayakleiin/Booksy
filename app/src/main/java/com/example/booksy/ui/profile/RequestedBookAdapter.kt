package com.example.booksy.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.booksy.databinding.ItemRequestedBookBinding
import com.example.booksy.model.RequestStatus
import com.example.booksy.model.RequestedBook
import com.example.booksy.R

class RequestedBookAdapter(
    private var requestedBooks: List<RequestedBook>,
    private val onActionClick: (RequestedBook, RequestStatus) -> Unit,
    private val isIncomingRequest: Boolean = false,
    private val onBookClick: (RequestedBook) -> Unit
) : RecyclerView.Adapter<RequestedBookAdapter.RequestedBookViewHolder>() {

    private val processingMap = mutableMapOf<String, Boolean>()

    inner class RequestedBookViewHolder(private val binding: ItemRequestedBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RequestedBook) {
            val book = item.book
            val request = item.request

            binding.bookTitle.text = book.title
            binding.bookAuthor.text = book.author
            binding.requestStatus.text = request.status.name
            binding.bookImage.load(book.imageUrl.ifEmpty { R.drawable.ic_book_placeholder })

            if (isIncomingRequest) {

                val isPending = request.status == RequestStatus.PENDING


                binding.cancelButton.text = binding.root.context.getString(R.string.approve)
                binding.cancelButton.isEnabled = isPending
                binding.cancelButton.visibility = if (isPending) View.VISIBLE else View.GONE
                if (isPending) {
                    binding.cancelButton.setOnClickListener {
                        if (!processingMap.getOrDefault(request.id, false)) {
                            processingMap[request.id] = true
                            onActionClick(item, RequestStatus.APPROVED)
                        }
                    }
                }

                // Reject button
                binding.rejectButton.visibility = if (isPending) View.VISIBLE else View.GONE
                binding.rejectButton.text = binding.root.context.getString(R.string.reject)
                binding.rejectButton.isEnabled = isPending
                if (isPending) {
                    binding.rejectButton.setOnClickListener {
                        if (!processingMap.getOrDefault(request.id, false)) {
                            processingMap[request.id] = true
                            onActionClick(item, RequestStatus.REJECTED)
                        }
                    }
                }
            } else {
                // My Requests - אפשר לבטל רק בקשות ב-PENDING
                if (request.status == RequestStatus.PENDING) {
                    binding.cancelButton.apply {
                        visibility = View.VISIBLE
                        text = binding.root.context.getString(R.string.cancel)
                        isEnabled = true
                        setOnClickListener {
                            if (!processingMap.getOrDefault(request.id, false)) {
                                processingMap[request.id] = true
                                onActionClick(item, RequestStatus.REJECTED)
                            }
                        }
                    }
                } else {
                    binding.cancelButton.visibility = View.GONE
                }

                binding.rejectButton.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onBookClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestedBookViewHolder {
        val binding = ItemRequestedBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestedBookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestedBookViewHolder, position: Int) {
        holder.bind(requestedBooks[position])
    }

    override fun getItemCount(): Int = requestedBooks.size

    fun updateData(newList: List<RequestedBook>) {
        requestedBooks = newList
        processingMap.clear()
        notifyDataSetChanged()
    }
}
