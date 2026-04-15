package org.keycloak.ssf.transmitter;

import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.ssf.event.SsfEventProviderFactory;
import org.keycloak.ssf.event.SsfEventRegistry;
import org.keycloak.ssf.transmitter.support.SsfUtil;
import org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.ssf.transmitter.metadata.TransmitterMetadataService;
import org.keycloak.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class DefaultSsfTransmitterProviderFactory implements SsfTransmitterProviderFactory {

    public static final String CONFIG_SUPPORTED_EVENTS = "supported-events";

    /**
     * Aliases (or full URIs) of the events the transmitter advertises as
     * "default supported events" for a receiver client that does not set
     * its own {@code ssf.supportedEvents} attribute. Sourced from the
     * {@code supported-events} SPI property. When {@code null} (i.e. the
     * property is unset), the provider falls back to every event type
     * known to the
     * {@link SsfEventRegistry}, which
     * includes events contributed by custom
     * {@link SsfEventProviderFactory}
     * implementations.
     */
    protected Set<String> configuredDefaultSupportedEventAliases;

    protected SsfTransmitterConfig transmitterConfig = SsfTransmitterConfig.defaults();

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public SsfTransmitterProvider create(KeycloakSession session) {
        var transmitterConfig = getTransmitterConfig();
        var mapper = new SecurityEventTokenMapper(session, SsfUtil.getIssuerUrl(session), transmitterConfig);
        var encoder = new SecurityEventTokenEncoder(session);
        var pushDelivery = new PushDeliveryService(session, transmitterConfig);
        var dispatcher = new SecurityEventTokenDispatcher(session, encoder, pushDelivery, transmitterConfig);
        var verificationService = new StreamVerificationService(session, new ClientStreamStore(session), mapper, dispatcher);

        return new DefaultSsfTransmitterProvider(session, new TransmitterMetadataService(session), verificationService, mapper, dispatcher, transmitterConfig, configuredDefaultSupportedEventAliases);
    }

    @Override
    public void init(Config.Scope config) {

        this.configuredDefaultSupportedEventAliases = extractSupportedEvents(config);
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
                .property()
                .name(SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS)
                .type("int")
                .helpText("Minimum amount of time in seconds that must pass between receiver-initiated verification requests. Requests within this window are rejected with HTTP 429. Set to 0 to disable rate limiting.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_MIN_VERIFICATION_INTERVAL_SECONDS)
                .add()
                .property()
                .name(CONFIG_SUPPORTED_EVENTS)
                .type("string")
                .helpText("Comma-separated list of event aliases or full event type URIs that the transmitter advertises as the default supported event set for receiver clients that do not configure their own ssf.supportedEvents attribute. When unset, every event type registered via SsfEventProviderFactory is advertised.")
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_SIGNATURE_ALGORITHM)
                .type("string")
                .helpText("Default JWS signature algorithm used to sign outgoing SSF Security Event Tokens when a receiver client does not configure its own ssf.signatureAlgorithm attribute. Defaults to RS256 per the CAEP interoperability profile 1.0 §2.6.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_SIGNATURE_ALGORITHM)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_USER_SUBJECT_FORMAT)
                .type("string")
                .helpText("Default subject identifier format for the user portion of outgoing SSF Security Event Tokens when a receiver client does not configure its own ssf.userSubjectFormat attribute. Defaults to iss_sub (realm issuer + user ID). Allowed values: iss_sub, email.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_USER_SUBJECT_FORMAT)
                .add()
                .build();
    }

    /**
     * Parses the {@code supported-events} SPI property into a set of
     * aliases (or full URIs). Returns {@code null} when the property is
     * unset or blank so the provider can fall back to the full
     * {@link SsfEventRegistry}.
     */
    protected Set<String> extractSupportedEvents(Config.Scope config) {
        String defaultSupportedEventsString = config.get(CONFIG_SUPPORTED_EVENTS);

        if (defaultSupportedEventsString == null || defaultSupportedEventsString.isBlank()) {
            return null;
        }

        return parseSupportedEvents(defaultSupportedEventsString);
    }

    protected Set<String> parseSupportedEvents(String supportedEventsString) {
        return SsfUtil.parseEventTypeAliases(supportedEventsString);
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
