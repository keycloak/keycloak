package org.keycloak.authorization.policy.provider.resource;

import java.util.Map;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcePolicyProviderFactory implements PolicyProviderFactory {

    private ResourcePolicyProvider provider = new ResourcePolicyProvider();

    @Override
    public String getName() {
        return "Resource-Based";
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
        return new PolicyProviderAdminService<ResourcePermissionRepresentation>() {
            @Override
            public void onCreate(Policy policy, ResourcePermissionRepresentation representation) {
                updateResourceType(policy, representation);
            }

            @Override
            public void onUpdate(Policy policy, ResourcePermissionRepresentation representation) {
                updateResourceType(policy, representation);
            }

            private void updateResourceType(Policy policy, ResourcePermissionRepresentation representation) {
                //TODO: remove this check once we migrate to new API
                if (representation != null) {
                    Map<String, String> config = policy.getConfig();

                    config.compute("defaultResourceType", (key, value) -> {
                        String resourceType = representation.getResourceType();
                        return resourceType != null ? representation.getResourceType() : null;
                    });

                    policy.setConfig(config);
                }
            }

            @Override
            public void onRemove(Policy policy) {

            }

            @Override
            public Class<ResourcePermissionRepresentation> getRepresentationType() {
                return ResourcePermissionRepresentation.class;
            }

            @Override
            public ResourcePermissionRepresentation toRepresentation(Policy policy) {
                ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

                representation.setResourceType(policy.getConfig().get("defaultResourceType"));

                return representation;
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
        return "resource";
    }
}
