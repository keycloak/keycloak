package org.keycloak.authorization.policy.provider.scope;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopePolicyProviderFactory implements PolicyProviderFactory {

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
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer, AuthorizationProvider authorization) {
        return new PolicyProviderAdminService<ScopePermissionRepresentation>() {
            @Override
            public void onCreate(Policy policy, ScopePermissionRepresentation representation) {

            }

            @Override
            public void onUpdate(Policy policy, ScopePermissionRepresentation representation) {

            }

            @Override
            public void onRemove(Policy policy) {

            }

            @Override
            public Class<ScopePermissionRepresentation> getRepresentationType() {
                return ScopePermissionRepresentation.class;
            }

            @Override
            public ScopePermissionRepresentation toRepresentation(Policy policy) {
                return new ScopePermissionRepresentation();
            }
        };
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
        return "scope";
    }
}
