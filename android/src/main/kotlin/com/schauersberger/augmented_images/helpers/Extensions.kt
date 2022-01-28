package com.schauersberger.augmented_images.helpers

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.toMap(): Map<String, Any> {
    val map = HashMap<String, Any>()

    val keysItr = keys().iterator()
    while (keysItr.hasNext()) {
        val key = keysItr.next()
        var value = get(key)

        if (value is JSONArray) {
            value = toList(value)
        } else if (value is JSONObject) {
            value = value.toMap()
        }
        map[key] = value
    }
    return map
}

fun toList(array: JSONArray): List<Any> {
    val list = ArrayList<Any>()
    for (i in 0 until array.length()) {
        var value = array.get(i)
        if (value is JSONArray) {
            value = toList(value)
        } else if (value is JSONObject) {
            value = value.toMap()
        }
        list.add(value)
    }
    return list
}