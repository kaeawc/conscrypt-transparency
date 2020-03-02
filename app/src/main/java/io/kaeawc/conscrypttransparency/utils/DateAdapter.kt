package io.kaeawc.conscrypttransparency.utils

import arrow.core.Try
import arrow.core.orNull
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter

class DateAdapter {

    companion object {
        @JvmStatic fun tryParsing(value: String): Instant? = Try {
            OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .atZoneSameInstant(ZoneOffset.UTC)
                .toInstant()
        }.orNull()
    }

    @ToJson fun toJson(value: Instant): String {
        val str = value.toString()
        return when (str.length) {
            20 -> str.substring(0, 19) + ".000Z"
            23 -> str
            else -> str // hope for the best?
        }
    }

    @FromJson fun fromJson(value: String): Instant? {
        return tryParsing(value)
    }
}
