package com.example.booksy.ui.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.booksy.R
import com.example.booksy.databinding.FragmentBookDetailBinding
import com.example.booksy.viewmodel.BookDetailViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BookDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[BookDetailViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val bookId = arguments?.getString("bookId") ?: return
        viewModel.loadBookDetails(bookId)

        viewModel.book.observe(viewLifecycleOwner) { book ->
            binding.title.text = book.title
            binding.author.text = book.author
            binding.pages.text = book.pages.toString()
            binding.language.text = book.languages.joinToString(", ") { it.name }
            binding.about.text = book.description
            binding.bookGenre.text = book.genres.joinToString(", ") { it.name }

            binding.bookCover.load(book.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_delete)
            }

            val userId = currentUser.uid

            if (book.ownerId == userId) {
                binding.editButton.visibility = View.VISIBLE
                binding.deleteButton.visibility = View.VISIBLE
                binding.borrowButton.visibility = View.GONE

                binding.editButton.setOnClickListener {
                    val action = BookDetailFragmentDirections.actionBookDetailFragmentToAddBookFragment(book)
                    findNavController().navigate(action)
                }

                binding.deleteButton.setOnClickListener {
                    FirebaseFirestore.getInstance()
                        .collection("books").document(book.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Book deleted", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to delete book", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                binding.editButton.visibility = View.GONE
                binding.deleteButton.visibility = View.GONE
                binding.borrowButton.visibility = View.VISIBLE
                binding.borrowButton.setOnClickListener {
                    viewModel.requestToBorrow(book)
                }
            }

            binding.openMapButton.setOnClickListener {
                val lat = book.lat ?: return@setOnClickListener
                val lng = book.lng ?: return@setOnClickListener
                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Book Location)")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Google Maps not found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
