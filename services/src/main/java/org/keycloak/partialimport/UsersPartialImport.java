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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * PartialImport handler for users.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class UsersPartialImport extends AbstractPartialImport<UserRepresentation> {

    // Sometimes session.users().getUserByUsername() doesn't work right after create,
    // so we cache the created id here.
    private final Map<String, String> createdIds = new HashMap<>();

    @Override
    public List<UserRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getUsers();
    }

    @Override
    public String getName(UserRepresentation user) {
        if (user.getUsername() != null) return user.getUsername();

        return user.getEmail();
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        if (createdIds.containsKey(getName(user))) return createdIds.get(getName(user));

        String userName = user.getUsername();
        if (userName != null) {
            return session.users().getUserByUsername(realm, userName).getId();
        } else if (!realm.isDuplicateEmailsAllowed()) {
            String email = user.getEmail();
            return session.users().getUserByEmail(realm, email).getId();
        }
        
        return null;
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        return userNameExists(realm, session, user) || userEmailExists(realm, session, user);
    }

    private boolean userNameExists(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        return session.users().getUserByUsername(realm, user.getUsername()) != null;
    }

    private boolean userEmailExists(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        return (user.getEmail() != null) && !realm.isDuplicateEmailsAllowed() &&
               (session.users().getUserByEmail(realm, user.getEmail()) != null);
    }

    @Override
    public String existsMessage(RealmModel realm, UserRepresentation user) {
        if (user.getEmail() == null || !realm.isDuplicateEmailsAllowed()) {
            return "User with user name " + getName(user) + " already exists.";
        }

        return "User with user name " + getName(user) + " or with email " + user.getEmail() + " already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.USER;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        UserModel userModel = session.users().getUserByUsername(realm, user.getUsername());
        if (userModel == null && !realm.isDuplicateEmailsAllowed()) {
            userModel = session.users().getUserByEmail(realm, user.getEmail());
        }
        if (userModel != null) {
            boolean success = new UserManager(session).removeUser(realm, userModel);
            if (!success) throw new RuntimeException("Unable to overwrite user " + getName(user));
        }
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, UserRepresentation user) {
        if (user.getId() == null) {
            user.setId(KeycloakModelUtils.generateId());
        }
        UserModel userModel = RepresentationToModel.createUser(session, realm, user);
        if (userModel == null) throw new RuntimeException("Unable to create user " + getName(user));
        createdIds.put(getName(user), userModel.getId());
    }

}
