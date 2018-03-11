package com.flamebase.core

import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.flamebase.core.interfaces.InternalServiceListener
import com.flamebase.core.interfaces.StatusListener
import com.google.gson.Gson
import org.json.JSONObject

/**
 * Created by efraespada on 11/03/2018.
 */

class Flamebase {

    companion object {

        private val TAG = Flamebase::class.simpleName

        private var context: Context? = null
        var id: String ? = null
        var urlServer: String ? = null
        var urlRedis: String ? = null
        lateinit var statusListener: StatusListener

        private var flamebaseService: FlamebaseService? = null
        private var isServiceBound: Boolean? = null

        private var gson: Gson? = null
        var debug: Boolean? = null
        var initialized: Boolean? = null

        val serviceConnection: ServiceConnection = object : ServiceConnection {

            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                if (service is FlamebaseService.FBinder) {
                    flamebaseService = service.service
                    flamebaseService?.sc = this
                    flamebaseService?.listener = object : InternalServiceListener {

                        override fun connected() {
                            if (initialized!!) {
                                initialized = false
                                statusListener.connected()
                            }
                        }

                        override fun reconnecting() {
                            statusListener.reconnecting()
                        }
                    }
                    if (debug!!) Log.e(TAG, "instanced service")
                }
            }

            override fun onServiceDisconnected(className: ComponentName) {
                if (className.className == FlamebaseService::class.java.name) {
                    flamebaseService?.listener = null
                    flamebaseService = null
                }
                if (debug!!) Log.e(TAG, "disconnected")
            }
        }

        @JvmStatic fun initialize(context: Context, urlServer: String, redisServer: String, statusListener: StatusListener) {
            Flamebase.context = context
            Flamebase.urlServer = urlServer
            Flamebase.urlRedis = redisServer
            Flamebase.statusListener = statusListener
            Flamebase.debug = false
            Flamebase.gson = Gson()
            val shared = context.getSharedPreferences("flamebase_config", MODE_PRIVATE)
            Flamebase.id = shared.getString("flamebase_id", null)
            if (Flamebase.id == null) {
                Flamebase.id = generateNewId()
            }

            initialized = true

            start()
        }

        private fun generateNewId(): String {
            val id = Settings.Secure.getString(context!!.getContentResolver(), Settings.Secure.ANDROID_ID)
            val shared = context!!.getSharedPreferences("flamebase_config", MODE_PRIVATE).edit()
            shared.putString("flamebase_id", id)
            shared.apply()
            return id
        }

        @JvmStatic fun stop() {
            if (isServiceBound != null && isServiceBound!! && flamebaseService != null && flamebaseService!!.getServiceConnection() != null) {
                flamebaseService!!.stopService()
                try {
                    context!!.unbindService(flamebaseService!!.getServiceConnection())
                } catch (e: IllegalArgumentException) {
                    // nothing to do here
                }

                if (debug!!) Log.e(TAG, "unbound")
                context!!.stopService(Intent(context, FlamebaseService::class.java))
                isServiceBound = false
            }
        }

        private fun start() {
            if (isServiceBound == null || !isServiceBound!!) {
                val i = Intent(context, FlamebaseService::class.java)
                context!!.startService(i)
                context!!.bindService(i, getServiceConnection(FlamebaseService())!!, Context.BIND_AUTO_CREATE)
                isServiceBound = true
            }
        }

        @JvmStatic fun onResume() {
            start()
        }

        @JvmStatic fun onPause() {
            if (flamebaseService != null && isServiceBound != null && isServiceBound!!) {
                context!!.unbindService(flamebaseService!!.getServiceConnection())
                isServiceBound = false
            }
        }


        @JvmStatic private fun getServiceConnection(obj: Any): ServiceConnection? {
            return if (obj is FlamebaseService) {
                serviceConnection
            } else {
                null
            }
        }

        @JvmStatic fun onMessageReceived(jsonObject: JSONObject) {

        }

    }


}