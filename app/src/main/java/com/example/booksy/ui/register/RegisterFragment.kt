package com.example.booksy.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.booksy.R
import com.example.booksy.databinding.FragmentRegisterBinding
import com.example.booksy.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var authViewModel: AuthViewModel
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        loadingOverlay = view.findViewById(R.id.loadingOverlay)

        authViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        }

        binding.registerButton.setOnClickListener {
            val fullName = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (fullName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.register(email, password,
                    onSuccess = {
                        val uid = authViewModel.getCurrentUserId()
                        if (uid != null) {
                            val userMap = hashMapOf(
                                "name" to fullName,
                                "email" to email,
                                "imageUrl" to ""
                            )
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Welcome!", Toast.LENGTH_SHORT).show()
                                    findNavController().navigate(R.id.action_registerFragment_to_userProfileFragment)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(requireContext(), "Failed saving user info", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), "Register failed: $error", Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goToLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}