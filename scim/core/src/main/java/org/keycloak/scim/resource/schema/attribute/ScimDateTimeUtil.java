package org.keycloak.scim.resource.schema.attribute;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Shared helper to convert SCIM date/time representations (ISO 8601 strings, e.g. "2011-05-13T04:42:34Z") to the
 * {@link Long} epoch-millisecond representation used by Keycloak models.
 */
public final class ScimDateTimeUtil {

    private ScimDateTimeUtil() {
    }

    /**
     * Parses the given {@code dateTimeString} as an ISO 8601 date/time, falling back to a plain numeric timestamp.
     *
     * @param dateTimeString the date/time string to parse
     * @return the parsed timestamp as {@link Long} milliseconds since epoch
     * @throws IllegalArgumentException if the input string is not a valid ISO 8601 date/time or numeric timestamp
     */
    public static Long parseDateTime(String dateTimeString) {
        try {
            return Instant.parse(dateTimeString).toEpochMilli();
        } catch (DateTimeParseException e) {
            try {
                return Long.parseLong(dateTimeString);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid date/time format: " + dateTimeString
                        + ". Expected ISO 8601 format (e.g., 2011-05-13T04:42:34Z) or timestamp");
            }
        }
    }
}
