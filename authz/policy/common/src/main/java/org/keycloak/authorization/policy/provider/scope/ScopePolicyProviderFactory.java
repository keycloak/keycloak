package org.keycloak.authorization.policy.provider.scope;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopePolicyProviderFactory implements PolicyProviderFactory<ScopePermissionRepresentation> {

    private ScopePolicyProvider provider = new ScopePolicyProvider();

    @Override
    public String getName() {
        return "Scope-Based";
    }

    @Override
    public String getGroup() {
        return "Permission";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public Class<ScopePermissionRepresentation> getRepresentationType() {
        return ScopePermissionRepresentation.class;
    }

    @Override
    public ScopePermissionRepresentation toRepresentation(Policy policy, ScopePermissionRepresentation representation) {
        return representation;
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
        return "scope";
    }
}
