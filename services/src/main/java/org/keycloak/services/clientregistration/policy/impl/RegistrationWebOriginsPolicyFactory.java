package org.keycloak.services.clientregistration.policy.impl;

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.policy.AbstractClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;

public class RegistrationWebOriginsPolicyFactory extends AbstractClientRegistrationPolicyFactory {

    public static final String PROVIDER_ID = "registration-web-origins";

    public static final String WEB_ORIGINS = "web-origins";

    private static final ProviderConfigProperty WEB_ORIGINS_PROPERTY = new ProviderConfigProperty(WEB_ORIGINS, "registration-web-origins.label", "registration-web-origins.tooltip", ProviderConfigProperty.MULTIVALUED_STRING_TYPE, null);

    @Override
    public ClientRegistrationPolicy create(KeycloakSession session, ComponentModel model) {
        return new RegistrationWebOriginsPolicy(session, model);
    }

    @Override
    public String getHelpText() {
        return "Allowed web origins for client registration requests";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(WEB_ORIGINS_PROPERTY);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
