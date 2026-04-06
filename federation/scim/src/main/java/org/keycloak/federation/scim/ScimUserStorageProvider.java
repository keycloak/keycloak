/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.federation.scim;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Map;
import java.util.stream.Stream;

/**
 * SCIM 2.0 User Storage Provider implementation.
 * Connects Keycloak to SCIM Service Providers for user/group synchronization.
 *
 * @author alexsedlex (original)
 * Refactored to use UserStorageProvider instead of EventListener
 */
public class ScimUserStorageProvider implements UserStorageProvider,
        UserLookupProvider, UserQueryProvider, UserRegistrationProvider {

    private final KeycloakSession session;
    private final ComponentModel model;
    private final RealmModel realm;
    private final ScimClient scimClient;

    public ScimUserStorageProvider(KeycloakSession session, ComponentModel model, RealmModel realm) {
        this.session = session;
        this.model = model;
        this.realm = realm;
        this.scimClient = new ScimClient(model);
    }

    @Override
    public void close() {
        // Cleanup if needed
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        // Lookup user by ID from SCIM provider
        ScimUser scimUser = scimClient.getUser(id);
        if (scimUser == null) return null;
        return new ScimUserAdapter(session, realm, model, scimUser);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        ScimUser scimUser = scimClient.getUserByUsername(username);
        if (scimUser == null) return null;
        return new ScimUserAdapter(session, realm, model, scimUser);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        ScimUser scimUser = scimClient.getUserByEmail(email);
        if (scimUser == null) return null;
        return new ScimUserAdapter(session, realm, model, scimUser);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return scimClient.getUsersCount();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        return scimClient.searchUsers(search, firstResult, maxResults)
                .stream()
                .map(u -> new ScimUserAdapter(session, realm, model, u));
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue, Integer firstResult, Integer maxResults) {
        // Search by specific attribute
        return scimClient.searchUsersByAttribute(attrName, attrValue, firstResult, maxResults)
                .stream()
                .map(u -> new ScimUserAdapter(session, realm, model, u));
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        ScimUser newUser = new ScimUser();
        newUser.setUserName(username);
        ScimUser created = scimClient.createUser(newUser);
        return new ScimUserAdapter(session, realm, model, created);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String scimId = user.getFirstAttribute("scimId");
        if (scimId != null) {
            return scimClient.deleteUser(scimId);
        }
        return false;
    }

    public void syncUserToScim(UserModel localUser) {
        // Sync local user changes to SCIM provider
        ScimUser scimUser = toScimUser(localUser);
        String scimId = localUser.getFirstAttribute("scimId");
        if (scimId != null) {
            scimClient.updateUser(scimId, scimUser);
        } else {
            ScimUser created = scimClient.createUser(scimUser);
            localUser.setSingleAttribute("scimId", created.getId());
        }
    }

    private ScimUser toScimUser(UserModel user) {
        ScimUser scimUser = new ScimUser();
        scimUser.setUserName(user.getUsername());
        scimUser.setEmail(user.getEmail());
        scimUser.setGivenName(user.getFirstName());
        scimUser.setFamilyName(user.getLastName());
        scimUser.setActive(user.isEnabled());
        return scimUser;
    }
}
