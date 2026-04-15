package org.keycloak.ssf.transmitter;

import java.util.LinkedHashSet;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.SsfEvent;
import org.keycloak.ssf.event.SsfEventRegistry;
import org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.ssf.transmitter.metadata.TransmitterMetadataService;
import org.keycloak.ssf.transmitter.resources.SsfStreamManagementResource;
import org.keycloak.ssf.transmitter.resources.SsfStreamStatusResource;
import org.keycloak.ssf.transmitter.resources.SsfStreamVerificationResource;
import org.keycloak.ssf.transmitter.stream.StreamService;
import org.keycloak.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;

import org.jboss.logging.Logger;

public class DefaultSsfTransmitterProvider implements SsfTransmitterProvider {

    protected static final Logger LOG = Logger.getLogger(DefaultSsfTransmitterProvider.class);

    protected final KeycloakSession session;

    protected final StreamVerificationService verificationService;

    protected final SecurityEventTokenMapper securityEventTokenMapper;

    protected final SecurityEventTokenDispatcher securityEventTokenDispatcher;

    protected final TransmitterMetadataService transmitterService;

    private final SsfTransmitterConfig transmitterConfig;

    /**
     * Aliases (or full URIs) of the events this transmitter advertises as
     * the default supported set for receivers that do not opt into their
     * own list. When {@code null}, {@link #getDefaultSupportedEvents()}
     * falls back to every event type known to the registry — which picks
     * up custom SPI-contributed events automatically.
     */
    private final Set<String> configuredDefaultSupportedEventAliases;

    public DefaultSsfTransmitterProvider(KeycloakSession session,
                                         TransmitterMetadataService transmitterMetadataService,
                                         StreamVerificationService verificationService,
                                         SecurityEventTokenMapper securityEventTokenMapper,
                                         SecurityEventTokenDispatcher securityEventTokenDispatcher,
                                         SsfTransmitterConfig transmitterConfig,
                                         Set<String> configuredDefaultSupportedEventAliases) {
        this.session = session;
        this.transmitterService = transmitterMetadataService;
        this.verificationService = verificationService;
        this.securityEventTokenMapper = securityEventTokenMapper;
        this.securityEventTokenDispatcher = securityEventTokenDispatcher;
        this.transmitterConfig = transmitterConfig;
        this.configuredDefaultSupportedEventAliases = configuredDefaultSupportedEventAliases;
    }

    @Override
    public StreamVerificationService verificationService() {
        return verificationService;
    }

    @Override
    public TransmitterMetadataService metadataService() {
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
    public SsfStreamManagementResource streamManagementEndpoint() {
        return new SsfStreamManagementResource(streamService());
    }

    @Override
    public StreamService streamService() {
        return new StreamService(new ClientStreamStore(session), transmitterService);
    }

    @Override
    public SsfStreamStatusResource streamStatusEndpoint() {
        return new SsfStreamStatusResource(streamService());
    }

    @Override
    public SsfStreamVerificationResource verificationEndpoint() {
        return new SsfStreamVerificationResource(verificationService);
    }

    @Override
    public Set<String> getDefaultSupportedEvents() {
        SsfEventRegistry registry = registry();
        if (configuredDefaultSupportedEventAliases == null) {
            // No explicit SPI configuration — advertise only the events
            // the transmitter can actually emit, as declared by every
            // registered SsfEventProviderFactory#getEmittableEventTypes.
            // This naturally picks up events contributed by custom SPI
            // extensions that also wire up an emission path.
            return registry.getEmittableEventTypes();
        }

        // SPI config is set — resolve each configured entry (which may be
        // either an alias or a full URI) to its canonical event type URI
        // via the registry. Unknown entries are silently dropped.
        Set<String> resolved = new LinkedHashSet<>();
        for (String candidate : configuredDefaultSupportedEventAliases) {
            String eventType = registry.resolveEventTypeForAlias(candidate);
            if (eventType == null && registry.getEventClassByType(candidate).isPresent()) {
                eventType = candidate;
            }
            if (eventType != null) {
                resolved.add(eventType);
            }
        }
        return resolved;
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
    public Set<String> getEmittableEventAliases() {
        SsfEventRegistry registry = registry();
        Set<String> aliases = new LinkedHashSet<>();
        for (String eventType : registry.getEmittableEventTypes()) {
            String alias = registry.resolveAliasForEventType(eventType);
            aliases.add(alias != null ? alias : eventType);
        }
        return aliases;
    }

    protected SsfEventRegistry registry() {
        return Ssf.events().getRegistry();
    }

    @Override
    public SsfTransmitterConfig getConfig() {
        return transmitterConfig;
    }
}
