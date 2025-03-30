package com.example.booksy.ui.login

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
import com.example.booksy.databinding.FragmentLoginBinding
import com.example.booksy.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var authViewModel: AuthViewModel
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        loadingOverlay = view.findViewById(R.id.loadingOverlay)

        authViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(
                    email = email,
                    password = password,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Welcome!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_userProfileFragment)
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), "Login failed: $error", Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goToRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
