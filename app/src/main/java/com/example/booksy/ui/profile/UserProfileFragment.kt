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
import com.example.booksy.R
import com.example.booksy.databinding.FragmentUserProfileBinding
import com.example.booksy.viewmodel.UserProfileViewModel
import com.example.booksy.viewmodel.UserProfileViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.example.booksy.util.CircleTransformation

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UserProfileViewModel
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        val factory = UserProfileViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[UserProfileViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadingOverlay = view.findViewById(R.id.loadingOverlay)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // Show/hide loading overlay
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Display user info
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.userName.text = it.name

                val imageUrl = it.imageUrl
                if (imageUrl.isNullOrBlank()) {
                    binding.userImage.setImageResource(R.drawable.default_profile)
                } else {
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .transform(CircleTransformation())
                        .into(binding.userImage)
                }
            }
        }

        // Show toast messages
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), getString(R.string.toast_profile_generic, message), Toast.LENGTH_SHORT).show()
        }

        // Load user data
        viewModel.loadCurrentUser()

        // Setup tabs
        val adapter = UserProfilePagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_my_books)
                1 -> getString(R.string.tab_my_requests)
                2 -> getString(R.string.tab_incoming_requests)
                else -> ""
            }
        }.attach()

        // Edit profile button (pencil icon)
        binding.editProfileButton.setOnClickListener {
            val dialogFragment = EditProfileDialogFragment()
            dialogFragment.show(childFragmentManager, "EditProfileDialog")
        }

        // Logout buttons
        binding.logoutButton.setOnClickListener { logoutUser() }
        binding.logoutText.setOnClickListener { logoutUser() }

        // Back to home button
        binding.backToHomeButton.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        findNavController().navigate(R.id.action_userProfileFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
