package com.schauersberger.augmented_images.communication

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.schauersberger.augmented_images.helpers.toMap
import io.flutter.plugin.common.EventChannel
import org.json.JSONObject

class MessageStreamHandler : EventChannel.StreamHandler {
    private var eventSink: EventChannel.EventSink? = null
    override fun onListen(arguments: Any?, sink: EventChannel.EventSink) {
        Log.e("Sheesh", "Listener connected")
        eventSink = sink
    }

    fun send(channel: String, event: String, data: Any) {
        val json = JSONObject(data as String)
        val map = json.toMap()
        Handler(Looper.getMainLooper()).post {
            eventSink?.success(mapOf("channel" to channel,
                "event" to event,
                "body" to map))
        }
    }

    override fun onCancel(p0: Any?) {
        Log.e("Sheesh", "Listener canceled")
        eventSink = null
    }
}