package com.omnifret.gplayer.core

internal open class Console {
    public open fun debug(format: String, vararg details: Any?) {
        val message = if (details.isNotEmpty()) "$format,${details.joinToString(",")}" else format
        println("[GPlayer Debug] $message")
    }

    public open fun warn(format: String, vararg details: Any?) {
        val message = if (details.isNotEmpty()) "$format,${details.joinToString(",")}" else format
        println("[GPlayer Warn] $message")
    }

    public open fun info(format: String, vararg details: Any?) {
        val message = if (details.isNotEmpty()) "$format,${details.joinToString(",")}" else format
        println("[GPlayer Info] $message")
    }

    public open fun error(format: String, vararg details: Any?) {
        val message = if (details.isNotEmpty()) "$format,${details.joinToString(",")}" else format
        println("[GPlayer Error] $message")
    }
}
