/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.partialimport;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.RoleResource;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * PartialImport handler for Realm Roles.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class RealmRolesPartialImport extends AbstractPartialImport<RoleRepresentation> {

    public Set<RoleRepresentation> getToOverwrite() {
        return this.toOverwrite;
    }

    public Set<RoleRepresentation> getToSkip() {
        return this.toSkip;
    }

    @Override
    public List<RoleRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        if (partialImportRep.getRoles() == null) return null;
        return partialImportRep.getRoles().getRealm();
    }

    @Override
    public String getName(RoleRepresentation roleRep) {
        if (roleRep.getName() == null)
            throw new IllegalStateException("Realm role to import does not have a name");
        return roleRep.getName();
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, RoleRepresentation roleRep) {
        return realm.getRolesStream()
                .filter(r -> Objects.equals(getName(roleRep), r.getName()))
                .map(RoleModel::getId)
                .findFirst().orElse(null);
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, RoleRepresentation roleRep) {
        return realm.getRolesStream().anyMatch(role -> Objects.equals(getName(roleRep), role.getName()));
    }

    @Override
    public String existsMessage(RealmModel realm, RoleRepresentation roleRep) {
        return "Realm role '" + getName(roleRep) + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.REALM_ROLE;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, RoleRepresentation roleRep) {
        RoleModel role = realm.getRole(getName(roleRep));
        RoleHelper helper = new RoleHelper(realm);
        helper.deleteRole(role);
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, RoleRepresentation roleRep) {
        realm.addRole(getName(roleRep));
    }

    public static class RoleHelper extends RoleResource {
        public RoleHelper(RealmModel realm) {
            super(realm);
        }

        @Override
        protected void deleteRole(RoleModel role) {
            super.deleteRole(role);
        }
    }
}
