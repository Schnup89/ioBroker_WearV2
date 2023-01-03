package com.schnup.iobrokerw

interface MessageListener {
    fun onConnectSuccess()
    fun onConnectFailed()
    fun onClose()
    fun onMessage(sMsg: String?)
}