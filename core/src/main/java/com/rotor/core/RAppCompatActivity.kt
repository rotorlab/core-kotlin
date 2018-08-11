package com.rotor.core

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.rotor.core.interfaces.RScreen

abstract class RAppCompatActivity: AppCompatActivity(), RScreen {

    private var active = false
    private lateinit var map: HashMap<String, Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        map = java.util.HashMap()
        Rotor.screens().add(this)
    }

    override fun onResume() {
        super.onResume()
        active = true
        Rotor.onResume()
    }

    override fun onPause() {
        Rotor.onPause()
        active = false
        super.onPause()
    }

    override fun onDestroy() {
        Rotor.screens().remove(this)
        super.onDestroy()
    }

    override fun isActive(): Boolean {
        return active
    }

    override fun addPath(path: String, obj: Any): Boolean {
        if (!map.contains(path)) {
            map.put(path, obj)
            return true;
        } else {
            return false
        }
    }

    override fun removePath(path: String) : Boolean {
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

    override fun holders(): java.util.HashMap<String, Any> {
        return map
    }
}