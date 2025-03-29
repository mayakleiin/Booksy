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
import com.example.booksy.databinding.FragmentMyRequestsBinding
import com.example.booksy.model.RequestedBook
import com.example.booksy.viewmodel.UserProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyRequestsFragment : Fragment() {

    private var _binding: FragmentMyRequestsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UserProfileViewModel
    private lateinit var adapter: RequestedBookAdapter
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyRequestsBinding.inflate(inflater, container, false)
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
            isIncomingRequest = false,
            onActionClick = { requestedBook, _ ->
                cancelRequest(requestedBook)
            }
        )

        binding.myRequestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myRequestsRecyclerView.adapter = adapter

        viewModel.requestedBooks.observe(viewLifecycleOwner) {
            adapter.updateData(it)
            binding.emptyMyRequestsMessage.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        viewModel.loadRequestedBooks()
    }

    private fun cancelRequest(requestedBook: RequestedBook) {
        viewModel.setIsLoading(true)
        val requestId = requestedBook.request.id
        FirebaseFirestore.getInstance()
            .collection("requests")
            .document(requestId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Request canceled", Toast.LENGTH_SHORT).show()
                viewModel.loadRequestedBooks()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to cancel request", Toast.LENGTH_SHORT).show()
                viewModel.setIsLoading(false)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
