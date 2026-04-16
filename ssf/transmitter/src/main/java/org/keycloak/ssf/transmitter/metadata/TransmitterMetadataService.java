package org.keycloak.ssf.transmitter.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.transmitter.SsfTransmitterUrls;
import org.keycloak.ssf.transmitter.support.SsfUtil;

/**
 * Service for managing the SSF transmitter functionality.
 */
public class TransmitterMetadataService {

    protected final KeycloakSession session;

    protected  final Function<KeycloakSession, String> issuerGenerator;

    public TransmitterMetadataService(KeycloakSession session, Function<KeycloakSession, String> issuerGenerator) {
        this.session = session;
        this.issuerGenerator = issuerGenerator;
    }

    /**
     * Returns the SSF transmitter configuration metadata.
     *
     * @return The SSF transmitter configuration metadata
     */
    public TransmitterMetadata getTransmitterMetadata() {

        TransmitterMetadata cached = (TransmitterMetadata)session.getAttribute("ssfTransmitterMetadata");
        if (cached != null) {
            return cached;
        }

        TransmitterMetadata transmitterMetadata = createTransmitterMetadata();
        session.setAttribute("ssfTransmitterMetadata", transmitterMetadata);

        return transmitterMetadata;
    }

    protected TransmitterMetadata createTransmitterMetadata() {

        TransmitterMetadata metadata = new TransmitterMetadata();

        metadata.setSpecVersion("1_0");

        String issuerUrl = issuerGenerator.apply(session);

        metadata.setIssuer(issuerUrl);
        metadata.setJwksUri(issuerUrl + "/protocol/openid-connect/certs");
        metadata.setDeliveryMethodSupported(Set.of( //
                // PUSH
                Ssf.DELIVERY_METHOD_PUSH_URI,
                // RISC PUSH URL for apple business manager
                Ssf.DELIVERY_METHOD_RISC_PUSH_URI
                // POLL
                // "urn:ietf:rfc:8936",
        ));

        // Set endpoints
        metadata.setConfigurationEndpoint(SsfTransmitterUrls.streamsEndpoint(issuerUrl));
        metadata.setStatusEndpoint(SsfTransmitterUrls.streamStatusEndpoint(issuerUrl));
        metadata.setVerificationEndpoint(SsfTransmitterUrls.streamVerificationEndpoint(issuerUrl));

        // Set authorization schemes
        metadata.setAuthorizationSchemes(createAuthorizationSchemes());

        metadata.setDefaultSubjects("ALL");

        return metadata;
    }

    protected List<Map<String, Object>> createAuthorizationSchemes() {
        return Collections.singletonList(createOAuthAuthorizationScheme());
    }

    protected Map<String, Object> createOAuthAuthorizationScheme() {
        Map<String, Object> oauthScheme = new HashMap<>();
        oauthScheme.put("spec_urn", "urn:ietf:rfc:6749");
        return oauthScheme;
    }
}
