package com.schnup.iobrokerw

import android.util.Log
import okhttp3.*
import okio.ByteString
import org.json.JSONArray
import java.util.concurrent.TimeUnit

object WebSocketManager {
    const val TAG = "Webscks"
    private const val MILLIS = 1000
    private lateinit var client: OkHttpClient
    private lateinit var request: Request
    private lateinit var messageListener: MessageListener
    private lateinit var mWebSocket: WebSocket
    private var isConnect = false
    var nWSID = 0


    fun init(url: String, _messageListener: MessageListener) : Boolean {
        try {
            client = OkHttpClient.Builder()
                .writeTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            request = Request.Builder().url(url).build()
            messageListener = _messageListener
        }catch (e:Exception){
            Log.e(
                TAG,
                "Init-Failed: " + e.message.toString()
            )
            return false
        }
        return true
    }

    fun connect() : Boolean {
        try {
            if (isConnect()) {
                Log.i(TAG, "Connected")
                return true
            }
            client.newWebSocket(request, createListener())
        } catch (e:Exception){
            Log.e(TAG, "Connect failed: " + e.message.toString())
            return false
        }
        return true
    }

    fun reconnect() {
        try {
            Thread.sleep(MILLIS.toLong())
            connect()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun isConnect(): Boolean {
        return isConnect
    }

    fun sendMessage(sOut: String): Boolean {
        nWSID++
        return if (!isConnect()) false else mWebSocket.send(sOut)
    }

    fun sendCallback(sTopic: String,sIOID: String): Boolean {
        nWSID++
        val sOut: String = JSONArray(listOf(3, nWSID,sTopic,JSONArray(listOf(sIOID)))).toString()
        return if (!isConnect()) false else mWebSocket.send(sOut)
    }


    fun close() {
        if (isConnect()) {
            mWebSocket.cancel()
            mWebSocket.close(1001, "Client Requested")
        }
    }

    private fun createListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(
                webSocket: WebSocket,
                response: Response
            ) {
                super.onOpen(webSocket, response)
                Log.d(TAG, "open:$response")
                mWebSocket = webSocket
                isConnect = response.code == 101
                if (!isConnect) {
                    reconnect()
                } else {
                    Log.i(TAG, "connect success.")
                    messageListener.onConnectSuccess()
                }
            }

            override fun onMessage(webSocket: WebSocket, sMsg: String) {
                super.onMessage(webSocket, sMsg)
                messageListener.onMessage(sMsg)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                messageListener.onMessage(bytes.base64())
            }

            override fun onClosing(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {
                super.onClosing(webSocket, code, reason)
                isConnect = false
                messageListener.onClose()
                Log.d(TAG, "Closed Reason: $code")
            }

            override fun onClosed(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {
                super.onClosed(webSocket, code, reason)
                isConnect = false
                messageListener.onClose()
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {
                super.onFailure(webSocket, t, response)
                if (response != null) {
                    Log.i(
                        TAG,
                        "connect failed：" + response.message
                    )
                }
                Log.i(
                    TAG,
                    "connect failed throwable：" + t.message
                )
                isConnect = false
                messageListener.onConnectFailed()
                reconnect()
            }
        }
    }
}
