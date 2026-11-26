package org.keycloak.ssf.transmitter.event;

import java.util.Set;

import org.keycloak.crypto.Algorithm;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

/**
 * Resolves and validates the JWS signature algorithm used for outgoing SSF
 * Security Event Tokens.
 *
 * <p>The CAEP interoperability profile 1.0 §2.6 pins transmitters to RS256
 * with 2048-bit RSA keys, so {@link #ALLOWED} currently contains only
 * {@code RS256}. The plumbing around it (per-receiver override on
 * {@link StreamConfig}, SPI default on {@link SsfTransmitterConfig}) is
 * deliberately kept flexible so the allow-list can grow once the working
 * group broadens the profile — in practice that means today every receiver
 * ends up signed with RS256 regardless of override, but operators can see
 * the knob exists and tests can exercise the resolution order.
 *
 * @see <a href="https://openid.github.io/sharedsignals/openid-caep-interoperability-profile-1_0.html#section-2.6">CAEP Interoperability Profile §2.6</a>
 */
public final class SsfSignatureAlgorithms {

    /**
     * Algorithms the transmitter is willing to emit SSF Security Event Tokens
     * under. Any alg outside this set is rejected at stream create/update
     * time so misconfigurations surface before the first push.
     */
    public static final Set<String> ALLOWED = Set.of(Algorithm.RS256);

    /**
     * Hardcoded safety-net default, matching the CAEP interop profile. Used
     * when neither the stream nor the transmitter SPI config provides a
     * value.
     */
    public static final String DEFAULT = Algorithm.RS256;

    private SsfSignatureAlgorithms() {
    }

    /**
     * Resolves the signature algorithm to use for a given stream, honoring
     * the precedence: per-stream override → transmitter SPI default →
     * hardcoded {@link #DEFAULT}.
     */
    public static String resolveForStream(StreamConfig streamConfig, SsfTransmitterConfig transmitterConfig) {

        if (streamConfig != null) {
            String streamAlg = streamConfig.getSignatureAlgorithm();
            if (streamAlg != null && !streamAlg.isBlank()) {
                return streamAlg;
            }
        }

        if (transmitterConfig != null) {
            String configAlg = transmitterConfig.getSignatureAlgorithm();
            if (configAlg != null && !configAlg.isBlank()) {
                return configAlg;
            }
        }

        return DEFAULT;
    }

    /**
     * Returns {@code true} if the given algorithm is in the allow-list.
     * Blank or {@code null} values are treated as not allowed — callers
     * should default to {@link #DEFAULT} before consulting this method if
     * they want missing config to fall through.
     */
    public static boolean isAllowed(String algorithm) {
        return algorithm != null && !algorithm.isBlank() && ALLOWED.contains(algorithm);
    }
}
