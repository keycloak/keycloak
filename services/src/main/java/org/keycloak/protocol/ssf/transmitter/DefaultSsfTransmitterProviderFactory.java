package org.keycloak.protocol.ssf.transmitter;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ssf.support.SsfUtil;
import org.keycloak.protocol.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.protocol.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.protocol.ssf.transmitter.metadata.SsfTransmitterMetadataService;
import org.keycloak.protocol.ssf.transmitter.stream.StreamService;
import org.keycloak.protocol.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.protocol.ssf.transmitter.stream.StreamVerificationService;

public class DefaultSsfTransmitterProviderFactory implements SsfTransmitterProviderFactory {

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public SsfTransmitterProvider create(KeycloakSession session) {
        var transmitterService = new SsfTransmitterMetadataService(session);
        var streamStore = new ClientStreamStore(session);
        var streamService = new StreamService(streamStore, transmitterService);
        var issuerUrl = SsfUtil.getIssuerUrl(session);
        var mapper = new SecurityEventTokenMapper(issuerUrl);
        var securityEventTokenEncoder = new SecurityEventTokenEncoder(session);
        var pushDeliveryService = new PushDeliveryService(session);
        var dispatcher = new SecurityEventTokenDispatcher(streamService, securityEventTokenEncoder, pushDeliveryService);
        var verificationService = new StreamVerificationService(streamStore, dispatcher);

        return new DefaultSsfTransmitterProvider(session, transmitterService, verificationService, mapper, dispatcher);
    }

    @Override
    public void init(Config.Scope config) {

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
