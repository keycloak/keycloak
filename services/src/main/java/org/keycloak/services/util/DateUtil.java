package org.keycloak.services.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

public class DateUtil {

    /**
     * Parses a string timestamp or date to an Epoc timestamp; if the date is a ISO-8601 extended local date format,
     * the time at the beginning of the day is returned.
     *
     * @param date the date in ISO-8601 extended local date format
     * @return Epoch time for the start of the day
     */
    public static long toStartOfDay(String date) {
        if (date.indexOf('-') != -1) {
            return LocalDate.parse(date).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        } else {
            return Long.parseLong(date);
        }
    }

    /**
     * Parses a string timestamp or date to an Epoc timestamp; if the date is a ISO-8601 extended local date format,
     * the time at the end of the day is returned.
     *
     * @param date the date in ISO-8601 extended local date format
     * @return Epoch time for the start of the day
     */
    public static long toEndOfDay(String date) {
        if (date.indexOf('-') != -1) {
            return LocalDate.parse(date).atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);
        } else {
            return Long.parseLong(date);
        }
    }

}
