package com.example.booksy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.R
import com.example.booksy.databinding.FragmentUserBooksBinding
import com.example.booksy.ui.home.BookAdapter
import com.example.booksy.viewmodel.UserProfileViewModel
import com.example.booksy.viewmodel.UserProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.paging.LoadState

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
        val factory = UserProfileViewModelFactory(requireContext())
        viewModel = ViewModelProvider(requireActivity(), factory)[UserProfileViewModel::class.java]
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
            onItemClick = { book ->
                findNavController().navigate(
                    R.id.action_global_bookDetailFragment,
                    Bundle().apply { putString("bookId", book.id) }
                )
            },
            onEditClick = { book ->
                findNavController().navigate(
                    R.id.action_global_addBookFragment,
                    Bundle().apply { putParcelable("bookToEdit", book) }
                )
            }
        )

        binding.userBooksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.userBooksRecyclerView.adapter = adapter

        adapter.addLoadStateListener { loadState ->
            val isListEmpty = adapter.itemCount == 0 &&
                    loadState.refresh is LoadState.NotLoading &&
                    loadState.append is LoadState.NotLoading &&
                    loadState.source.refresh is LoadState.NotLoading

            binding.emptyMessage.visibility = if (isListEmpty) View.VISIBLE else View.GONE
        }

        lifecycleScope.launch {
            viewModel.pagedUserBooks.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }

        binding.addBookButton.setOnClickListener {
            findNavController().navigate(R.id.addBookFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
