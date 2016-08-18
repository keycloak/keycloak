package org.keycloak.authorization.policy.provider.time;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyProviderFactory implements PolicyProviderFactory {

    @Override
    public String getName() {
        return "Time";
    }

    @Override
    public String getGroup() {
        return "Time Based";
    }

    @Override
    public PolicyProvider create(Policy policy, AuthorizationProvider authorization) {
        return new TimePolicyProvider(policy);
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer) {
        return new TimePolicyAdminResource();
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
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

    @Override
    public String getId() {
        return "time";
    }
}
