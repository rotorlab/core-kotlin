package com.rotor.core

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.rotor.core.interfaces.RStatus
import com.google.gson.Gson
import com.rotor.core.interfaces.BuilderFace
import com.rotor.core.interfaces.RScreen
import org.json.JSONObject
import java.util.ArrayList

/**
 * Created by efraespada on 11/03/2018.
 */

class Rotor {

    companion object {

        private val TAG = Rotor::class.java.simpleName
        internal val PREF_ID = "rotor_id"
        internal val PREF_URL = "rotor_url"
        internal val PREF_CONFIG = "rotor_config"

        @JvmStatic var context: Context? = null
        @JvmStatic var id: String ? = null
        @JvmStatic var urlServer: String ? = null
        @JvmStatic var urlRedis: String ? = null
        var RStatus: RStatus ? = null
        private var jobId = 0
        private var serviceComponent: ComponentName ? = null

        private val jobs = ArrayList<RJob>()

        private val list = ArrayList<RScreen>()

        var gson: Gson? = null
        var debug: Boolean? = null
        var initialize: Boolean = false
        var builders: HashMap<Builder, BuilderFace> ? = null

        @JvmStatic fun initialize(context: Context, urlServer: String, redisServer: String, RStatus: RStatus) {
            this@Companion.context = context
            this@Companion.urlServer = urlServer
            this@Companion.urlRedis = redisServer
            this@Companion.RStatus = RStatus
            if (builders == null) {
                builders = HashMap<Builder, BuilderFace>()
            }
            debug = false
            gson = Gson()
            val shared = context.getSharedPreferences(PREF_CONFIG, MODE_PRIVATE)
            id = shared.getString(PREF_ID, null)
            if (id == null) {
                id = generateNewId()
            }

            serviceComponent = ComponentName(context, RJobService::class.java)

            initialize = false

            stop()
            start()
        }

        private fun generateNewId(): String {
            val id = Settings.Secure.getString(context!!.getContentResolver(), Settings.Secure.ANDROID_ID)
            val shared = context!!.getSharedPreferences(PREF_CONFIG, MODE_PRIVATE).edit()
            shared.putString(PREF_ID, id)
            shared.apply()
            return id
        }

        fun stop() {
            try {
                (context?.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).cancelAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun start() {
            context?.let {
                val builder = JobInfo.Builder(jobId++, serviceComponent!!)
                /*
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    builder.setPeriodic(5000)
                }*/
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)

                Log.d(TAG, "Scheduling job")
                (it.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(builder.build())
            }
        }

        @JvmStatic fun onMessageReceived(jsonObject: JSONObject) {
            if (builders != null) {
                for (face in builders!!.values) {
                    face.onMessageReceived(jsonObject)
                }
            }
        }

        @JvmStatic fun prepare(type: Builder, face: BuilderFace) {
            if (builders != null) {
                builders!![type] = face
            }
        }

        @JvmStatic fun debug(debug: Boolean) {
            this@Companion.debug = debug
        }

        internal fun connected() {
            initialize = true
            for (entry in list) {
                if (entry.isActive) {
                    entry.connected()
                }
            }
            RStatus?.ready()
        }

        internal fun notConnected() {
            initialize = false
            for (entry in list) {
                if (entry.isActive) {
                    entry.disconnected()
                }
            }
        }

        @JvmStatic fun isConnected() : Boolean {
            if (NetworkUtil.getConnectivityStatus(Rotor.context!!).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) && initialize) {
                stop()
            } else if (!NetworkUtil.getConnectivityStatus(Rotor.context!!).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) && !initialize) {
                start()
            }
            return !NetworkUtil.getConnectivityStatus(Rotor.context!!).equals(NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) && initialize
        }

        @JvmStatic fun screens() : ArrayList<RScreen> {
            return list
        }

        fun onResume() {
            builders?.let {
                for (face in it.entries) {
                    face.value.onResume()
                }
            }
        }

        fun onPause() {
            builders?.let {
                for (face in it.entries) {
                    face.value.onPause()
                }
            }
        }

        @JvmStatic fun addJob(job: RJob) {
            if (!jobs.contains(job)) {
                job.onCreate()
                job.startJob()
                jobs.add(job)
            }
        }

        @JvmStatic fun removeJob(job: RJob) {
            if (jobs.contains(job)) {
                job.stopJob()
                jobs.remove(job)
            }
        }

        @JvmStatic fun jobs() : ArrayList<RJob> {
            return jobs
        }

    }


}