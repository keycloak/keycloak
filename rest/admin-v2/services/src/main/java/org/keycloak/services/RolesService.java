package org.keycloak.services;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.RoleContainerResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public class RolesService {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator permissions;
    private final AdminEventBuilder adminEventBuilder;

    public RolesService(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator permissions, AdminEventBuilder adminEventBuilder) {
        this.session = session;
        this.realm = realm;
        this.permissions = permissions;
        this.adminEventBuilder = adminEventBuilder;
    }

    public RoleContainerResource resource(RoleContainerModel roleContainer) {
        var resource = new RoleContainerResource(session, session.getContext().getUri(), realm, permissions, adminEventBuilder);
        resource.setRoleContainer(roleContainer);
        return resource;
    }
}
