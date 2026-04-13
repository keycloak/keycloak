package org.keycloak.protocol.ssf.transmitter;

import java.util.List;
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
import org.keycloak.protocol.ssf.transmitter.metadata.TransmitterMetadataService;
import org.keycloak.protocol.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.protocol.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class DefaultSsfTransmitterProviderFactory implements SsfTransmitterProviderFactory {

    protected Set<String> defaultSupportedEvents;

    protected SsfTransmitterConfig transmitterConfig = SsfTransmitterConfig.defaults();

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public SsfTransmitterProvider create(KeycloakSession session) {
        var mapper = new SecurityEventTokenMapper(SsfUtil.getIssuerUrl(session));
        var dispatcher = new SecurityEventTokenDispatcher(session, new SecurityEventTokenEncoder(session), new PushDeliveryService(session));
        var verificationService = new StreamVerificationService(new ClientStreamStore(session), mapper, dispatcher);

        return new DefaultSsfTransmitterProvider(session, new TransmitterMetadataService(session), verificationService, mapper, dispatcher, getTransmitterConfig());
    }

    @Override
    public void init(Config.Scope config) {

        Set<String> defaultSupportedEvents = extractSupportedEvents(config);
        if (defaultSupportedEvents != null) {
            this.defaultSupportedEvents = defaultSupportedEvents;
        }

        this.transmitterConfig = createTransmitterConfig(config);
    }

    protected SsfTransmitterConfig createTransmitterConfig(Config.Scope config) {
        return new SsfTransmitterConfig(config);
    }

    @Override
    public SsfTransmitterConfig getTransmitterConfig() {
        return transmitterConfig;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(SsfTransmitterConfig.CONFIG_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS)
                .type("int")
                .helpText("Default connect timeout in milliseconds for delivering SSF events via HTTP push to a receiver's push endpoint.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS)
                .type("int")
                .helpText("Default socket (read) timeout in milliseconds for delivering SSF events via HTTP push to a receiver's push endpoint.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS)
                .type("int")
                .helpText("Delay in milliseconds before the transmitter dispatches a verification event after a stream is created or updated.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS)
                .add()
                .build();
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
