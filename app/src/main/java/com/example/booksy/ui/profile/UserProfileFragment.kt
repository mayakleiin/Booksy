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
import com.bumptech.glide.Glide
import com.example.booksy.R
import com.example.booksy.databinding.FragmentUserProfileBinding
import com.example.booksy.viewmodel.UserProfileViewModel
import com.example.booksy.viewmodel.UserProfileViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

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

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.userName.text = it.name

                Glide.with(this)
                    .load(it.imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.userImage)
            }
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), getString(R.string.toast_profile_generic, message), Toast.LENGTH_SHORT).show()
        }

        viewModel.loadCurrentUser()

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

        binding.editProfileButton.setOnClickListener {
            val dialogFragment = EditProfileDialogFragment()
            dialogFragment.show(childFragmentManager, "EditProfileDialog")
        }

        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_userProfileFragment_to_loginFragment)
        }

        binding.backToHomeButton.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
