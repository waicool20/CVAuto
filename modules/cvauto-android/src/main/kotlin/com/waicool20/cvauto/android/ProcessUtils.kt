package com.waicool20.cvauto.android

internal fun Process.readLines() = inputStream.bufferedReader().readLines()
internal fun Process.readText() = inputStream.bufferedReader().readText()
internal fun Process.lineSequence() = inputStream.bufferedReader().lineSequence()