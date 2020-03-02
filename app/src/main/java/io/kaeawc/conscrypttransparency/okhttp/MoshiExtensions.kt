package io.kaeawc.conscrypttransparency.okhttp

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import java.io.EOFException
import java.io.IOException
import java.lang.reflect.Type

fun Any.writeAsJson(moshi: Moshi): String? {
    return try {
        moshi.adapter(javaClass).toJson(this)
    } catch (ex: IOException) {
        null
    }
}

@Suppress("UNUSED")
fun <T> List<T>.writeAsJsonList(moshi: Moshi, clazz: Class<T>): String? {
    val type: Type = Types.newParameterizedType(List::class.java, clazz)
    val jsonAdapter = moshi.adapter<List<T>>(type)
    return try {
        jsonAdapter.toJson(this)
    } catch (ex: IOException) {
        null
    }
}

fun <T> String.readJson(moshi: Moshi, clazz: Class<T>): T? {
    if (isBlank()) {
        return null
    }
    val jsonAdapter = moshi.adapter(clazz)
    return try {
        jsonAdapter.lenient().fromJson(this)
    } catch (ex: JsonEncodingException) {
        Timber.e(ex)
        null
    } catch (ex: JsonDataException) {
        Timber.e(ex)
        null
    } catch (ex: EOFException) {
        Timber.e("Could not parse anything")
        null
    }
}

fun <T> String.readJsonList(moshi: Moshi, clazz: Class<T>): List<T>? {

    if (isBlank()) {
        Timber.e("Cannot parse object from blank JSON string")
        return null
    }

    val type: Type = Types.newParameterizedType(List::class.java, clazz)
    val jsonAdapter = moshi.adapter<List<T>>(type)
    return try {
        jsonAdapter.lenient().fromJson(this)
    } catch (ex: JsonEncodingException) {
        null
    } catch (ex: JsonDataException) {
        null
    } catch (ex: EOFException) {
        Timber.e("Could not parse anything")
        null
    }
}
