package io.kaeawc.conscrypttransparency

import arrow.core.Try
import arrow.core.orNull
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import java.lang.IllegalArgumentException
import java.util.*

class DateAdapter {

    companion object {
        @JvmStatic fun tryParsing(value: String): Instant? = Try {
            OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .atZoneSameInstant(ZoneOffset.UTC)
                .toInstant()
        }.orNull()

        @JvmStatic fun tryFormattingLocalHumanTime(value: Instant): String {
            val zoneId = Try { ZoneId.systemDefault() }.orNull() ?: ZoneId.of("UTC")
            return LocalDateTime.ofInstant(value, zoneId).format(DateTimeFormatter.ofPattern("h:mma", Locale.US))
        }

        @JvmStatic fun tryFormattingLocalHumanDateTime(value: Instant): String {
            val zoneId = Try { ZoneId.systemDefault() }.orNull() ?: ZoneId.of("UTC")
            return LocalDateTime.ofInstant(value, zoneId).format(DateTimeFormatter.ofPattern("E, MMM d, h:mma", Locale.US))
        }

        @JvmStatic fun tryFormattingLocalHumanDate(value: Instant): String {
            val zoneId = Try { ZoneId.systemDefault() }.orNull() ?: ZoneId.of("UTC")
            return LocalDateTime.ofInstant(value, zoneId).format(DateTimeFormatter.ofPattern("E, MMM d", Locale.US))
        }

        /**
         * Creates a Calendar instance and sets the fields for year, month,
         * and day without retaining any other information.
         *
         * @param year the value used to set the [Year] field.
         * @param month the value used to set the [Month] field.
         *  Month value is 0-based. e.g., 0 for January.
         * @param day the value used to set the [MonthDay] field.
         */
        @JvmStatic fun parseDate(year: Int, month: Int, day: Int): Instant {
            if (month < 0) throw IllegalArgumentException("Month must be a positive number or zero")
            if (month >= 12) throw IllegalArgumentException("Month must be less than 12")
            if (day <= 0) throw IllegalArgumentException("Day must be a positive number")
            if (day > 31) throw IllegalArgumentException("Day must be less than or equal to 31")

            val millis = Calendar.getInstance().apply {
                clear()
                set(year, month, day)
                timeZone = TimeZone.getTimeZone("UTC")
            }.timeInMillis

            return Instant.ofEpochMilli(millis)
        }
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
