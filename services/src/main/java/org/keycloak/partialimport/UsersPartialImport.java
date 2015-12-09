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
import java.util.Map;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.UsersResource;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class UsersPartialImport extends AbstractPartialImport<UserRepresentation> {

    @Override
    public List<UserRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getUsers();
    }

    @Override
    public String getName(UserRepresentation user) {
        return user.getUsername();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        return userNameExists(realm, session, user) || userEmailExists(realm, session, user);
    }

    private boolean userNameExists(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        return session.users().getUserByUsername(user.getUsername(), realm) != null;
    }

    private boolean userEmailExists(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        return (user.getEmail() != null) &&
               (session.users().getUserByEmail(user.getEmail(), realm) != null);
    }

    @Override
    public String existsMessage(UserRepresentation user) {
        if (user.getEmail() == null) {
            return "User with user name " + getName(user) + " already exists.";
        }

        return "User with user name " + getName(user) + " or with email " + user.getEmail() + " already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.USER;
    }

    @Override
    public void overwrite(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        UserModel userModel = session.users().getUserByUsername(user.getUsername(), realm);
        UsersResource.updateUserFromRep(userModel, user, null, realm, session);
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        Map<String, ClientModel> apps = realm.getClientNameMap();
        user.setId(null);
        RepresentationToModel.createUser(session, realm, user, apps);
    }

}
