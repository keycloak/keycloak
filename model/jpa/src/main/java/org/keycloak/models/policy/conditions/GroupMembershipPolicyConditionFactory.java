package org.keycloak.models.policy.conditions;

import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.policy.ResourcePolicyConditionProviderFactory;

public class GroupMembershipPolicyConditionFactory implements ResourcePolicyConditionProviderFactory<GroupMembershipPolicyConditionProvider> {

    public static final String ID = "group-membership-condition";
    public static final String EXPECTED_GROUPS = "groups";

    @Override
    public GroupMembershipPolicyConditionProvider create(KeycloakSession session, Map<String, List<String>> config) {
        return new GroupMembershipPolicyConditionProvider(session, config.get(EXPECTED_GROUPS));
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
