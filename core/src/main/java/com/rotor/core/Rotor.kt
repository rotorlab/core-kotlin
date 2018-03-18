package com.rotor.core

import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.rotor.core.interfaces.InternalServiceListener
import com.rotor.core.interfaces.StatusListener
import com.google.gson.Gson
import com.rotor.core.RotorService.Companion.PREF_CONFIG
import com.rotor.core.RotorService.Companion.PREF_ID
import com.rotor.core.interfaces.BuilderFace
import org.json.JSONObject

/**
 * Created by efraespada on 11/03/2018.
 */

class Rotor {

    companion object {

        private val TAG = Rotor::class.java.simpleName

        var context: Context? = null
        @JvmStatic var id: String ? = null
        @JvmStatic var urlServer: String ? = null
        @JvmStatic var urlRedis: String ? = null
        lateinit var statusListener: StatusListener

        var rotorService: RotorService? = null
        private var isServiceBound: Boolean? = null

        var gson: Gson? = null
        var debug: Boolean? = null
        var initializing: Boolean? = null
        var builders: HashMap<Builder, BuilderFace> ? = null

        @JvmStatic val serviceConnection: ServiceConnection = object : ServiceConnection {

            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                if (service is RotorService.FBinder) {
                    rotorService = service.service
                    rotorService?.sc = this
                    rotorService?.listener = object : InternalServiceListener {

                        override fun connected() {
                            if (initializing!!) {
                                initializing = false
                                statusListener.connected()
                            }
                        }

                        override fun reconnecting() {
                            statusListener.reconnecting()
                        }
                    }
                    if (initializing!!) {
                        rotorService?.startService()
                    }
                    if (debug!!) Log.e(TAG, "instanced service")
                }
            }

            override fun onServiceDisconnected(className: ComponentName) {
                if (className.className == RotorService::class.java.name) {
                    rotorService?.listener = null
                    rotorService = null
                }
                if (debug!!) Log.e(TAG, "disconnected")
            }
        }

        @JvmStatic fun initialize(context: Context, urlServer: String, redisServer: String, statusListener: StatusListener) {
            Companion.context = context
            Companion.urlServer = urlServer
            Companion.urlRedis = redisServer
            Companion.statusListener = statusListener
            if (Companion.builders == null) {
                Companion.builders = HashMap<Builder, BuilderFace>()
            }
            Companion.debug = false
            Companion.gson = Gson()
            val shared = context.getSharedPreferences(PREF_CONFIG, MODE_PRIVATE)
            Companion.id = shared.getString(PREF_ID, null)
            if (Companion.id == null) {
                Companion.id = generateNewId()
            }

            Companion.initializing = true

            start()
        }

        private fun generateNewId(): String {
            val id = Settings.Secure.getString(context!!.getContentResolver(), Settings.Secure.ANDROID_ID)
            val shared = context!!.getSharedPreferences(PREF_CONFIG, MODE_PRIVATE).edit()
            shared.putString(PREF_ID, id)
            shared.apply()
            return id
        }

        @JvmStatic fun stop() {
            if (isServiceBound != null && isServiceBound!! && rotorService != null && rotorService!!.getServiceConnection() != null) {
                rotorService!!.stopService()
                try {
                    context!!.unbindService(rotorService!!.getServiceConnection())
                } catch (e: IllegalArgumentException) {
                    // nothing to do here
                }

                if (debug!!) Log.e(TAG, "unbound")
                context!!.stopService(Intent(context, RotorService::class.java))
                isServiceBound = false
            }
        }

        private fun start() {
            if (isServiceBound == null || !isServiceBound!!) {
                val i = Intent(context, RotorService::class.java)
                context!!.startService(i)
                context!!.bindService(i, getServiceConnection(RotorService())!!, Context.BIND_AUTO_CREATE)
                isServiceBound = true
            }
        }

        @JvmStatic fun onResume() {
            start()
        }

        @JvmStatic fun onPause() {
            if (rotorService != null && isServiceBound != null && isServiceBound!!) {
                context!!.unbindService(rotorService!!.getServiceConnection())
                isServiceBound = false
            }
        }


        @JvmStatic private fun getServiceConnection(obj: Any): ServiceConnection? {
            return if (obj is RotorService) {
                serviceConnection
            } else {
                null
            }
        }

        @JvmStatic fun onMessageReceived(jsonObject: JSONObject) {
            if (Companion.builders != null) {
                for (face in Companion.builders!!.values) {
                    face.onMessageReceived(jsonObject)
                }
            }
        }

        @JvmStatic fun prepare(type: Builder, face: BuilderFace) {
            if (Companion.builders != null) {
                Companion.builders!![type] = face
            }
        }

        @JvmStatic fun debug(debug: Boolean) {
            Companion.debug = debug
        }

    }


}