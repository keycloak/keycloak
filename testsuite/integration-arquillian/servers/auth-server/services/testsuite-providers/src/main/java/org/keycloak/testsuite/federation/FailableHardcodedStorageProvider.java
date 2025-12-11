/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FailableHardcodedStorageProvider implements UserStorageProvider, UserLookupProvider, UserQueryProvider,
        ImportedUserValidation, CredentialInputUpdater, CredentialInputValidator {

    public static String username = "billb";
    public static String password = "password";
    public static String email = "billb@nowhere.com";
    public static String first = "Bill";
    public static String last = "Burke";
    public static MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();

    public static boolean fail;

    protected ComponentModel model;
    protected KeycloakSession session;
    protected boolean componentFail;

    public FailableHardcodedStorageProvider(ComponentModel model, KeycloakSession session) {
        this.model = model;
        this.session = session;
        componentFail = isInFailMode(model);
    }

    public static boolean isInFailMode(ComponentModel model) {
        return model.getConfig().getFirst("fail") != null && model.getConfig().getFirst("fail").equalsIgnoreCase("true");
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        checkForceFail();
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        checkForceFail();
        if (!(input instanceof UserCredentialModel)) return false;
        if (!user.getUsername().equals(username)) throw new RuntimeException("UNKNOWN USER!");

        if (input.getType().equals(PasswordCredentialModel.TYPE)) {
            password = input.getChallengeResponse();
            return true;

        } else {
            return false;
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        checkForceFail();

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        checkForceFail();
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        checkForceFail();
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        checkForceFail();
        if (!user.getUsername().equals("billb")) throw new RuntimeException("UNKNOWN USER!");
        if (credentialInput.getType().equals(PasswordCredentialModel.TYPE)) {
            return password != null && password.equals(credentialInput.getChallengeResponse());
        } else {
            return false;
        }
    }

    private static class Delegate extends UserModelDelegate {
        public Delegate(UserModel delegate) {
            super(delegate);
        }

        @Override
        public void setUsername(String name) {
            super.setUsername(name);
            username = name;
        }

        @Override
        public void setSingleAttribute(String name, String value) {
            super.setSingleAttribute(name, value);
            attributes.putSingle(name, value);
        }

        @Override
        public void setAttribute(String name, List<String> values) {
            super.setAttribute(name, values);
            attributes.put(name, values);
        }

        @Override
        public void removeAttribute(String name) {
            super.removeAttribute(name);
            attributes.remove(name);
        }

        @Override
        public void setFirstName(String firstName) {
            super.setFirstName(firstName);
            first = firstName;
        }

        @Override
        public void setLastName(String lastName) {
            super.setLastName(lastName);
            last = lastName;
        }

        @Override
        public void setEmail(String em) {
            super.setEmail(em);
            email = em;
        }
    }

    @Override
    public UserModel validate(RealmModel realm, UserModel user) {
        checkForceFail();
        return new Delegate(user);
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        checkForceFail();
        throw new RuntimeException("THIS IMPORTS  SHOULD NEVER BE CALLED");
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String uname) {
        checkForceFail();
        if (!username.equals(uname)) return null;
        UserModel local = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, uname);
        if (local != null && !model.getId().equals(local.getFederationLink())) {
            throw new RuntimeException("local storage has wrong federation link");
        }
        if (local != null) return new Delegate(local);
        local = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, uname);
        local.setEnabled(true);
        local.setFirstName(first);
        local.setLastName(last);
        local.setEmail(email);
        local.setFederationLink(model.getId());
        for (var entry : attributes.entrySet()) {
            List<String> values = entry.getValue();
            if (values == null) continue;
            local.setAttribute(entry.getKey(), values);
        }
        return new Delegate(local);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        checkForceFail();
        return null;
    }

    protected void checkForceFail() {
        if (fail || componentFail) throwFailure();
    }

    public static  void throwFailure() {
        throw new RuntimeException("FORCED FAILURE");
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        checkForceFail();
        return 1;
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search) {
        checkForceFail();
        if (!search.equals(username)) return Stream.empty();
        UserModel model = getUserByUsername(realm, username);
        return model != null ? Stream.of(model) : Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        checkForceFail();
        if (!search.equals(username)) return Stream.empty();
        UserModel model = getUserByUsername(realm, username);
        return model != null ? Stream.of(model) : Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params) {
        checkForceFail();
        if (!username.equals(params.get("username")))return Stream.empty();
        UserModel model = getUserByUsername(realm, username);
        return model != null ? Stream.of(model) : Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        checkForceFail();
        if (!username.equals(params.get("username")))return Stream.empty();
        UserModel model = getUserByUsername(realm, username);
        return model != null ? Stream.of(model) : Stream.empty();
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        checkForceFail();
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
        checkForceFail();
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        checkForceFail();
        return Stream.empty();
    }

    @Override
    public void close() {

    }
}
