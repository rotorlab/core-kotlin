package com.rotor.core

import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import com.lambdaworks.redis.RedisClient
import com.lambdaworks.redis.pubsub.RedisPubSubConnection
import com.lambdaworks.redis.pubsub.RedisPubSubListener
import com.rotor.core.Rotor.Companion.PREF_CONFIG
import com.rotor.core.Rotor.Companion.PREF_URL
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class RJobService : JobService() {

    companion object {
        private val TAG = RJobService::class.java.simpleName
    }

    internal var moment: Long = 0
    internal var connectedToRedis: Boolean = false
    internal var client: RedisClient ? = null
    internal var url: String ? = null
    private val EXCEPTION_NO_SERVER_URL = "No URL was defined for Rotor Server"
    internal var connection: RedisPubSubConnection<String, String>? = null

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
            val task = Runnable {
                Rotor.connected()
            }
            Handler(applicationContext.mainLooper).post(task)
        }

        override fun psubscribed(s: String, l: Long) {
            // nothing to do here
        }

        override fun unsubscribed(s: String, l: Long) {
            moment = 0
            connectedToRedis = false
            val task = Runnable { Rotor.notConnected() }
            Handler(applicationContext.mainLooper).post(task)
        }

        override fun punsubscribed(s: String, l: Long) {
            // nothing to do here
        }
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }



    private fun startConnection() {
        if (client == null) {
            if (url == null) {
                url = Rotor.urlRedis
            }
            if (url == null || url?.length == 0) {
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

        if (!connectedToRedis && !NetworkUtil.getConnectivityStatusString(applicationContext).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED)) {
            if (client != null && connection != null) {
                connection?.subscribe(Rotor.id)
            }
        } else if (connectedToRedis && !NetworkUtil.getConnectivityStatusString(applicationContext).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED)) {
            val task = Runnable {
                Rotor.connected()
            }
            Handler(applicationContext.mainLooper).post(task)
        }
    }


    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "on start job: ${params!!.jobId}")
        startConnection()
        val jobs = Rotor.jobs()
        for (job in jobs) {
            job.startJob()
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i(TAG, "on stop job: ${params!!.jobId}")
        stopService()
        val jobs = Rotor.jobs()
        for (job in jobs) {
            job.stopJob()
        }
        return true
    }

    fun stopService() {
        if (connectedToRedis && !NetworkUtil.getConnectivityStatusString(applicationContext).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED)) {
            connection?.unsubscribe(Rotor.id)
        }
    }


}