package io.kaeawc.conscrypttransparency

import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.HttpException
import timber.log.Timber
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLPeerUnverifiedException

private val UTF8: Charset = Charset.forName("UTF-8")

fun Int.isHttpCodeRecoverable(): Boolean {
    return when (this) {
        460 -> true
        in 500..599 -> true
        else -> false
    }
}

fun HttpException.isRecoverable(): Boolean {
    return this.code().isHttpCodeRecoverable()
}

fun <T> Throwable.getHttpErrorPlainText(moshi: Moshi, clazz: Class<T>, code: Int? = null): T? {
    if (this !is HttpException || (code != null && this.code() != code)) return null
    return this.response()?.errorBody()?.getPlainText()?.readJson(moshi, clazz)
}

fun Throwable.isRecoverable(): Boolean {
    return when (this) {
        is HttpException -> this.isRecoverable()

        // Could not parse response body as JSON
        is JsonEncodingException -> false

        // SSL Certificate invalid or couldn't be verified
        is SSLPeerUnverifiedException -> false

        // Network failure
        is ConnectException,
        is SocketTimeoutException,
        is UnknownHostException,
        is TimeoutException,
        is IOException -> true

        // Could not parse response
        is IllegalArgumentException -> false
        else -> false
    }
}

fun ResponseBody.getPlainText(): String {

    val source = source()
    source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
    val buffer = source.buffer

    val contentLength = contentLength()
    var charset = UTF8
    val contentType = contentType()
    if (contentType != null) {
        charset = contentType.charset(UTF8) ?: return ""
    }

    return when (contentLength > 0 && buffer.isPlaintext()) {
        true -> buffer.clone().readString(charset)
        false -> ""
    }
}

fun Buffer.isPlaintext(): Boolean {

    try {
        val prefix = Buffer()
        val byteCount = if (size < 64) size else 64
        copyTo(prefix, 0, byteCount)
        for (i in 0..15) {
            if (prefix.exhausted()) {
                break
            }
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                return false
            }
        }
        return true
    } catch (e: EOFException) {
        Timber.e("Could not determine whether buffer was plaintext")
        return false // Truncated UTF-8 sequence.
    }
}


fun Request.failWithReason(code: Int, reason: String): Response {
    Timber.e("Automatically failing request to $method $url")
    val mimeType = "application/json".toMediaTypeOrNull()
    val body: ResponseBody = "{\"local\":true,\"code\":$code,\"reason\":\"${reason.replace('"', '\'')}\"}".toResponseBody(mimeType)
    return Response.Builder()
        .code(code)
        .protocol(Protocol.HTTP_2)
        .message(reason)
        .body(body)
        .request(this)
        .build()
}
