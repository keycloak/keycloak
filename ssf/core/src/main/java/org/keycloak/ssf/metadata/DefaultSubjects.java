package org.keycloak.ssf.metadata;

/**
 * Legal values for the SSF transmitter's {@code default_subjects}
 * metadata field (SSF 1.0 §7.1). Controls the transmitter's
 * default behavior for subject-scoped event delivery:
 *
 * <ul>
 *     <li>{@link #ALL} — deliver events for every matching subject unless
 *         a stream explicitly narrows the scope. Preserves the
 *         transmitter's pre-subject-management behaviour.</li>
 *     <li>{@link #NONE} — deliver events only for subjects that have been
 *         explicitly subscribed (via receiver add-subject calls or via
 *         admin-curated {@code ssf.notify.<clientId>} attributes).</li>
 * </ul>
 *
 * <p>The spec also permits a concrete Subject claim as the third option;
 * that variant is not supported in Keycloak today.
 */
public enum DefaultSubjects {
    ALL,
    NONE;

    /**
     * Parses a case-insensitive string into a {@link DefaultSubjects}
     * value, returning {@code fallback} when the input is {@code null},
     * blank, or not a legal value. Used from SPI / config entrypoints
     * where user input needs to be tolerant of case.
     */
    public static DefaultSubjects parseOrDefault(String value, DefaultSubjects fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return DefaultSubjects.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
