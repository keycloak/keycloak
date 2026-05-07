package org.keycloak.services.clientregistration.policy.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

public class RegistrationWebOriginsPolicy implements ClientRegistrationPolicy {

    private final List<String> allowedWebOrigins;

    public RegistrationWebOriginsPolicy(KeycloakSession session, ComponentModel model) {
        allowedWebOrigins = model.getConfig().getList(RegistrationWebOriginsPolicyFactory.WEB_ORIGINS);
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel) {
    }

    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
    }

    @Override
    public void afterUpdate(ClientRegistrationContext context, ClientModel clientModel) {
    }

    @Override
    public void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
    }

    @Override
    public void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
    }

    @Override
    public Collection<String> getAllowedOrigins() {
        return allowedWebOrigins != null ? allowedWebOrigins : Collections.emptyList();
    }

}
