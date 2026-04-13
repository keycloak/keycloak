package org.keycloak.protocol.ssf.transmitter;

import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.SsfEvent;
import org.keycloak.protocol.ssf.event.SsfEventRegistry;
import org.keycloak.protocol.ssf.event.caep.CaepCredentialChange;
import org.keycloak.protocol.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.protocol.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.protocol.ssf.transmitter.metadata.SsfTransmitterMetadataService;
import org.keycloak.protocol.ssf.transmitter.resources.StreamManagementResource;
import org.keycloak.protocol.ssf.transmitter.resources.StreamStatusResource;
import org.keycloak.protocol.ssf.transmitter.resources.StreamVerificationResource;
import org.keycloak.protocol.ssf.transmitter.stream.StreamService;
import org.keycloak.protocol.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.protocol.ssf.transmitter.stream.storage.client.ClientStreamStore;

import org.jboss.logging.Logger;

public class DefaultSsfTransmitterProvider implements SsfTransmitterProvider {

    protected static final Logger LOG = Logger.getLogger(DefaultSsfTransmitterProvider.class);

    protected final KeycloakSession session;

    protected final StreamVerificationService verificationService;

    protected final SecurityEventTokenMapper securityEventTokenMapper;

    protected final SecurityEventTokenDispatcher securityEventTokenDispatcher;

    protected final SsfTransmitterMetadataService transmitterService;

    public DefaultSsfTransmitterProvider(KeycloakSession session,
                                         SsfTransmitterMetadataService transmitterService,
                                         StreamVerificationService verificationService,
                                         SecurityEventTokenMapper securityEventTokenMapper,
                                         SecurityEventTokenDispatcher securityEventTokenDispatcher) {
        this.session = session;
        this.transmitterService = transmitterService;
        this.verificationService = verificationService;
        this.securityEventTokenMapper = securityEventTokenMapper;
        this.securityEventTokenDispatcher = securityEventTokenDispatcher;
    }

    @Override
    public StreamVerificationService verificationService() {
        return verificationService;
    }

    @Override
    public SsfTransmitterMetadataService transmitterService() {
        return transmitterService;
    }

    @Override
    public SecurityEventTokenMapper securityEventTokenMapper() {
        return securityEventTokenMapper;
    }

    @Override
    public SecurityEventTokenDispatcher securityEventTokenDispatcher() {
        return securityEventTokenDispatcher;
    }

    @Override
    public StreamManagementResource streamManagementEndpoint() {
        return new StreamManagementResource(streamService());
    }

    @Override
    public StreamService streamService() {
        return new StreamService(new ClientStreamStore(session), transmitterService);
    }

    @Override
    public StreamStatusResource streamStatusEndpoint() {
        return new StreamStatusResource(streamService());
    }

    @Override
    public StreamVerificationResource verificationEndpoint() {
        return new StreamVerificationResource(verificationService);
    }

    @Override
    public Set<String> getDefaultSupportedEvents() {
        return Set.of(CaepCredentialChange.TYPE, CaepSessionRevoked.TYPE);
    }

    @Override
    public Class<? extends SsfEvent> resolveSupportedEventType(String supportedEvent) {
        return registry().resolveEventClass(supportedEvent);
    }

    @Override
    public String resolveAliasForEventType(String eventType) {
        return registry().resolveAliasForEventType(eventType);
    }

    @Override
    public Set<String> getKnownEventAliases() {
        return registry().getKnownAliases();
    }

    protected SsfEventRegistry registry() {
        return Ssf.events().getRegistry();
    }
}
