package org.keycloak.services.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

public class DateUtil {

    /**
     * Parses a string timestamp or date to an Epoch timestamp in milliseconds (number of milliseconds since January 1, 1970, 00:00:00 GMT);
     * if the date is a ISO-8601 extended local date format, the time at the beginning of the day is returned.
     *
     * @param date the date in ISO-8601 extended local date format
     * @return Epoch time for the start of the day
     */
    public static long toStartOfDay(String date) {
        if (date.indexOf('-') != -1) {
            return LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        } else {
            return Long.parseLong(date);
        }
    }

    /**
     * Parses a string timestamp or date to an Epoch timestamp in milliseconds (number of milliseconds since January 1, 1970, 00:00:00 GMT);
     * if the date is a ISO-8601 extended local date format, the time at the end of the day is returned.
     *
     * @param date the date in ISO-8601 extended local date format
     * @return Epoch time for the end of the day
     */
    public static long toEndOfDay(String date) {
        if (date.indexOf('-') != -1) {
            return LocalDate.parse(date).atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli();
        } else {
            return Long.parseLong(date);
        }
    }

}
