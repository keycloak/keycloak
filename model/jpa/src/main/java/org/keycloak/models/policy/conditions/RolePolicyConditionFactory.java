package org.keycloak.models.policy.conditions;

import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.policy.ResourcePolicyConditionProviderFactory;

public class RolePolicyConditionFactory implements ResourcePolicyConditionProviderFactory<RolePolicyConditionProvider> {

    public static final String ID = "role-condition";
    public static final String EXPECTED_ROLES = "roles";

    @Override
    public RolePolicyConditionProvider create(KeycloakSession session, Map<String, List<String>> config) {
        return new RolePolicyConditionProvider(session, config.get(EXPECTED_ROLES));
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
