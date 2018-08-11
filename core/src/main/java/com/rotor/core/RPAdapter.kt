package com.rotor.core

import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

class RPAdapter(val pager: RViewPager, fragmentManager: FragmentManager): FragmentStatePagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        return pager.fragments().get(position)
    }

    override fun getCount(): Int {
        return pager.fragments().size
    }

    fun <T> setFragment(clazz: Class<T>, transition: Boolean) {
        val current = pager.currentItem
        var next: Int = -1
        var currentFragment: RFragment ? = null
        var nextFragment: RFragment ? = null
        pager.fragments().forEachIndexed { index, rFragment ->
            if (rFragment::class.java.simpleName.equals(clazz.simpleName)) {
                next = index
                nextFragment = rFragment
            }
            if (index == current) {
                currentFragment = rFragment
            }
        }
        currentFragment?.onPauseFragment()
        if (next > -1) {
            pager.setCurrentItem(next, transition)
        }
        nextFragment?.onResumeFragment()
    }
}