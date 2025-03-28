package com.example.booksy.ui.booklist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.databinding.FragmentBookListBinding
import com.example.booksy.model.Book
import com.example.booksy.ui.home.BookAdapter
import com.example.booksy.viewmodel.HomeViewModel

class BookListFragment : Fragment() {

    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var adapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = BookAdapter(emptyList()) { selectedBook ->
            val action = BookListFragmentDirections.actionBookListFragmentToBookDetailFragment(selectedBook.id)
            findNavController().navigate(action)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.books.observe(viewLifecycleOwner) { bookList ->
            adapter.updateBooks(bookList)
        }

        viewModel.loadBooks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
