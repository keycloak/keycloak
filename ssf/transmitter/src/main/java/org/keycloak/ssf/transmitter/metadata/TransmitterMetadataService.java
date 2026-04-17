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
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;

/**
 * Service for managing the SSF transmitter functionality.
 */
public class TransmitterMetadataService {

    protected final KeycloakSession session;

    protected  final Function<KeycloakSession, String> issuerGenerator;

    protected final SsfTransmitterConfig transmitterConfig;

    public TransmitterMetadataService(KeycloakSession session,
                                      Function<KeycloakSession, String> issuerGenerator,
                                      SsfTransmitterConfig transmitterConfig) {
        this.session = session;
        this.issuerGenerator = issuerGenerator;
        this.transmitterConfig = transmitterConfig;
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

        metadata.setSpecVersion(Ssf.SSF_VERSION_1_0);

        String issuerUrl = issuerGenerator.apply(session);

        metadata.setIssuer(issuerUrl);
        metadata.setJwksUri(createJwksUri(issuerUrl));
        metadata.setDeliveryMethodSupported(Set.of( //
                // PUSH
                Ssf.DELIVERY_METHOD_PUSH_URI,
                // RISC PUSH URL for apple business manager
                Ssf.DELIVERY_METHOD_RISC_PUSH_URI
                // POLL
                // "urn:ietf:rfc:8936",
        ));

        // Stream management endpoints
        metadata.setConfigurationEndpoint(SsfTransmitterUrls.getStreamsEndpointUrl(issuerUrl));
        metadata.setStatusEndpoint(SsfTransmitterUrls.getStreamStatusEndpointUrl(issuerUrl));
        metadata.setVerificationEndpoint(SsfTransmitterUrls.getStreamVerificationEndpointUrl(issuerUrl));

        // Subject management endpoints (only advertised when enabled)
        if (transmitterConfig.isSubjectManagementEnabled()) {
            metadata.setAddSubjectEndpoint(SsfTransmitterUrls.getAddSubjectEndpointUrl(issuerUrl));
            metadata.setRemoveSubjectEndpoint(SsfTransmitterUrls.getRemoveSubjectEndpointUrl(issuerUrl));
        }

        metadata.setAuthorizationSchemes(createAuthorizationSchemes());

        metadata.setDefaultSubjects(transmitterConfig.getDefaultSubjects().name());

        return metadata;
    }

    protected String createJwksUri(String issuerUrl) {
        return issuerUrl + "/protocol/openid-connect/certs";
    }

    protected List<Map<String, Object>> createAuthorizationSchemes() {
        return Collections.singletonList(createOAuthAuthorizationScheme());
    }

    protected Map<String, Object> createOAuthAuthorizationScheme() {
        Map<String, Object> oauthScheme = new HashMap<>();
        oauthScheme.put("spec_urn", Ssf.SSF_OAUTH_AUTHORIZATION_SCHEME_URN);
        return oauthScheme;
    }
}
