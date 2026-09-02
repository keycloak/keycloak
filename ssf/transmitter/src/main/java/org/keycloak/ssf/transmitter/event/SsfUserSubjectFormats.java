package org.keycloak.ssf.transmitter.event;

import java.util.Set;

import org.keycloak.ssf.subject.EmailSubjectId;
import org.keycloak.ssf.subject.IssuerSubjectId;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

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
 * <p>Two compositions are also supported —
 * {@link #COMPLEX_ISS_SUB_PLUS_TENANT complex.iss_sub+tenant} and
 * {@link #COMPLEX_EMAIL_PLUS_TENANT complex.email+tenant} — which wrap
 * the user subject in a complex subject and add a {@code tenant}
 * sibling carrying the user's Keycloak organization. The
 * {@code complex.} prefix flags that the SET will carry a
 * {@link org.keycloak.ssf.subject.ComplexSubjectId ComplexSubjectId}
 * rather than a single subject; the {@code +tenant} suffix names the
 * additional member. Both are Keycloak-only compositions, not
 * RFC 9493 subject identifier formats; receivers that don't understand
 * {@code critical_subject_members=["user","tenant"]} will reject the
 * stream at creation time.
 *
 * <p>The allow-list is deliberately small for now — expanding it to
 * other SSF subject formats (e.g. {@code phone_number}, {@code aliases})
 * is a matter of extending the mapper's {@code buildUserSubjectId}
 * dispatch and adding the value here.
 */
public final class SsfUserSubjectFormats {

    /**
     * Composition prefix that flags the SET will carry a
     * {@link org.keycloak.ssf.subject.ComplexSubjectId ComplexSubjectId}
     * rather than a single user subject. The portion after the prefix
     * names the user-subject component (e.g. {@code iss_sub} /
     * {@code email}); additional siblings are appended via composition
     * suffixes such as {@link #TENANT_SUFFIX}.
     */
    public static final String COMPLEX_PREFIX = "complex.";

    /** Composition suffix that asks the mapper to add a {@code tenant} member. */
    public static final String TENANT_SUFFIX = "+tenant";

    /** Composition: complex(user={@code iss_sub}, tenant=user's organization). */
    public static final String COMPLEX_ISS_SUB_PLUS_TENANT = COMPLEX_PREFIX + IssuerSubjectId.TYPE + TENANT_SUFFIX;

    /** Composition: complex(user={@code email}, tenant=user's organization). */
    public static final String COMPLEX_EMAIL_PLUS_TENANT = COMPLEX_PREFIX + EmailSubjectId.TYPE + TENANT_SUFFIX;

    /**
     * Subject identifier formats the transmitter knows how to produce
     * for the user portion of an SSF SET. Validated at stream
     * create/update time via {@link #isAllowed(String)}.
     */
    public static final Set<String> ALLOWED = Set.of(
            IssuerSubjectId.TYPE,
            EmailSubjectId.TYPE,
            COMPLEX_ISS_SUB_PLUS_TENANT,
            COMPLEX_EMAIL_PLUS_TENANT);

    /**
     * Default user subject identifier format — the realm issuer plus the
     * user's Keycloak ID. Matches the behavior the transmitter had before
     * this knob was added, so existing deployments observe no change.
     */
    public static final String DEFAULT = IssuerSubjectId.TYPE;

    private SsfUserSubjectFormats() {
    }

    /**
     * Returns {@code true} if {@code format} carries the {@code +tenant}
     * composition suffix — i.e. the mapper should add a tenant subject
     * sibling to the complex SET subject.
     */
    public static boolean includesTenant(String format) {
        return format != null && format.endsWith(TENANT_SUFFIX);
    }

    /**
     * Strips the {@link #COMPLEX_PREFIX complex.} prefix and the
     * {@link #TENANT_SUFFIX +tenant} suffix to return the bare
     * user-subject format ({@code iss_sub} / {@code email}). Pass-through
     * when neither marker is present.
     */
    public static String userPartOf(String format) {
        if (format == null) {
            return null;
        }
        String result = format;
        if (result.startsWith(COMPLEX_PREFIX)) {
            result = result.substring(COMPLEX_PREFIX.length());
        }
        if (result.endsWith(TENANT_SUFFIX)) {
            result = result.substring(0, result.length() - TENANT_SUFFIX.length());
        }
        return result;
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
