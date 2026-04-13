package org.keycloak.protocol.ssf.receiver.registration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.crypto.Algorithm;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

/**
 * Holds the user configuration of an SSF Receiver.
 */
public class SsfReceiverRegistrationProviderConfig extends IdentityProviderModel {

    public static final String DESCRIPTION = "description";

    public static final String TRANSMITTER_METADATA_URL = "transmitterMetadataUrl";

    public static final String STREAM_ID = "streamId";

    public static final String STREAM_AUDIENCE = "streamAudience";

    public static final String TRANSMITTER_TOKEN = "transmitterToken";

    public static final String TRANSMITTER_TOKEN_TYPE = "transmitterTokenType";

    public static final String TRANSMITTER_AUTH_METHOD = "transmitterAuthMethod";

    public static final String CLIENT_ID = "clientId";

    public static final String CLIENT_SECRET = "clientSecret";

    public static final String CLIENT_AUTH_METHOD = "clientAuthMethod";

    public static final String TOKEN_URL = "tokenUrl";

    public static final String SCOPE = "scope";

    public static final String DELIVERY_METHOD = "deliveryMethod";

    public static final String PUSH_AUTHORIZATION_HEADER = "pushAuthorizationHeader";

    public static final String EXPECTED_SIGNATURE_ALGORITHMS = "expectedSignatureAlgorithms";

    /**
     * JWS signature algorithms this receiver is willing to accept on inbound
     * Security Event Tokens when
     * {@link #EXPECTED_SIGNATURE_ALGORITHMS} is unset. Defaults to the single
     * algorithm mandated by the CAEP interoperability profile 1.0 §2.6.
     *
     * @see <a href="https://openid.github.io/sharedsignals/openid-caep-interoperability-profile-1_0.html#section-2.6">CAEP Interoperability Profile §2.6</a>
     */
    public static final Set<String> DEFAULT_EXPECTED_SIGNATURE_ALGORITHMS = Set.of(Algorithm.RS256);

    /**
     * Known JWS asymmetric signing algorithm names accepted as entries of
     * {@link #EXPECTED_SIGNATURE_ALGORITHMS}. Excludes symmetric HMAC
     * families (HS*) because SET verification on the receiver uses the
     * transmitter's public keys loaded via JWKS.
     */
    private static final Set<String> SUPPORTED_JWS_ALGORITHMS = Set.of(
            Algorithm.RS256, Algorithm.RS384, Algorithm.RS512,
            Algorithm.PS256, Algorithm.PS384, Algorithm.PS512,
            Algorithm.ES256, Algorithm.ES384, Algorithm.ES512,
            Algorithm.EdDSA);

    public SsfReceiverRegistrationProviderConfig() {
    }

    public SsfReceiverRegistrationProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getIssuer() {
        return getConfig().get(ISSUER);
    }

    public void setIssuer(String issuer) {
        getConfig().put(ISSUER, issuer);
    }

    public String getDescription() {
        return getConfig().get(DESCRIPTION);
    }

    public void setDescription(String description) {
        getConfig().put(DESCRIPTION, description);
    }

    public String getTransmitterToken() {
        return getConfig().get(TRANSMITTER_TOKEN);
    }

    public void setTransmitterToken(String transmitterToken) {
        getConfig().put(TRANSMITTER_TOKEN, transmitterToken);
    }

    public TransmitterTokenType getTransmitterTokenType() {
        String value = getConfig().get(TRANSMITTER_TOKEN_TYPE);
        if (value == null) {
            return TransmitterTokenType.ACCESS_TOKEN;
        }
        return TransmitterTokenType.valueOf(value);
    }

    public void setTransmitterTokenType(TransmitterTokenType transmitterTokenType) {
        getConfig().put(TRANSMITTER_TOKEN_TYPE, transmitterTokenType.name());
    }

    public String getPushAuthorizationHeader() {
        return getConfig().get(PUSH_AUTHORIZATION_HEADER);
    }

    public void setPushAuthorizationHeader(String pushAuthorizationHeader) {
        getConfig().put(PUSH_AUTHORIZATION_HEADER, pushAuthorizationHeader);
    }

    public String getStreamId() {
        return getConfig().get(STREAM_ID);
    }

    public void setStreamId(String streamId) {
        getConfig().put(STREAM_ID, streamId);
    }

    public String getTransmitterMetadataUrl() {
        return getConfig().get(TRANSMITTER_METADATA_URL);
    }

    public void setTransmitterMetadataUrl(String transmitterMetadataUrl) {
        getConfig().put(TRANSMITTER_METADATA_URL, transmitterMetadataUrl);
    }

    public String getStreamAudience() {
        return getConfig().get(STREAM_AUDIENCE);
    }

    public void setStreamAudience(String streamAudience) {
        getConfig().put(STREAM_AUDIENCE, streamAudience);
    }

    public Set<String> streamAudience() {
        String streamAudience = getStreamAudience();
        if (streamAudience == null || streamAudience.isBlank()) {
            return null;
        }
        return Arrays.stream(streamAudience.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public TransmitterAuthMethod getTransmitterAuthMethod() {
        String value = getConfig().get(TRANSMITTER_AUTH_METHOD);
        if (value == null) {
            return TransmitterAuthMethod.STATIC_TOKEN;
        }
        return TransmitterAuthMethod.valueOf(value);
    }

    public void setTransmitterAuthMethod(TransmitterAuthMethod transmitterAuthMethod) {
        getConfig().put(TRANSMITTER_AUTH_METHOD, transmitterAuthMethod.name());
    }

    public String getClientId() {
        return getConfig().get(CLIENT_ID);
    }

    public void setClientId(String clientId) {
        getConfig().put(CLIENT_ID, clientId);
    }

    public String getClientSecret() {
        return getConfig().get(CLIENT_SECRET);
    }

    public void setClientSecret(String clientSecret) {
        getConfig().put(CLIENT_SECRET, clientSecret);
    }

    public String getClientAuthMethod() {
        String value = getConfig().get(CLIENT_AUTH_METHOD);
        if (value == null) {
            return "client_secret_post";
        }
        return value;
    }

    public void setClientAuthMethod(String clientAuthMethod) {
        getConfig().put(CLIENT_AUTH_METHOD, clientAuthMethod);
    }

    public String getTokenUrl() {
        return getConfig().get(TOKEN_URL);
    }

    public void setTokenUrl(String tokenUrl) {
        getConfig().put(TOKEN_URL, tokenUrl);
    }

    public String getScope() {
        return getConfig().get(SCOPE);
    }

    public void setScope(String scope) {
        getConfig().put(SCOPE, scope);
    }

    public DeliveryMethod getDeliveryMethod() {
        String value = getConfig().get(DELIVERY_METHOD);
        if (value == null) {
            return DeliveryMethod.PUSH;
        }
        return DeliveryMethod.valueOf(value);
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        getConfig().put(DELIVERY_METHOD, deliveryMethod.name());
    }

    /**
     * Returns the allow-list of JWS signature algorithms this receiver will
     * accept on inbound Security Event Tokens. The decoder rejects any SET
     * whose JWS header {@code alg} is not in this set before any key lookup
     * or signer construction, closing the classic JWS alg-confusion surface.
     *
     * <p>When the config value is unset or blank, returns
     * {@link #DEFAULT_EXPECTED_SIGNATURE_ALGORITHMS} — {@code {RS256}} per
     * CAEP interoperability profile 1.0 §2.6. The set form is deliberate:
     * operators may widen it during a transmitter-side key/algorithm
     * rotation window.
     */
    public Set<String> getExpectedSignatureAlgorithms() {
        String raw = getConfig().get(EXPECTED_SIGNATURE_ALGORITHMS);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_EXPECTED_SIGNATURE_ALGORITHMS;
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void setExpectedSignatureAlgorithms(Set<String> expectedSignatureAlgorithms) {
        if (expectedSignatureAlgorithms == null || expectedSignatureAlgorithms.isEmpty()) {
            getConfig().remove(EXPECTED_SIGNATURE_ALGORITHMS);
            return;
        }
        getConfig().put(EXPECTED_SIGNATURE_ALGORITHMS, String.join(",", expectedSignatureAlgorithms));
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);

        TransmitterAuthMethod authMethod = getTransmitterAuthMethod();
        if (authMethod == TransmitterAuthMethod.CLIENT_CREDENTIALS) {
            if (isBlank(getTokenUrl())) {
                throw new IllegalArgumentException("tokenUrl is required when using CLIENT_CREDENTIALS authentication");
            }
            if (isBlank(getClientId())) {
                throw new IllegalArgumentException("clientId is required when using CLIENT_CREDENTIALS authentication");
            }
            if (isBlank(getClientSecret())) {
                throw new IllegalArgumentException("clientSecret is required when using CLIENT_CREDENTIALS authentication");
            }
        } else {
            if (isBlank(getTransmitterToken())) {
                throw new IllegalArgumentException("transmitterToken is required when using STATIC_TOKEN authentication");
            }
        }

        validateExpectedSignatureAlgorithms();
    }

    /**
     * Fails fast at receiver registration time when the
     * {@link #EXPECTED_SIGNATURE_ALGORITHMS} allow-list is empty or contains
     * an unrecognized JWS algorithm name. The "does the realm actually have
     * a verifier for this alg" check is deferred to the decoder, because
     * {@link #validate(RealmModel)} has no {@code KeycloakSession} handle
     * and looking up providers via a thread-local here would add
     * session-binding to a config-only code path.
     */
    protected void validateExpectedSignatureAlgorithms() {
        String raw = getConfig().get(EXPECTED_SIGNATURE_ALGORITHMS);
        if (raw == null || raw.isBlank()) {
            // Unset falls back to DEFAULT_EXPECTED_SIGNATURE_ALGORITHMS at
            // read time; nothing to validate.
            return;
        }
        Set<String> expected = getExpectedSignatureAlgorithms();
        if (expected.isEmpty()) {
            throw new IllegalArgumentException(
                    "expectedSignatureAlgorithms must not be empty — defaults to " + DEFAULT_EXPECTED_SIGNATURE_ALGORITHMS
                            + " when unset.");
        }
        for (String alg : expected) {
            if (!SUPPORTED_JWS_ALGORITHMS.contains(alg)) {
                throw new IllegalArgumentException(
                        "expectedSignatureAlgorithms entry " + alg + " is not a recognized JWS asymmetric signing algorithm"
                                + " (supported: " + SUPPORTED_JWS_ALGORITHMS + ")");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static enum TransmitterAuthMethod {
        STATIC_TOKEN,
        CLIENT_CREDENTIALS,
    }

    public static enum TransmitterTokenType {
        ACCESS_TOKEN,
        // TODO add support for refresh token
        // REFRESH_TOKEN
    }

    public static enum DeliveryMethod {
        PUSH,
        // we might support polling in the future
        // POLL,
    }
}
