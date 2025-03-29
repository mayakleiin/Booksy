package com.example.booksy.ui.profile

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class UserProfilePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserBooksFragment()
            1 -> MyRequestsFragment()
            2 -> IncomingRequestsFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
