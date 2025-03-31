package com.example.booksy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.R
import com.example.booksy.databinding.FragmentIncomingRequestsBinding
import com.example.booksy.model.RequestStatus
import com.example.booksy.model.RequestedBook
import com.example.booksy.viewmodel.UserProfileViewModel
import com.google.firebase.auth.FirebaseAuth

class IncomingRequestsFragment : Fragment() {

    private var _binding: FragmentIncomingRequestsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UserProfileViewModel
    private lateinit var adapter: RequestedBookAdapter
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomingRequestsBinding.inflate(inflater, container, false)
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

        adapter = RequestedBookAdapter(
            requestedBooks = emptyList(),
            isIncomingRequest = true,
            onActionClick = { requestedBook, newStatus ->
                when (newStatus) {
                    RequestStatus.APPROVED -> viewModel.approveRequest(requestedBook)
                    RequestStatus.REJECTED -> viewModel.rejectRequest(requestedBook)
                    else -> {}
                }
            },
            onBookClick = { requestedBook ->
                findNavController().navigate(
                    R.id.action_global_bookDetailFragment,
                    Bundle().apply { putString("bookId", requestedBook.book.id) }
                )
            }
        )

        binding.incomingRequestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.incomingRequestsRecyclerView.adapter = adapter

        viewModel.incomingRequests.observe(viewLifecycleOwner) {
            adapter.updateData(it)
            binding.emptyIncomingRequestsMessage.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), getString(R.string.toast_profile_generic, it), Toast.LENGTH_SHORT).show()
        }

        viewModel.loadIncomingRequests()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
