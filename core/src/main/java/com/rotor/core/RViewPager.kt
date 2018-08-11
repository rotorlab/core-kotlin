package com.rotor.core

import android.content.Context
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet

class RViewPager(context: Context, attributes: AttributeSet?): ViewPager(context, attributes) {

    lateinit var adapter: RPAdapter

    val fragments: ArrayList<RFragment> = ArrayList()

    var lastPaused = -1

    constructor(context: Context) : this(context, null) {

    }

    fun init(activity: AppCompatActivity) {
        adapter = RPAdapter(this@RViewPager, activity.supportFragmentManager)
        setAdapter(adapter)
    }

    fun add(fragment: RFragment) : Boolean {
        var found = false
        fragments.forEach {
            if (it::class.java.simpleName.equals(fragment::class.java.simpleName)) {
                found = true
            }
        }
        if (found) {
            return false
        } else {
            fragment.onPauseFragment()
            fragments.add(fragment)
            adapter.notifyDataSetChanged()
            return true
        }
    }

    fun fragments() : ArrayList<RFragment> {
        return fragments
    }

    fun <T> setFragment(clazz: Class<T>) {
        setFragment(clazz, false)
    }

    fun <T> setFragment(clazz: Class<T>, transition: Boolean) {
        adapter.setFragment(clazz, transition)
    }

    fun adapter() : RPAdapter {
        return adapter
    }

    fun onBackPressed() {
        fragments().get(currentItem).onBackPressed()
    }

    fun pauseActiveFragment() {
        if (lastPaused == -1) {
            lastPaused = currentItem
            fragments().get(lastPaused).onPauseFragment()
        }
    }

    fun resumeLastActiveFragment() {
        if (lastPaused != -1) {
            fragments().get(lastPaused).onResumeFragment()
            lastPaused = -1
        }
    }

}