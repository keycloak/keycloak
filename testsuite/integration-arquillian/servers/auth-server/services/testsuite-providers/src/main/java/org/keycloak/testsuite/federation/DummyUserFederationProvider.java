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

package org.keycloak.testsuite.federation;

import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DummyUserFederationProvider implements UserFederationProvider {

    private final Map<String, UserModel> users;

    public DummyUserFederationProvider(Map<String, UserModel> users) {
        this.users = users;
    }

    @Override
    public UserModel validateAndProxy(RealmModel realm, UserModel local) {
        return local;
    }

    @Override
    public boolean synchronizeRegistrations() {
        return true;
    }

    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        users.put(user.getUsername(), user);
        return user;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return users.remove(user.getUsername()) != null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return users.get(username);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public boolean isValid(RealmModel realm, UserModel local) {
        String username = local.getUsername();
        return users.containsKey(username);
    }

    @Override
    public Set<String> getSupportedCredentialTypes(UserModel user) {
        // Just user "test-user" is able to validate password with this federationProvider
        if (user.getUsername().equals("test-user")) {
            return Collections.singleton(UserCredentialModel.PASSWORD);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> getSupportedCredentialTypes() {
        return Collections.singleton(UserCredentialModel.PASSWORD);
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        if (user.getUsername().equals("test-user") && input.size() == 1) {
            UserCredentialModel password = input.get(0);
            if (password.getType().equals(UserCredentialModel.PASSWORD)) {
                return "secret".equals(password.getValue());
            }
        }
        return false;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        return validCredentials(realm, user, Arrays.asList(input));
    }

    @Override
    public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel credential) {
        return CredentialValidationOutput.failed();
    }

    @Override
    public void close() {

    }
}

