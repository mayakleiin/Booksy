package com.example.booksy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.databinding.FragmentUserBooksBinding
import com.example.booksy.model.Book
import com.example.booksy.ui.home.BookAdapter
import com.example.booksy.viewmodel.UserProfileViewModel

class UserBooksFragment : Fragment() {

    private var _binding: FragmentUserBooksBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UserProfileViewModel
    private lateinit var adapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBooksBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[UserProfileViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = BookAdapter(emptyList()) { book: Book ->
            // פעולה בעת לחיצה על ספר
        }

        binding.userBooksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.userBooksRecyclerView.adapter = adapter

        viewModel.userBooks.observe(viewLifecycleOwner) {
            adapter.updateBooks(it)
        }

        viewModel.loadUserBooks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
