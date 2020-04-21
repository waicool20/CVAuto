package com.waicool20.cvauto.android.server

import android.text.format.DateFormat
import android.util.Log
import java.util.*


object Logger {
    val TAG = "CVAutoAndroidServer"

    fun i(msg: String) {
        Log.i(TAG, msg)
        println("[$TAG] [$time] INFO - $msg")
    }

    fun e(msg: String) {
        Log.e(TAG, msg)
        println("[$TAG] [$time] ERROR - $msg")
    }

    fun w(msg: String) {
        Log.w(TAG, msg)
        println("[$TAG] [$time] WARN - $msg")
    }

    private val time get() = DateFormat.format("yyyy-MM-dd HH:mm:ss", Date())
}