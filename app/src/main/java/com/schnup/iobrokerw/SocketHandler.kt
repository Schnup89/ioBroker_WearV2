package com.schnup.iobrokerw

import android.telecom.PhoneAccount.builder
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.Transport
import io.socket.engineio.client.transports.Polling.NAME
import io.socket.engineio.client.transports.WebSocket.NAME
import okhttp3.WebSocket
import java.lang.Exception
import java.net.URI
import java.net.URISyntaxException

object SocketHandler {

    lateinit var mSocket: Socket
    lateinit var mOpts: IO.Options
    @Synchronized
    fun setSocket(sUrl : String): Boolean {
        try {
            mOpts = IO.Options()
            mOpts.reconnection = true
            mOpts.reconnectionDelay = 1000
            mOpts.reconnectionDelayMax = 5000
            mOpts.transports = arrayOf(io.socket.engineio.client.transports.PollingXHR.NAME)
            mOpts.timeout = 5000
            //mOpts.transports = arrayOf(io.socket.engineio.client.transports.WebSocket.NAME)
            mOpts.query = "key=nokey"
            mSocket = IO.socket(sUrl, mOpts)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    @Synchronized
    fun getSocket(): Socket {
        return mSocket
    }

    @Synchronized
    fun establishConnection(): Socket {
        return mSocket.connect()
    }

    @Synchronized
    fun closeConnection() {
        mSocket.disconnect()
    }
}