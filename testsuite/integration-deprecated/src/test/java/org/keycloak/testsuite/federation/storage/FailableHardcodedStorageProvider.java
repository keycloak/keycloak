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
package org.keycloak.testsuite.federation.storage;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FailableHardcodedStorageProvider implements UserStorageProvider, UserLookupProvider, ImportedUserValidation, CredentialInputUpdater, CredentialInputValidator {

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
        componentFail = model.getConfig().getFirst("fail") != null && model.getConfig().getFirst("fail").equalsIgnoreCase("true");
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");
        if (!(input instanceof UserCredentialModel)) return false;
        if (!user.getUsername().equals(username)) throw new RuntimeException("UNKNOWN USER!");

        if (input.getType().equals(UserCredentialModel.PASSWORD)) {
            password = ((UserCredentialModel)input).getValue();
            return true;

        } else {
            return false;
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");
        if (!(input instanceof UserCredentialModel)) return false;
        if (!user.getUsername().equals("billb")) throw new RuntimeException("UNKNOWN USER!");
        if (input.getType().equals(UserCredentialModel.PASSWORD)) {
            return password != null && password.equals( ((UserCredentialModel)input).getValue());
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
            name = name;
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
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");
        return new Delegate(user);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");
        throw new RuntimeException("THIS IMPORTS  SHOULD NEVER BE CALLED");
    }

    @Override
    public UserModel getUserByUsername(String uname, RealmModel realm) {
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");
        if (!username.equals(uname)) return null;
        UserModel local = session.userLocalStorage().getUserByUsername(uname, realm);
        if (local != null && !model.getId().equals(local.getFederationLink())) {
            throw new RuntimeException("local storage has wrong federation link");
        }
        if (local != null) return new Delegate(local);
        local = session.userLocalStorage().addUser(realm, uname);
        local.setEnabled(true);
        local.setFirstName(first);
        local.setLastName(last);
        local.setEmail(email);
        local.setFederationLink(model.getId());
        for (String key : attributes.keySet()) {
            List<String> values = attributes.get(key);
            if (values == null) continue;
            local.setAttribute(key, values);
        }
        return new Delegate(local);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        if (fail || componentFail) throw new RuntimeException("FORCED FAILURE");
        return null;
    }

    @Override
    public void close() {

    }
}
