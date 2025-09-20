package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class EventBasedWorkflowProviderFactory implements WorkflowProviderFactory<EventBasedWorkflowProvider> {

    public static final String ID = "event-based-workflow";

    @Override
    public EventBasedWorkflowProvider create(KeycloakSession session, ComponentModel model) {
        return new EventBasedWorkflowProvider(session, model);
    }

    @Override
    public void init(Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getHelpText() {
        return "";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void close() {

    }

}
