package org.keycloak.ssf.transmitter.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
        metadata.setDeliveryMethodSupported(createDeliveryMethods());

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

        // critical_subject_members tells a receiver which complex-subject
        // member keys (e.g. "user", "session", "tenant") it MUST be able
        // to interpret. Empty / null configured set omits the field.
        Set<String> critical = transmitterConfig.getCriticalSubjectMembers();
        if (critical != null && !critical.isEmpty()) {
            metadata.setCriticalSubjectMembers(new LinkedHashSet<>(critical));
        }

        return metadata;
    }

    protected Set<String> createDeliveryMethods() {
        // Spec-standard SSF 1.0 delivery methods are always advertised.
        // The RISC variants (Apple Business Manager / Apple School
        // Manager interop) are gated on the sse-caep-enabled SPI flag
        // so deployments that don't integrate with Apple-style
        // receivers can keep the advertised surface to the
        // spec-standard URIs only.
        Set<String> deliveryMethods = new LinkedHashSet<>();
        // PUSH (RFC 8935)
        deliveryMethods.add(Ssf.DELIVERY_METHOD_PUSH_URI);
        // POLL (RFC 8936)
        deliveryMethods.add(Ssf.DELIVERY_METHOD_POLL_URI);
        if (transmitterConfig.isSseCaepEnabled()) {
            // Legacy RISC PUSH URI (Apple Business Manager)
            deliveryMethods.add(Ssf.DELIVERY_METHOD_RISC_PUSH_URI);
            // Legacy RISC POLL URI
            deliveryMethods.add(Ssf.DELIVERY_METHOD_RISC_POLL_URI);
        }
        return deliveryMethods;
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
