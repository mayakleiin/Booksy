package com.example.booksy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.example.booksy.databinding.FragmentUserProfileBinding
import com.example.booksy.ui.home.BookAdapter
import com.example.booksy.viewmodel.UserProfileViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.fragment.findNavController
import com.example.booksy.R

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UserProfileViewModel
    private lateinit var bookAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[UserProfileViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.userName.text = user.name
            binding.userImage.load(user.imageUrl)
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.loadCurrentUser()

        val adapter = UserProfilePagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "My Books"
                1 -> "My Requests"
                2 -> "Incoming Requests"
                else -> ""
            }
        }.attach()

        binding.editProfileButton.setOnClickListener {
            val dialogFragment = EditProfileDialogFragment()
            dialogFragment.show(childFragmentManager, "EditProfileDialog")
        }

        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_userProfileFragment_to_loginFragment)
        }
    }
}
