package org.keycloak.tests.authz.services;

import java.util.Set;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testframework.realm.ManagedClient;

public final class Permission {

    public static Builder create(ManagedClient resourceServer) {
        return new Builder(resourceServer);
    }

    private final ManagedClient resourceServer;
    private Set<String> resources;

    private Permission(ManagedClient resourceServer) {
        this.resourceServer = resourceServer;
    }

    public static final class Builder {

        private final ManagedClient resourceServer;
        private Permission permission;

        public Builder(ManagedClient resourceServer) {
            this.resourceServer = resourceServer;
            permission = new Permission(resourceServer);
        }

        public Builder resource(String resource) {
            permission.resources = Set.of(resource);
            return this;
        }

        public Permission grant() {
            if (permission.resources != null) {
                PolicyRepresentation alwaysGrant = Authz.createAlwaysGrantPolicy(resourceServer);

                for (String resource : permission.resources) {
                    Authz.create(resourceServer, ResourceRepresentation.create()
                            .name(resource)
                            .build());
                }

                Authz.create(resourceServer, ResourcePermissionRepresentation.create()
                        .name(KeycloakModelUtils.generateId())
                        .resources(permission.resources)
                        .policies(Set.of(alwaysGrant.getId()))
                        .build());
            } else {
                throw new IllegalStateException("You must specify a resources or scopes to grant permission");
            }
            return permission;
        }
    }
}
