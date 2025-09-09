package org.keycloak.models.policy.conditions;

import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.policy.ResourcePolicyConditionProviderFactory;

public class UserAttributePolicyConditionFactory implements ResourcePolicyConditionProviderFactory<UserAttributePolicyConditionProvider> {

    public static final String ID = "user-attribute-condition";

    @Override
    public UserAttributePolicyConditionProvider create(KeycloakSession session, Map<String, List<String>> config) {
        return new UserAttributePolicyConditionProvider(session, config);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}
