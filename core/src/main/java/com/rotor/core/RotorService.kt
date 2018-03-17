package com.rotor.core

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.StrictMode
import android.util.Log
import com.rotor.core.interfaces.InternalServiceListener
import com.lambdaworks.redis.RedisClient
import com.lambdaworks.redis.pubsub.RedisPubSubConnection
import com.lambdaworks.redis.pubsub.RedisPubSubListener
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Created by efraespada on 11/03/2018.
 */
class RotorService: Service() {

    companion object {
        internal val PREF_ID = "rotor_id"
        internal val PREF_URL = "rotor_url"
        internal val PREF_CONFIG = "rotor_config"
    }
    private val TAG = RotorService::class.java.simpleName
    private val EXCEPTION_NO_SERVER_URL = "No URL was defined for Rotor Server"
    internal var binder: FBinder = FBinder()
    internal var initialized: Boolean = false
    internal var client: RedisClient ? = null
    internal var moment: Long = 0
    internal var connection: RedisPubSubConnection<String, String> ? = null
    internal var sc: ServiceConnection ? = null
    internal var connectedToRedis: Boolean = false
    var listener: InternalServiceListener? = null

    private val redisPubSubListener = object : RedisPubSubListener<String, String> {
        override fun message(s: String, s2: String) {
            val task = Runnable {
                try {
                    Rotor.onMessageReceived(JSONObject(s2))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            Handler(applicationContext.mainLooper).post(task)
        }

        override fun message(s: String, k1: String, s2: String) {
            // nothing to do here
        }

        override fun subscribed(s: String, l: Long) {
            moment = Date().time
            connectedToRedis = true
            if (listener != null) {
                val task = Runnable { listener!!.connected() }
                Handler(applicationContext.mainLooper).post(task)
            }
        }

        override fun psubscribed(s: String, l: Long) {
            // nothing to do here
        }

        override fun unsubscribed(s: String, l: Long) {
            moment = 0
            connectedToRedis = false
            if (listener != null) {
                val task = Runnable { listener!!.connected() }
                Handler(applicationContext.mainLooper).post(task)
            }
        }

        override fun punsubscribed(s: String, l: Long) {
            // nothing to do here
        }
    }

    override fun onCreate() {
        super.onCreate()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        startConnection()
    }

    private fun startConnection() {
        listener?.reconnecting()
        if (client == null) {
            var url = Rotor.urlRedis
            if (url?.length == 0) {
                val shared = applicationContext.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
                url = shared.getString(PREF_URL, null)
            } else {
                val shared = applicationContext.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE).edit()
                shared.putString(PREF_URL, url)
                shared.apply()
            }

            if (url == null) {
                throw ConnectionException(EXCEPTION_NO_SERVER_URL)
            } else if (NetworkUtil.getConnectivityStatusString(applicationContext).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED)) {
                return
            }

            client = RedisClient.create(url)
            connection = client!!.connectPubSub()
            connection!!.addListener(redisPubSubListener)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "service start")
        startService()
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "service bound")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "service destroyed")
        connection?.removeListener(redisPubSubListener)
        super.onDestroy()
    }

    fun startService() {
        if (!connectedToRedis && !NetworkUtil.getConnectivityStatusString(applicationContext).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED)) {
            initialized = true
            if (client != null && connection != null) {
                connection?.subscribe(Rotor.id)
            }
        } else if (connectedToRedis && !NetworkUtil.getConnectivityStatusString(applicationContext).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED)) {
            if (listener != null) {
                val task = Runnable { listener?.connected() }
                Handler(applicationContext.mainLooper).post(task)
            }

        }
    }

    fun stopService() {
        if (connectedToRedis && !NetworkUtil.getConnectivityStatusString(applicationContext).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED)) {
            connection?.unsubscribe(Rotor.id)
        }
    }


    /*
    fun setServiceConnection(sc: ServiceConnection) {
        Log.d(TAG, "serviceConnection set")
        this.sc = sc
    }


    fun setListener(listener: InternalServiceListener) {
        this@RotorService.listener = listener

        if (initializing) {
            this@RotorService.listener.connected()
        }
    }*/

    fun getMoment(): Long? {
        return moment
    }

    fun getServiceConnection(): ServiceConnection ? {
        return this.sc
    }

    inner class FBinder : Binder() {

        val service: RotorService
            get() = this@RotorService

    }
}