package com.example.booksy.ui.details

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.booksy.R
import com.example.booksy.databinding.FragmentBookDetailBinding
import com.example.booksy.model.Book
import com.example.booksy.model.BookStatus
import com.example.booksy.viewmodel.BookDetailViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BookDetailViewModel
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[BookDetailViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val bookId = arguments?.getString("bookId")
        if (bookId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.toast_book_not_found), Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        viewModel.loadBookDetails(bookId)

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.book.observe(viewLifecycleOwner) { book ->
            updateBookUI(book)
            loadOwnerInfo(book.ownerId)

            val userId = currentUser.uid
            if (book.ownerId == userId) {
                setupOwnerUI(book)
            } else {
                setupBorrowerUI(book)
            }

            if (book.lat != null && book.lng != null) {
                binding.openMapButton.visibility = View.VISIBLE
                binding.openMapButton.setOnClickListener {
                    openMap(book.lat, book.lng)
                }
            } else {
                binding.openMapButton.visibility = View.GONE
            }
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.hasPendingRequest.observe(viewLifecycleOwner) { hasPendingRequest ->
            if (hasPendingRequest) {
                binding.borrowButton.text = getString(R.string.request_sent)
                binding.borrowButton.isEnabled = false
                binding.borrowButton.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                )
            } else {
                binding.borrowButton.text = getString(R.string.request_to_borrow)
                binding.borrowButton.isEnabled = true
                binding.borrowButton.setBackgroundResource(R.color.white)
            }
        }
    }

    private fun updateBookUI(book: Book) {
        binding.title.text = book.title
        binding.author.text = book.author
        binding.pages.text = book.pages.toString()
        binding.language.text = book.languages.joinToString(", ") { it.name }
        binding.about.text = book.description
        binding.bookGenre.text = book.genres.joinToString(", ") { it.name }

        Picasso.get()
            .load(book.imageUrl)
            .placeholder(R.drawable.ic_book_placeholder)
            .error(R.drawable.ic_book_placeholder)
            .into(binding.bookCover)
    }

    private fun loadOwnerInfo(ownerId: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(ownerId)
            .get()
            .addOnSuccessListener { document ->
                val ownerName = document.getString("name") ?: "Unknown Owner"
                val ownerImageUrl = document.getString("imageUrl")

                binding.ownerName.text = ownerName

                if (!ownerImageUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(ownerImageUrl)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(binding.ownerImage)
                }
            }
    }

    private fun openMap(lat: Double, lng: Double) {
        val uri = "geo:$lat,$lng?q=$lat,$lng(Book Location)".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), getString(R.string.toast_maps_not_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupOwnerUI(book: Book) {
        val currentUserId = auth.currentUser?.uid

        binding.ownerActionsLayout.visibility = View.VISIBLE
        binding.borrowButton.visibility = View.GONE

        binding.editButton.setOnClickListener {
            val action = BookDetailFragmentDirections.actionBookDetailFragmentToAddBookFragment(book)
            findNavController().navigate(action)
        }

        binding.deleteButton.setOnClickListener {
            FirebaseFirestore.getInstance()
                .collection("books").document(book.id).delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), getString(R.string.toast_book_deleted), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), getString(R.string.toast_delete_failed), Toast.LENGTH_SHORT).show()
                }
        }

        if (book.status == BookStatus.BORROWED && book.ownerId == currentUserId) {
            binding.returnButton.visibility = View.VISIBLE
            binding.returnButton.setOnClickListener {
                viewModel.returnBook(book.id)
            }
        } else {
            binding.returnButton.visibility = View.GONE
        }
    }

    private fun setupBorrowerUI(book: Book) {
        val userId = auth.uid ?: return

        binding.ownerActionsLayout.visibility = View.GONE
        binding.borrowButton.visibility = View.VISIBLE

        if (book.status == BookStatus.BORROWED) {
            binding.borrowButton.text = getString(R.string.book_already_borrowed)
            binding.borrowButton.isEnabled = false
            binding.borrowButton.setBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            )
        } else {
            binding.borrowButton.setOnClickListener {
                viewModel.requestToBorrow(book)
            }
        }

        viewModel.checkIfRequestExists(book.id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
