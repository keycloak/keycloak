/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.partialimport;

import java.util.List;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.RoleResource;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class RealmRolesPartialImport extends AbstractPartialImport<RoleRepresentation> {

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
    public boolean exists(RealmModel realm, KeycloakSession session, RoleRepresentation roleRep) {
        for (RoleModel role : realm.getRoles()) {
            if (getName(roleRep).equals(role.getName())) return true;
        }

        return false;
    }

    @Override
    public String existsMessage(RoleRepresentation roleRep) {
        return "Realm role '" + getName(roleRep) + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.REALM_ROLE;
    }

    @Override
    public void overwrite(RealmModel realm, KeycloakSession session, RoleRepresentation roleRep) {
        checkForComposite(roleRep);
        RoleModel role = realm.getRole(getName(roleRep));
        checkForOverwriteComposite(role);
        RoleHelper helper = new RoleHelper(realm);
        helper.updateRole(roleRep, role);
    }

    private void checkForComposite(RoleRepresentation roleRep) {
        if (roleRep.isComposite()) {
            throw new IllegalArgumentException("Composite role '" + getName(roleRep) + "' can not be partially imported");
        }
    }

    private void checkForOverwriteComposite(RoleModel role) {
        if (role.isComposite()) {
            throw new IllegalArgumentException("Composite role '" + role.getName() + "' can not be overwritten.");
        }
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, RoleRepresentation roleRep) {
        checkForComposite(roleRep);
        realm.addRole(getName(roleRep));
        overwrite(realm, session, roleRep);
    }

    public static class RoleHelper extends RoleResource {
        public RoleHelper(RealmModel realm) {
            super(realm);
        }

        @Override
        protected void updateRole(RoleRepresentation rep, RoleModel role) {
            super.updateRole(rep, role);
        }
    }
}
