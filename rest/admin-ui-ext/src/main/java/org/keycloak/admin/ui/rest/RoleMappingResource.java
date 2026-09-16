package org.keycloak.admin.ui.rest;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public abstract class RoleMappingResource {
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AdminPermissionEvaluator auth;

    public RoleMappingResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }

    protected void requireView(GroupModel group) {
        if (!GroupModel.Type.ORGANIZATION.equals(group.getType())) {
            auth.groups().requireView(group);
            return;
        }

        OrganizationModel organization = group.getOrganization();
        if (organization == null || organization.getRealm() == null
                || !Objects.equals(realm.getId(), organization.getRealm().getId())) {
            throw new NotFoundException("Could not find group");
        }
        GroupModel root = session.getProvider(OrganizationProvider.class).getOrganizationGroup(organization);
        if (root == null || Objects.equals(root.getId(), group.getId())) {
            throw new NotFoundException("Could not find group");
        }
        Set<String> visited = new HashSet<>();
        GroupModel current = group;
        while (current != null && visited.add(current.getId())) {
            OrganizationModel owner = current.getOrganization();
            if (!GroupModel.Type.ORGANIZATION.equals(current.getType()) || owner == null || owner.getRealm() == null
                    || !Objects.equals(organization.getId(), owner.getId())
                    || !Objects.equals(realm.getId(), owner.getRealm().getId())) {
                throw new NotFoundException("Could not find group");
            }
            if (Objects.equals(root.getId(), current.getId())) {
                auth.orgs().requireView(organization);
                return;
            }
            current = current.getParent();
        }
        throw new NotFoundException("Could not find group");
    }
}
