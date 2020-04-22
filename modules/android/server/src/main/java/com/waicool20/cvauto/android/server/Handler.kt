package com.waicool20.cvauto.android.server

import java.io.Closeable

interface Handler: Closeable {
    fun waitFor()
}