package com.rocketbase.core.interfaces

import org.json.JSONObject

/**
 * Created by efraespada on 12/03/2018.
 */
interface BuilderFace {
    fun onMessageReceived(jsonObject: JSONObject)
}