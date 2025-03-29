package com.example.booksy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.R
import com.example.booksy.databinding.FragmentUserBooksBinding
import com.example.booksy.ui.home.BookAdapter
import com.example.booksy.viewmodel.UserProfileViewModel
import com.google.firebase.auth.FirebaseAuth

class UserBooksFragment : Fragment() {

    private var _binding: FragmentUserBooksBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UserProfileViewModel
    private lateinit var adapter: BookAdapter
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBooksBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[UserProfileViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        loadingOverlay = view.findViewById(R.id.loadingOverlay)

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        adapter = BookAdapter(
            books = emptyList(),
            onItemClick = { book ->
                val action = UserBooksFragmentDirections.actionUserBooksFragmentToBookDetailFragment(book.id)
                findNavController().navigate(action)
            },
            onEditClick = { book ->
                val action = UserBooksFragmentDirections.actionUserBooksFragmentToAddBookFragment(book)
                findNavController().navigate(action)
            }
        )

        binding.userBooksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.userBooksRecyclerView.adapter = adapter

        viewModel.userBooks.observe(viewLifecycleOwner) {
            adapter.updateBooks(it)
            binding.emptyMessage.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadUserBooks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
