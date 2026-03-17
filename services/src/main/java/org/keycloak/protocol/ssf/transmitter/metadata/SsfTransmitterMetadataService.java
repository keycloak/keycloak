package org.keycloak.protocol.ssf.transmitter.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.caep.CaepCredentialChange;
import org.keycloak.protocol.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.protocol.ssf.support.SsfUtil;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;

/**
 * Service for managing the SSF transmitter functionality.
 */
public class SsfTransmitterMetadataService {

    private final KeycloakSession session;

    public SsfTransmitterMetadataService(KeycloakSession session) {
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

    public Set<String> getEventsDelivered(StreamConfig streamConfig, Set<String> eventsRequested) {

        // TODO compute events delivered for current realm
        Set<String> eventsDelivered = new HashSet<>(eventsRequested);
        eventsDelivered.retainAll(getSupportedEvents());

        return eventsDelivered;
    }

    public Set<String> getSupportedEvents() {

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.getContext().getClient();

        // TODO compute supported events for current realm
        return Set.of(CaepCredentialChange.TYPE, CaepSessionRevoked.TYPE);
    }
}
