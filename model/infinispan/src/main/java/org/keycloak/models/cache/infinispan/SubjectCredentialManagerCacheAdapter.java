/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.cache.infinispan;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author Alexander Schwartz
 */
public abstract class SubjectCredentialManagerCacheAdapter extends UserCredentialManager {

    public SubjectCredentialManagerCacheAdapter(KeycloakSession session, RealmModel realm, UserModel user) {
        super(session, realm, user);
    }

    public abstract void invalidateCacheForEntity();

    @Override
    public boolean updateCredential(CredentialInput input) {
        invalidateCacheForEntity();
        return super.updateCredential(input);
    }

    @Override
    public void updateStoredCredential(CredentialModel cred) {
        invalidateCacheForEntity();
        super.updateStoredCredential(cred);
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        invalidateCacheForEntity();
        return super.createStoredCredential(cred);
    }

    @Override
    public boolean removeStoredCredentialById(String id) {
        invalidateCacheForEntity();
        return super.removeStoredCredentialById(id);
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        invalidateCacheForEntity();
        return super.moveStoredCredentialTo(id, newPreviousCredentialId);
    }

    @Override
    public void updateCredentialLabel(String credentialId, String userLabel) {
        invalidateCacheForEntity();
        super.updateCredentialLabel(credentialId, userLabel);
    }

    @Override
    public void disableCredentialType(String credentialType) {
        invalidateCacheForEntity();
        super.disableCredentialType(credentialType);
    }

    @Override
    public CredentialModel createCredentialThroughProvider(CredentialModel model) {
        invalidateCacheForEntity();
        return super.createCredentialThroughProvider(model);
    }

}
