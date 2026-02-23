package org.keycloak.testsuite.client.policies;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

import org.jboss.logging.Logger;

public class TrackEventsClientPolicyExecutor implements ClientPolicyExecutorProviderFactory, ClientPolicyExecutorProvider<TrackEventsClientPolicyExecutor.Configuration> {
    public static final String PROVIDER_ID = "track-events-client-policy-executor";

    private static final Logger log = Logger.getLogger(TrackEventsClientPolicyExecutor.class);
    private static final TrackEventsClientPolicyExecutor SINGLETON = new TrackEventsClientPolicyExecutor();
    private final List<ClientPolicyEvent> events;

    public TrackEventsClientPolicyExecutor() {
        this.events = new ArrayList<>();
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) {
        log.warnf("Executor with type: %s", context.getEvent());
        events.add(context.getEvent());
    }

    public List<ClientPolicyEvent> getEvents() {
        return events;
    }

    public void clearEventResult() {
        events.clear();
    }

    @Override
    public TrackEventsClientPolicyExecutor create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Test Client Policy executor that checks if only specific policies with certain ClientPolicyEvent types are executed.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
    }
}
