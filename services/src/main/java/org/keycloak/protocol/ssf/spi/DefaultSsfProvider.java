package org.keycloak.protocol.ssf.spi;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.endpoint.SsfPushDeliveryEndpoint;
import org.keycloak.protocol.ssf.event.listener.DefaultSsfEventListener;
import org.keycloak.protocol.ssf.event.listener.SsfEventListener;
import org.keycloak.protocol.ssf.event.parser.DefaultSsfSecurityEventTokenParser;
import org.keycloak.protocol.ssf.event.parser.SsfSecurityEventTokenParser;
import org.keycloak.protocol.ssf.event.processor.DefaultSsfEventProcessor;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;
import org.keycloak.protocol.ssf.event.processor.SsfEventProcessor;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.receiver.spi.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.management.SsfReceiverManagementEndpoint;
import org.keycloak.protocol.ssf.receiver.management.SsfReceiverManager;
import org.keycloak.protocol.ssf.receiver.management.SsfReceiverStreamManager;
import org.keycloak.protocol.ssf.receiver.streamclient.DefaultSsfStreamClient;
import org.keycloak.protocol.ssf.receiver.streamclient.SsfStreamClient;
import org.keycloak.protocol.ssf.receiver.transmitterclient.DefaultSsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.transmitterclient.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.DefaultSsfVerificationClient;
import org.keycloak.protocol.ssf.receiver.verification.DefaultSsfStreamSsfStreamVerificationStore;
import org.keycloak.protocol.ssf.receiver.verification.SsfVerificationClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;

public class DefaultSsfProvider implements SsfProvider {

    protected final KeycloakSession session;

    protected SsfSecurityEventTokenParser securityEventTokenParser;

    protected SsfEventProcessor eventProcessor;

    protected SsfEventListener eventListener;

    protected SsfPushDeliveryEndpoint pushDeliveryEndpoint;

    protected SsfReceiverManagementEndpoint ssfReceiverManagementEndpoint;

    protected SsfVerificationClient securityEventsVerifier;

    protected SsfStreamVerificationStore verificationStore;

    protected SsfStreamClient streamClient;

    protected SsfTransmitterClient transmitterClient;

    protected SsfVerificationClient verificationClient;

    protected SsfReceiverManager receiverManager;

    protected SsfReceiverStreamManager receiverStreamManager;

    public DefaultSsfProvider(KeycloakSession session) {
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
                    this,
                    getEventListener(),
                    getVerificationStore()
            );
        }
        return eventProcessor;
    }

    protected SsfPushDeliveryEndpoint getPushEndpoint() {
        if (pushDeliveryEndpoint == null) {
            pushDeliveryEndpoint = new SsfPushDeliveryEndpoint(this);
        }
        return pushDeliveryEndpoint;
    }

    protected SsfReceiverManagementEndpoint getReceiverManagementEndpoint() {
        if (ssfReceiverManagementEndpoint == null) {
            ssfReceiverManagementEndpoint = new SsfReceiverManagementEndpoint(session, getReceiverManager());
        }
        return ssfReceiverManagementEndpoint;
    }

    protected SsfReceiverManager getReceiverManager() {
        if (receiverManager == null) {
            receiverManager = new SsfReceiverManager(session);
        }
        return receiverManager;
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

    protected SsfStreamClient getStreamClient() {
        if (streamClient == null) {
            streamClient = new DefaultSsfStreamClient(session);
        }
        return streamClient;
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

    public SsfEventProcessor eventProcessor() {
        return getSecurityEventProcessor();
    }

    @Override
    public SsfPushDeliveryEndpoint pushEndpoint() {
        return getPushEndpoint();
    }

    @Override
    public SsfReceiverManagementEndpoint receiverManagementEndpoint() {
        return getReceiverManagementEndpoint();
    }

    @Override
    public SsfReceiverStreamManager receiverStreamManager() {
        return getReceiverStreamManager();
    }

    protected SsfReceiverStreamManager getReceiverStreamManager() {
        if (receiverStreamManager == null) {
            receiverStreamManager = new SsfReceiverStreamManager(this);
        }
        return receiverStreamManager;
    }

    @Override
    public SsfStreamClient streamClient() {
        return getStreamClient();
    }

    @Override
    public SsfTransmitterClient transmitterClient() {
        return getTransmitterClient();
    }

    @Override
    public SsfSecurityEventContext createSecurityEventContext(SecurityEventToken securityEventToken, SsfReceiverModel receiverModel) {

        SsfReceiver receiver = receiverManager().loadReceiverFromModel(receiverModel);

        SsfSecurityEventContext context = new SsfSecurityEventContext();
        context.setSecurityEventToken(securityEventToken);
        context.setSession(session);
        context.setReceiver(receiver);

        return context;
    }

    @Override
    public SsfReceiverManager receiverManager() {
        return getReceiverManager();
    }

}
