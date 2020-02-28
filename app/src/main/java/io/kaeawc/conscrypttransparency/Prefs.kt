package io.kaeawc.conscrypttransparency

import com.squareup.moshi.Moshi
import org.threeten.bp.Instant
import android.content.SharedPreferences
import androidx.core.content.edit
import arrow.core.Try
import arrow.core.getOrElse
import org.threeten.bp.Duration
import javax.inject.Inject

open class Prefs @Inject constructor(val prefs: SharedPreferences, val moshi: Moshi) {

    internal open fun SharedPreferences.get(key: String): String? {
        return getString(key, null)
    }

    internal open fun SharedPreferences.get(key: String, default: String): String {
        return getString(key, default) ?: default
    }

    internal open fun SharedPreferences.getInstant(key: String): Instant {
        return Instant.ofEpochMilli(getLong(key, 0))
    }

    internal open fun SharedPreferences.getNullableInstant(key: String): Instant? {

        return Try {
            val value = getLong(key, 0)
            return if (value > 0) {
                Instant.ofEpochMilli(value)
            } else {
                null
            }
        }.getOrElse {
            val dateValue: String = prefs.get(key) ?: return null
            val value = DateAdapter.tryParsing(dateValue)
            prefs.setNullableInstant(key, value)
            value
        }
    }

    internal open fun SharedPreferences.setNullableInstant(key: String, value: Instant?) {
        if (value != null) {
            prefs.put(key, value.toEpochMilli())
        } else {
            prefs.put(key, Long.MIN_VALUE)
        }
    }

    internal open fun SharedPreferences.getNonNullInstant(key: String, default: Instant): Instant {
        return Try {
            val value = getLong(key, 0)
            return if (value > 0) {
                Instant.ofEpochMilli(value)
            } else {
                default
            }
        }.getOrElse {
            val dateValue: String = prefs.get(key) ?: "1970-01-01T00:00:00Z"
            val value = DateAdapter.tryParsing(dateValue) ?: Instant.ofEpochMilli(0)
            prefs.setNonNullInstant(key, value)
            value
        }
    }

    internal open fun SharedPreferences.setNonNullInstant(key: String, value: Instant) {
        prefs.put(key, value.toEpochMilli())
    }

    @Suppress("UNUSED")
    internal open fun SharedPreferences.putInstant(key: String, value: Instant) {
        prefs.put(key, value.toEpochMilli())
    }

    @Suppress("UNUSED")
    internal open fun SharedPreferences.putNullableInstant(key: String, value: Instant?) {
        prefs.put(key, value?.toEpochMilli() ?: Long.MIN_VALUE)
    }

    internal open fun SharedPreferences.put(key: String, value: String?) {
        prefs.edit { putString(key, value) }
    }

    internal open fun SharedPreferences.put(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    internal open fun SharedPreferences.put(key: String, value: Float) {
        prefs.edit { putFloat(key, value) }
    }

    @Suppress("UNUSED")
    internal open fun SharedPreferences.put(key: String, value: Double) {
        prefs.edit { putFloat(key, value.toFloat()) }
    }

    internal open fun SharedPreferences.put(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    internal open fun SharedPreferences.put(key: String, value: Long) {
        prefs.edit { putLong(key, value) }
    }

    open fun isCachedExpired(resource: String, maxAge: Duration, now: Instant = Instant.now()): Boolean {
        return prefs.getInstant("cache-$resource").plus(maxAge).isBefore(now)
    }

    open fun getCacheTimestamp(resource: String): Instant {
        return prefs.getInstant("cache-$resource")
    }

    open fun expireCache(resource: String) {
        prefs.put("cache-$resource", 0L)
    }

    open fun cacheResource(resource: String, now: Instant = Instant.now()) {
        prefs.put("cache-$resource", now.toEpochMilli())
    }

    @Suppress("UNUSED")
    open fun isCached(resource: String): Boolean {
        return getCacheTimestamp(resource).isAfter(firstAppLaunch)
    }

    open fun isCachedSinceLaunch(resource: String): Boolean {
        return getCacheTimestamp(resource).isAfter(lastAppLaunch)
    }

    @Suppress("UNUSED")
    open fun isRateLimitExceeded(resource: String, maxAttempts: Int): Boolean {
        return getResourceAttempts(resource) > maxAttempts
    }

    open fun getResourceAttempts(resource: String): Int {
        return prefs.getInt("rate-limit-$resource", 0)
    }

    @Suppress("UNUSED")
    open fun attemptRequest(resource: String) {
        prefs.put("rate-limit-$resource", getResourceAttempts(resource) + 1)
    }

    open fun clearResourceRateLimit(resource: String) {
        prefs.put("rate-limit-$resource", 0)
    }

    open fun wipeAllData() {
        prefs.edit { clear() }
    }

    open var hostName: String
        get() = prefs.get("hostName", "")
        set(value) = prefs.put("hostName", value)
    open var requestPath: String
        get() = prefs.get("requestPath", "")
        set(value) = prefs.put("requestPath", value)

    open var firstAppLaunch: Instant?
        get() = prefs.getNullableInstant("firstAppLaunch")
        set(value) = prefs.setNullableInstant("firstAppLaunch", value)
    open var lastAppLaunch: Instant
        get() = prefs.getNonNullInstant("lastAppLaunch", firstAppLaunch ?: Instant.now())
        set(value) = prefs.setNonNullInstant("lastAppLaunch", value)

}
