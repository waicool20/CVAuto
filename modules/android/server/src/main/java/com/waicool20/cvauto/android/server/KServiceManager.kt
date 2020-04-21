package com.waicool20.cvauto.android.server

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.input.IInputManager
import android.os.IBinder
import android.view.IWindowManager

@SuppressLint("PrivateApi")
object KServiceManager {
    private val cls = Class.forName("android.os.ServiceManager")
    private val getServiceMethod by lazy { cls.getDeclaredMethod("getService", String::class.java) }

    val windowManager by lazy { IWindowManager.Stub.asInterface(getService(Context.WINDOW_SERVICE)) }
    val inputManager by lazy { IInputManager.Stub.asInterface(getService(Context.INPUT_SERVICE)) }

    private fun getService(service: String): IBinder {
        return getServiceMethod.invoke(null, service) as IBinder
    }
}