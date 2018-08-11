package com.rotor.core

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rotor.core.interfaces.RScreen
import java.util.HashMap

abstract class RFragment: Fragment(), RScreen {

    private var active = false
    private var viewed = false
    private var viewCreated = false
    private var shouldCallRemove = false
    private lateinit var map: HashMap<String, Any>

    abstract fun onResumeView()

    abstract fun onPauseView()

    abstract fun onBackPressed()

    abstract fun onCreateRView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View?

    abstract fun onRViewCreated(view: View?, savedInstanceState: Bundle?)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return onCreateRView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewed = true
        map = java.util.HashMap()
        Rotor.screens().add(this)
        viewCreated = true;
        onRViewCreated(view, savedInstanceState)
        if (shouldCallRemove) {
            onResumeFragment()
        }
    }

    fun onResumeFragment() {
        if (!active && viewCreated) {
            active = true
            Rotor.onResume()
            onResumeView()
        } else if (!active && !viewCreated) {
            shouldCallRemove = true;
        }
    }

    fun onPauseFragment() {
        if (active) {
            Rotor.onPause()
            active = false
            onPauseView()
        }
    }

    override fun onDestroyView() {
        Rotor.screens().remove(this)
        viewed = false
        super.onDestroyView()
    }


    override fun isActive(): Boolean {
        return active
    }

    fun viewed(): Boolean {
        return viewed
    }

    override fun addPath(path: String, obj: Any): Boolean {
        if (!map.contains(path)) {
            map.put(path, obj)
            return true;
        } else {
            return false
        }
    }

    override fun removePath(path: String): Boolean {
        if (map.contains(path)) {
            map.remove(path)
            return true;
        } else {
            return false
        }
    }

    override fun hasPath(path: String): Boolean {
        return map.contains(path)
    }

    override fun holders(): HashMap<String, Any> {
        return map
    }

}