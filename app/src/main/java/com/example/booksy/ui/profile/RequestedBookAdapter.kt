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

    // Map to track processing state per request ID
    private val processingMap = mutableMapOf<String, Boolean>()

    inner class RequestedBookViewHolder(private val binding: ItemRequestedBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RequestedBook) {
            val book = item.book
            val request = item.request
            val isProcessing = processingMap[request.id] == true
            val isPending = request.status == RequestStatus.PENDING

            binding.bookTitle.text = book.title
            binding.bookAuthor.text = book.author
            binding.requestStatus.text = request.status.name

            binding.bookImage.load(book.imageUrl.ifEmpty { R.drawable.ic_book_placeholder })

            if (isIncomingRequest) {
                // Approve (cancelButton)
                binding.cancelButton.text = if (isProcessing) "" else binding.root.context.getString(R.string.approve)
                binding.cancelButton.isEnabled = isPending && !isProcessing
                binding.cancelButton.setOnClickListener {
                    disableButtons(request.id)
                    onActionClick(item, RequestStatus.APPROVED)
                }

                // Reject
                binding.rejectButton.visibility = View.VISIBLE
                binding.rejectButton.text = if (isProcessing) "" else binding.root.context.getString(R.string.reject)
                binding.rejectButton.isEnabled = isPending && !isProcessing
                binding.rejectButton.setOnClickListener {
                    disableButtons(request.id)
                    onActionClick(item, RequestStatus.REJECTED)
                }

            } else {
                // Cancel
                binding.cancelButton.text = if (isProcessing) "" else binding.root.context.getString(R.string.cancel)
                binding.cancelButton.isEnabled = isPending && !isProcessing
                binding.cancelButton.setOnClickListener {
                    disableButtons(request.id)
                    onActionClick(item, RequestStatus.REJECTED)
                }

                binding.rejectButton.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onBookClick(item)
            }
        }


        private fun disableButtons(requestId: String) {
            processingMap[requestId] = true
            notifyItemChanged(requestedBooks.indexOfFirst { it.request.id == requestId })
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
        processingMap.clear() // Reset all flags when new data arrives
        notifyDataSetChanged()
    }
}
