package com.rotor.core

import android.content.Context
import android.net.ConnectivityManager

/**
 * Created by efraespada on 11/03/2018.
 */
class NetworkUtil {

    companion object {
        var TYPE_WIFI = 1
        var TYPE_MOBILE = 2
        var TYPE_NOT_CONNECTED = 0
        val NETWORK_STATUS_NOT_CONNECTED = 0
        val NETWORK_STAUS_WIFI = 1
        val NETWORK_STATUS_MOBILE = 2

        fun getConnectivityStatus(context: Context): Int {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork = cm.activeNetworkInfo
            if (null != activeNetwork) {
                if (activeNetwork.type == ConnectivityManager.TYPE_WIFI)
                    return TYPE_WIFI

                if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE)
                    return TYPE_MOBILE
            }
            return TYPE_NOT_CONNECTED
        }

        fun getConnectivityStatusString(context: Context): Int {
            val conn = getConnectivityStatus(context)
            var status = 0
            if (conn == TYPE_WIFI) {
                status = NETWORK_STAUS_WIFI
            } else if (conn == TYPE_MOBILE) {
                status = NETWORK_STATUS_MOBILE
            } else if (conn == TYPE_NOT_CONNECTED) {
                status = NETWORK_STATUS_NOT_CONNECTED
            }
            return status
        }
    }
}