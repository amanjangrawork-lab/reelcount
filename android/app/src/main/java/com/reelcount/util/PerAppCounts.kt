package com.reelcount.util

import org.json.JSONObject

/**
 * Serialises the per-app counter map to and from the JSON blob stored in
 * `daily_counts.perAppJson`. Malformed or empty input yields an empty map
 * rather than throwing, so a corrupt row can never crash the counter.
 */
object PerAppCounts {

    fun parse(raw: String?): MutableMap<String, Int> {
        val result = mutableMapOf<String, Int>()
        if (raw.isNullOrBlank()) {
            return result
        }
        try {
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.optInt(key, 0)
                if (key.isNotBlank() && value > 0) {
                    result[key] = value
                }
            }
        } catch (ignored: Exception) {
            return mutableMapOf()
        }
        return result
    }

    fun serialize(counts: Map<String, Int>): String {
        val json = JSONObject()
        for ((key, value) in counts) {
            if (key.isNotBlank() && value > 0) {
                json.put(key, value)
            }
        }
        return json.toString()
    }

    /** Parses per-app toggles, defaulting every tracked app to enabled. */
    fun parseToggles(raw: String?): MutableMap<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        for (pkg in TrackedApps.ALL) {
            result[pkg] = true
        }
        if (raw.isNullOrBlank()) {
            return result
        }
        try {
            val json = JSONObject(raw)
            for (pkg in TrackedApps.ALL) {
                if (json.has(pkg)) {
                    result[pkg] = json.optBoolean(pkg, true)
                }
            }
        } catch (ignored: Exception) {
            return result
        }
        return result
    }

    fun serializeToggles(toggles: Map<String, Boolean>): String {
        val json = JSONObject()
        for (pkg in TrackedApps.ALL) {
            json.put(pkg, toggles[pkg] ?: true)
        }
        return json.toString()
    }
}
