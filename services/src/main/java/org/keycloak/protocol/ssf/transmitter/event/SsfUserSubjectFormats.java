package org.keycloak.protocol.ssf.transmitter.event;

import java.util.Set;

import org.keycloak.protocol.ssf.event.subjects.EmailSubjectId;
import org.keycloak.protocol.ssf.event.subjects.IssuerSubjectId;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;

/**
 * Resolves and validates the subject identifier format the transmitter
 * uses for the <em>user</em> part of outgoing SSF Security Event Tokens.
 *
 * <p>Defaults to {@link IssuerSubjectId#TYPE iss_sub} (the transmitter
 * realm's issuer URL plus the user's Keycloak ID). Receiver clients can
 * override per-stream by setting the {@code ssf.userSubjectFormat}
 * attribute to one of the values in {@link #ALLOWED}; any other value is
 * rejected at stream create/update time so misconfigurations surface
 * before the first event emission.
 *
 * <p>The allow-list is deliberately small for now ({@code iss_sub} and
 * {@code email}) — expanding it to other SSF subject formats (e.g.
 * {@code phone_number}, {@code aliases}) is a matter of extending the
 * mapper's {@code buildUserSubjectId} dispatch and adding the value here.
 */
public final class SsfUserSubjectFormats {

    /**
     * Subject identifier formats the transmitter knows how to produce
     * for the user portion of an SSF SET. Validated at stream
     * create/update time via {@link #isAllowed(String)}.
     */
    public static final Set<String> ALLOWED = Set.of(
            IssuerSubjectId.TYPE,
            EmailSubjectId.TYPE);

    /**
     * Default user subject identifier format — the realm issuer plus the
     * user's Keycloak ID. Matches the behavior the transmitter had before
     * this knob was added, so existing deployments observe no change.
     */
    public static final String DEFAULT = IssuerSubjectId.TYPE;

    private SsfUserSubjectFormats() {
    }

    /**
     * Resolves the user subject format for a given stream, honoring the
     * precedence: per-stream override → transmitter SPI default →
     * hardcoded {@link #DEFAULT}.
     */
    public static String resolveForStream(StreamConfig streamConfig, SsfTransmitterConfig transmitterConfig) {

        if (streamConfig != null) {
            String streamFormat = streamConfig.getUserSubjectFormat();
            if (streamFormat != null && !streamFormat.isBlank()) {
                return streamFormat;
            }
        }

        if (transmitterConfig != null) {
            String configFormat = transmitterConfig.getUserSubjectFormat();
            if (configFormat != null && !configFormat.isBlank()) {
                return configFormat;
            }
        }

        return DEFAULT;
    }

    /**
     * Returns {@code true} if the given format is in the allow-list.
     * Blank or {@code null} values are treated as not allowed — callers
     * should default to {@link #DEFAULT} before consulting this method
     * if they want missing config to fall through.
     */
    public static boolean isAllowed(String format) {
        return format != null && !format.isBlank() && ALLOWED.contains(format);
    }
}
