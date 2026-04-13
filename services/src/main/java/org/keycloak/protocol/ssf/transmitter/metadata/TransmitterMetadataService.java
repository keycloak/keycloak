package org.keycloak.protocol.ssf.transmitter.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.support.SsfUtil;

/**
 * Service for managing the SSF transmitter functionality.
 */
public class TransmitterMetadataService {

    private final KeycloakSession session;

    public TransmitterMetadataService(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Returns the SSF transmitter configuration metadata.
     *
     * @return The SSF transmitter configuration metadata
     */
    public SsfTransmitterMetadata getTransmitterMetadata() {

        SsfTransmitterMetadata cached = (SsfTransmitterMetadata)session.getAttribute("ssfTransmitterMetadata");
        if (cached != null) {
            return cached;
        }

        SsfTransmitterMetadata transmitterMetadata = createTransmitterMetadata();
        session.setAttribute("ssfTransmitterMetadata", transmitterMetadata);

        return transmitterMetadata;
    }

    protected SsfTransmitterMetadata createTransmitterMetadata() {

        SsfTransmitterMetadata metadata = new SsfTransmitterMetadata();

        metadata.setSpecVersion("1_0");

        String issuerUrl = SsfUtil.getIssuerUrl(session);

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
        metadata.setConfigurationEndpoint(Ssf.streamsEndpoint(issuerUrl));
        metadata.setStatusEndpoint(Ssf.streamStatusEndpoint(issuerUrl));
        metadata.setVerificationEndpoint(Ssf.streamVerificationEndpoint(issuerUrl));

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
