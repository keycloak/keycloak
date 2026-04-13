package org.keycloak.protocol.ssf.transmitter;

import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ssf.event.caep.CaepCredentialChange;
import org.keycloak.protocol.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.protocol.ssf.support.SsfUtil;
import org.keycloak.protocol.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.protocol.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.protocol.ssf.transmitter.metadata.SsfTransmitterMetadataService;
import org.keycloak.protocol.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.protocol.ssf.transmitter.stream.storage.client.ClientStreamStore;

public class DefaultSsfTransmitterProviderFactory implements SsfTransmitterProviderFactory {

    protected Set<String> defaultSupportedEvents;

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public SsfTransmitterProvider create(KeycloakSession session) {
        var mapper = new SecurityEventTokenMapper(SsfUtil.getIssuerUrl(session));
        var dispatcher = new SecurityEventTokenDispatcher(session, new SecurityEventTokenEncoder(session), new PushDeliveryService(session));
        var verificationService = new StreamVerificationService(new ClientStreamStore(session), mapper, dispatcher);

        return new DefaultSsfTransmitterProvider(session, new SsfTransmitterMetadataService(session), verificationService, mapper, dispatcher);
    }

    @Override
    public void init(Config.Scope config) {

        Set<String> defaultSupportedEvents;

        defaultSupportedEvents = extractSupportedEvents(config);
        if (defaultSupportedEvents == null) return;

        this.defaultSupportedEvents = defaultSupportedEvents;
    }

    protected Set<String> extractSupportedEvents(Config.Scope config) {
        String defaultSupportedEventsString = config.get("supported-events", "CaepCredentialChange, CaepSessionRevoked");

        if (defaultSupportedEventsString == null || defaultSupportedEventsString.isBlank()) {
            return getDefaultSupportedEvents();
        }

        return parseSupportedEvents(defaultSupportedEventsString);
    }

    protected Set<String> parseSupportedEvents(String supportedEventsString) {
        return SsfUtil.parseEventTypeAliases(supportedEventsString);
    }

    protected Set<String> getDefaultSupportedEvents() {
        return Set.of(CaepCredentialChange.class.getSimpleName(), CaepSessionRevoked.class.getSimpleName());
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
        // NOOP
    }


    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }
}
