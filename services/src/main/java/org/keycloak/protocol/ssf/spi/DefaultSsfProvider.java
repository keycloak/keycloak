package org.keycloak.protocol.ssf.spi;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.endpoint.SsfPushDeliveryResource;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.listener.DefaultSsfEventListener;
import org.keycloak.protocol.ssf.event.listener.SsfEventListener;
import org.keycloak.protocol.ssf.event.parser.DefaultSsfSecurityEventTokenParser;
import org.keycloak.protocol.ssf.event.parser.SsfSecurityEventTokenParser;
import org.keycloak.protocol.ssf.event.processor.DefaultSsfSecurityEventProcessor;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventProcessor;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.transmitter.DefaultSsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.DefaultSsfStreamSsfStreamVerificationStore;
import org.keycloak.protocol.ssf.receiver.verification.DefaultSsfVerificationClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.protocol.ssf.receiver.verification.SsfVerificationClient;

public class DefaultSsfProvider implements SsfProvider {

    protected final KeycloakSession session;

    protected SsfSecurityEventTokenParser securityEventTokenParser;

    protected SsfSecurityEventProcessor eventProcessor;

    protected SsfEventListener eventListener;

    protected SsfPushDeliveryResource pushDeliveryEndpoint;

    protected SsfVerificationClient securityEventsVerifier;

    protected SsfStreamVerificationStore verificationStore;

    protected SsfTransmitterClient transmitterClient;

    protected SsfVerificationClient verificationClient;

    public DefaultSsfProvider(KeycloakSession session) {
        this.session = session;
    }

    protected SsfSecurityEventTokenParser getSsfEventParser() {
        if (securityEventTokenParser == null) {
            securityEventTokenParser = new DefaultSsfSecurityEventTokenParser(session);
        }
        return securityEventTokenParser;
    }

    protected SsfSecurityEventProcessor getSecurityEventProcessor() {
        if (eventProcessor == null) {
            eventProcessor = new DefaultSsfSecurityEventProcessor(
                    this,
                    getEventListener(),
                    getVerificationStore()
            );
        }
        return eventProcessor;
    }

    protected SsfPushDeliveryResource getPushEndpoint() {
        if (pushDeliveryEndpoint == null) {
            pushDeliveryEndpoint = new SsfPushDeliveryResource(this);
        }
        return pushDeliveryEndpoint;
    }

    protected SsfEventListener getEventListener() {
        if (eventListener == null) {
            eventListener = new DefaultSsfEventListener(session);
        }
        return eventListener;
    }

    protected SsfVerificationClient getSecurityEventsVerifier() {
        if (securityEventsVerifier == null) {
            securityEventsVerifier = new DefaultSsfVerificationClient(session);
        }
        return securityEventsVerifier;
    }

    protected SsfTransmitterClient getTransmitterClient() {
        if (transmitterClient == null) {
            transmitterClient = new DefaultSsfTransmitterClient(session);
        }
        return transmitterClient;
    }

    @Override
    public SsfVerificationClient verificationClient() {
        return getVerificationClient();
    }

    protected SsfVerificationClient getVerificationClient() {
        if (verificationClient == null) {
            verificationClient = new DefaultSsfVerificationClient(session);
        }
        return verificationClient;
    }

    @Override
    public SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfSecurityEventContext securityEventContext) {
        var parser = getSsfEventParser();
        return parser.parseSecurityEventToken(encodedSecurityEventToken, securityEventContext.getReceiver());
    }

    @Override
    public void processSecurityEvents(SsfSecurityEventContext securityEventContext) {
        eventProcessor().processSecurityEvents(securityEventContext);
    }

    @Override
    public SsfStreamVerificationStore verificationStore() {
        return getVerificationStore();
    }

    protected SsfStreamVerificationStore getVerificationStore() {
        if (verificationStore == null) {
            verificationStore = new DefaultSsfStreamSsfStreamVerificationStore(session);
        }
        return verificationStore;
    }

    public SsfSecurityEventProcessor eventProcessor() {
        return getSecurityEventProcessor();
    }

    @Override
    public SsfPushDeliveryResource pushDeliveryEndpoint() {
        return getPushEndpoint();
    }

    @Override
    public SsfTransmitterClient transmitterClient() {
        return getTransmitterClient();
    }

    @Override
    public SsfSecurityEventContext createSecurityEventContext(SecurityEventToken securityEventToken, SsfReceiver receiver) {

        SsfSecurityEventContext context = new SsfSecurityEventContext();
        context.setSecurityEventToken(securityEventToken);
        context.setSession(session);
        context.setReceiver(receiver);

        return context;
    }

}
