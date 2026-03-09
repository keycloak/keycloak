package org.keycloak.protocol.ssf.receiver.spi;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.receiver.event.listener.DefaultSsfReceiverEventListener;
import org.keycloak.protocol.ssf.receiver.event.listener.SsfReceiverEventListener;
import org.keycloak.protocol.ssf.receiver.event.parser.DefaultSsfSecurityEventTokenParser;
import org.keycloak.protocol.ssf.receiver.event.parser.SsfSecurityEventTokenParser;
import org.keycloak.protocol.ssf.receiver.event.processor.DefaultSsfEventProcessor;
import org.keycloak.protocol.ssf.receiver.event.processor.SsfEventContext;
import org.keycloak.protocol.ssf.receiver.event.processor.SsfEventProcessor;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.transmitter.DefaultSsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.DefaultSsfStreamVerificationStore;
import org.keycloak.protocol.ssf.receiver.verification.DefaultSsfVerificationClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.protocol.ssf.receiver.verification.SsfVerificationClient;

public class DefaultSsfReceiverProvider implements SsfReceiverProvider {

    protected final KeycloakSession session;

    protected SsfSecurityEventTokenParser securityEventTokenParser;

    protected SsfEventProcessor eventProcessor;

    protected SsfReceiverEventListener eventListener;

    protected SsfStreamVerificationStore verificationStore;

    protected SsfTransmitterClient transmitterClient;

    protected SsfVerificationClient verificationClient;

    public DefaultSsfReceiverProvider(KeycloakSession session) {
        this.session = session;
    }

    protected SsfSecurityEventTokenParser getSsfEventParser() {
        if (securityEventTokenParser == null) {
            securityEventTokenParser = new DefaultSsfSecurityEventTokenParser(session);
        }
        return securityEventTokenParser;
    }

    protected SsfEventProcessor getSecurityEventProcessor() {
        if (eventProcessor == null) {
            eventProcessor = new DefaultSsfEventProcessor(
                    getEventListener(),
                    getVerificationStore()
            );
        }
        return eventProcessor;
    }

    protected SsfReceiverEventListener getEventListener() {
        if (eventListener == null) {
            eventListener = new DefaultSsfReceiverEventListener(session);
        }
        return eventListener;
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
    public SsfSecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfEventContext eventContext) {
        var parser = getSsfEventParser();
        return parser.parseSecurityEventToken(encodedSecurityEventToken, eventContext.getReceiver());
    }

    @Override
    public void processEvents(SsfSecurityEventToken securityEventToken, SsfEventContext eventContext) {
        eventProcessor().processEvents(securityEventToken, eventContext);
    }

    @Override
    public SsfStreamVerificationStore verificationStore() {
        return getVerificationStore();
    }

    protected SsfStreamVerificationStore getVerificationStore() {
        if (verificationStore == null) {
            verificationStore = new DefaultSsfStreamVerificationStore(session);
        }
        return verificationStore;
    }

    public SsfEventProcessor eventProcessor() {
        return getSecurityEventProcessor();
    }

    @Override
    public SsfTransmitterClient transmitterClient() {
        return getTransmitterClient();
    }

    @Override
    public SsfEventContext createEventContext(SsfSecurityEventToken securityEventToken, SsfReceiver receiver) {

        SsfEventContext context = new SsfEventContext();
        context.setSecurityEventToken(securityEventToken);
        context.setSession(session);
        context.setReceiver(receiver);

        return context;
    }

}
