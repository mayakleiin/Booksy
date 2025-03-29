package com.example.booksy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.databinding.FragmentIncomingRequestsBinding
import com.example.booksy.model.RequestStatus
import com.example.booksy.model.RequestedBook
import com.example.booksy.viewmodel.UserProfileViewModel
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.fragment.findNavController
import com.example.booksy.R
import com.google.firebase.auth.FirebaseAuth

class IncomingRequestsFragment : Fragment() {
    private var _binding: FragmentIncomingRequestsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UserProfileViewModel
    private lateinit var adapter: RequestedBookAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

        adapter = RequestedBookAdapter(
            requestedBooks = emptyList(),
            isIncomingRequest = true,
            onActionClick = { requestedBook, newStatus ->
                updateRequestStatus(requestedBook, newStatus)
            }
        )

        binding.incomingRequestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.incomingRequestsRecyclerView.adapter = adapter

        viewModel.incomingRequests.observe(viewLifecycleOwner) {
            adapter.updateData(it)
            binding.emptyIncomingRequestsMessage.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        viewModel.loadIncomingRequests()
    }

    private fun updateRequestStatus(requestedBook: RequestedBook, newStatus: RequestStatus) {
        val requestId = requestedBook.request.id
        val db = FirebaseFirestore.getInstance()
        db.collection("requests")
            .document(requestId)
            .update("status", newStatus.name)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Request ${newStatus.name}", Toast.LENGTH_SHORT).show()
                viewModel.loadIncomingRequests()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update request", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Additional fragments should follow a similar cleanup strategy
// Let me know if you want me to continue and clean the rest (MyRequests, UserBooks, UserProfile)!
