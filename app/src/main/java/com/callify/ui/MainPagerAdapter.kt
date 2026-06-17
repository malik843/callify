package com.callify.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * ViewPager2 adapter for [com.callify.MainActivity].
 *
 * Page 0 — [DialPadFragment]:      numeric dial grid
 * Page 1 — [PermissionsFragment]:  permission dashboard
 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2
    override fun createFragment(position: Int): Fragment = when (position) {
        0    -> DialPadFragment()
        else -> PermissionsFragment()
    }
}
