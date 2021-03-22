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

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Provides one user where everything is stored in user federated storage
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PassThroughFederatedUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater
{

    public static final Set<String> CREDENTIAL_TYPES = Collections.singleton(PasswordCredentialModel.TYPE);
    public static final String PASSTHROUGH_USERNAME = "passthrough";
    public static final String INITIAL_PASSWORD = "secret";
    private KeycloakSession session;
    private ComponentModel component;

    public PassThroughFederatedUserStorageProvider(KeycloakSession session, ComponentModel component) {
        this.session = session;
        this.component = component;
    }

    public Set<String> getSupportedCredentialTypes() {
        return CREDENTIAL_TYPES;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getSupportedCredentialTypes().contains(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!PasswordCredentialModel.TYPE.equals(credentialType)) return false;
        return true;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (input.getType().equals(PasswordCredentialModel.TYPE)) {
             if (INITIAL_PASSWORD.equals(input.getChallengeResponse())) {
                 return true;
             }
            List<CredentialModel> existing = session.userFederatedStorage().getStoredCredentialsByType(realm, user.getId(), "CLEAR_TEXT_PASSWORD");
            if (existing.isEmpty()) return false;
            return existing.get(0).getSecretData().equals("{\"value\":\"" + input.getChallengeResponse() + "\"}");
        }
        return false;
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        // testing federated credential attributes
        if (input.getType().equals(PasswordCredentialModel.TYPE)) {
            List<CredentialModel> existing = session.userFederatedStorage().getStoredCredentialsByType(realm, user.getId(), "CLEAR_TEXT_PASSWORD");
            if (existing.isEmpty()) {
                CredentialModel model = new CredentialModel();
                model.setType("CLEAR_TEXT_PASSWORD");
                model.setSecretData("{\"value\":\"" + input.getChallengeResponse() + "\"}");
                session.userFederatedStorage().createCredential(realm, user.getId(), model);
            } else {
                CredentialModel model = existing.get(0);
                model.setType("CLEAR_TEXT_PASSWORD");
                model.setSecretData("{\"value\":\"" + input.getChallengeResponse() + "\"}");
                session.userFederatedStorage().updateCredential(realm, user.getId(), model);

            }
            return true;
        }
        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        List<CredentialModel> existing = session.userFederatedStorage().getStoredCredentialsByType(realm, user.getId(), "CLEAR_TEXT_PASSWORD");
        for (CredentialModel model : existing) {
            session.userFederatedStorage().removeStoredCredential(realm, user.getId(), model.getId());
        }
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return CREDENTIAL_TYPES;
    }

    @Override
    public void close() {

    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        if (!StorageId.externalId(id).equals(PASSTHROUGH_USERNAME)) return null;
        return getUserModel(realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        if  (!PASSTHROUGH_USERNAME.equals(username)) return null;

        return getUserModel(realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        List<String> list = session.userFederatedStorage().getUsersByUserAttribute(realm, AbstractUserAdapterFederatedStorage.EMAIL_ATTRIBUTE, email);
        for (String user : list) {
            StorageId storageId = new StorageId(user);
            if (!storageId.getExternalId().equals(PASSTHROUGH_USERNAME)) continue;
            if (!storageId.getProviderId().equals(component.getId())) continue;
            return getUserModel(realm);

        }
        return null;
    }

    private UserModel getUserModel(final RealmModel realm) {
        return new AbstractUserAdapterFederatedStorage(session, realm, component) {
            @Override
            public String getUsername() {
                return PASSTHROUGH_USERNAME;
            }

            @Override
            public void setUsername(String username) {

            }
        };
    }
}
