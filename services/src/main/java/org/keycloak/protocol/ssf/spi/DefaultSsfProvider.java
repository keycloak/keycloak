package org.keycloak.protocol.ssf.spi;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.delivery.push.PushEndpoint;
import org.keycloak.protocol.ssf.event.listener.DefaultSsfEventListener;
import org.keycloak.protocol.ssf.event.listener.SsfEventListener;
import org.keycloak.protocol.ssf.event.parser.DefaultSsfEventParser;
import org.keycloak.protocol.ssf.event.parser.SsfEventParser;
import org.keycloak.protocol.ssf.event.processor.DefaultSsfEventProcessor;
import org.keycloak.protocol.ssf.event.processor.SsfEventContext;
import org.keycloak.protocol.ssf.event.processor.SsfEventProcessor;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.management.ReceiverManagementEndpoint;
import org.keycloak.protocol.ssf.receiver.management.ReceiverManager;
import org.keycloak.protocol.ssf.receiver.management.ReceiverStreamManager;
import org.keycloak.protocol.ssf.receiver.streamclient.DefaultSsfStreamClient;
import org.keycloak.protocol.ssf.receiver.streamclient.SsfStreamClient;
import org.keycloak.protocol.ssf.receiver.transmitterclient.DefaultSsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.transmitterclient.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.DefaultSsfVerificationClient;
import org.keycloak.protocol.ssf.receiver.verification.DefaultVerificationStore;
import org.keycloak.protocol.ssf.receiver.verification.SsfVerificationClient;
import org.keycloak.protocol.ssf.receiver.verification.VerificationStore;

public class DefaultSsfProvider implements SsfProvider {

    protected final KeycloakSession session;

    protected SsfEventParser ssfEventParser;

    protected SsfEventProcessor ssfEventProcessor;

    protected SsfEventListener ssfEventListener;

    protected PushEndpoint pushEndpoint;

    protected ReceiverManagementEndpoint receiverManagementEndpoint;

    protected SsfVerificationClient securityEventsVerifier;

    protected VerificationStore verificationStore;

    protected SsfStreamClient streamClient;

    protected SsfTransmitterClient ssfTransmitterClient;

    protected SsfVerificationClient ssfVerificationClient;

    protected ReceiverManager receiverManager;

    protected ReceiverStreamManager receiverStreamManager;

    public DefaultSsfProvider(KeycloakSession session) {
        this.session = session;
    }

    protected SsfEventParser getSsfEventParser() {
        if (ssfEventParser == null) {
            ssfEventParser = new DefaultSsfEventParser(session);
        }
        return ssfEventParser;
    }

    protected SsfEventProcessor getSecurityEventProcessor() {
        if (ssfEventProcessor == null) {
            ssfEventProcessor = new DefaultSsfEventProcessor(
                    this,
                    getSsfEventListener(),
                    getVerificationStore()
            );
        }
        return ssfEventProcessor;
    }

    protected PushEndpoint getPushEndpoint() {
        if (pushEndpoint == null) {
            pushEndpoint = new PushEndpoint(this);
        }
        return pushEndpoint;
    }

    protected ReceiverManagementEndpoint getReceiverManagementEndpoint() {
        if (receiverManagementEndpoint == null) {
            receiverManagementEndpoint = new ReceiverManagementEndpoint(session, getReceiverManager());
        }
        return receiverManagementEndpoint;
    }

    protected ReceiverManager getReceiverManager() {
        if (receiverManager == null) {
            receiverManager = new ReceiverManager(session);
        }
        return receiverManager;
    }

    protected SsfEventListener getSsfEventListener() {
        if (ssfEventListener == null) {
            ssfEventListener = new DefaultSsfEventListener(session);
        }
        return ssfEventListener;
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
        if (ssfTransmitterClient == null) {
            ssfTransmitterClient = new DefaultSsfTransmitterClient(session);
        }
        return ssfTransmitterClient;
    }

    @Override
    public SsfVerificationClient verificationClient() {
        return getVerificationClient();
    }

    protected SsfVerificationClient getVerificationClient() {
        if (ssfVerificationClient == null) {
            ssfVerificationClient = new DefaultSsfVerificationClient(session);
        }
        return ssfVerificationClient;
    }

    @Override
    public SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfEventContext processingContext) {
        var parser = getSsfEventParser();
        return parser.parseSecurityEventToken(encodedSecurityEventToken, processingContext.getReceiver());
    }

    @Override
    public void processSecurityEvents(SsfEventContext securityEventProcessingContext) {
        var processor = getSecurityEventProcessor();
        processor.processSecurityEvents(securityEventProcessingContext);
    }

    @Override
    public VerificationStore verificationStore() {
        return getVerificationStore();
    }

    public VerificationStore getVerificationStore() {
        if (verificationStore == null) {
            verificationStore = new DefaultVerificationStore(session);
        }
        return verificationStore;
    }

    @Override
    public PushEndpoint pushEndpoint() {
        return getPushEndpoint();
    }

    @Override
    public ReceiverManagementEndpoint receiverManagementEndpoint() {
        return getReceiverManagementEndpoint();
    }

    @Override
    public ReceiverStreamManager receiverStreamManager() {
        return getReceiverStreamManager();
    }

    protected ReceiverStreamManager getReceiverStreamManager() {
        if (receiverStreamManager == null) {
            receiverStreamManager = new ReceiverStreamManager(this);
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
    public SsfEventContext createSecurityEventProcessingContext(SecurityEventToken securityEventToken, String receiverAlias) {
        SsfEventContext context = new SsfEventContext();
        context.setSecurityEventToken(securityEventToken);
        context.setSession(session);
        SsfReceiver receiver = getReceiverManager().lookupReceiver(session.getContext(), receiverAlias);
        context.setReceiver(receiver);
        return context;
    }

    @Override
    public ReceiverManager receiverManager() {
        return getReceiverManager();
    }

    public static class Factory implements SsfProviderFactory {

        @Override
        public String getId() {
            return "default";
        }

        @Override
        public SsfProvider create(KeycloakSession keycloakSession) {
            return new DefaultSsfProvider(keycloakSession);
        }

        @Override
        public void init(Config.Scope scope) {
        }

        @Override
        public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

        }

        @Override
        public void close() {

        }
    }
}
