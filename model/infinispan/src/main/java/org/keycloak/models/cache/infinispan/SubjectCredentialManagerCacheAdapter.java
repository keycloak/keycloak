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
import org.keycloak.models.SubjectCredentialManager;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Alexander Schwartz
 */
public abstract class SubjectCredentialManagerCacheAdapter implements SubjectCredentialManager {

    private final SubjectCredentialManager subjectCredentialManager;

    protected SubjectCredentialManagerCacheAdapter(SubjectCredentialManager subjectCredentialManager) {
        this.subjectCredentialManager = subjectCredentialManager;
    }

    public abstract void invalidateCacheForEntity();

    @Override
    public boolean isValid(List<CredentialInput> inputs) {
        // validating a password might still update its hashes, similar logic might apply to OTP logic
        // instead of having each
        invalidateCacheForEntity();
        return subjectCredentialManager.isValid(inputs);
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        invalidateCacheForEntity();
        return subjectCredentialManager.updateCredential(input);
    }

    @Override
    public void updateStoredCredential(CredentialModel cred) {
        invalidateCacheForEntity();
        subjectCredentialManager.updateStoredCredential(cred);
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        invalidateCacheForEntity();
        return subjectCredentialManager.createStoredCredential(cred);
    }

    @Override
    public boolean removeStoredCredentialById(String id) {
        invalidateCacheForEntity();
        return subjectCredentialManager.removeStoredCredentialById(id);
    }

    @Override
    public CredentialModel getStoredCredentialById(String id) {
        return subjectCredentialManager.getStoredCredentialById(id);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream() {
        return subjectCredentialManager.getStoredCredentialsStream();
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
        return subjectCredentialManager.getStoredCredentialsByTypeStream(type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
        return subjectCredentialManager.getStoredCredentialByNameAndType(name, type);
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        invalidateCacheForEntity();
        return subjectCredentialManager.moveStoredCredentialTo(id, newPreviousCredentialId);
    }

    @Override
    public void updateCredentialLabel(String credentialId, String userLabel) {
        invalidateCacheForEntity();
        subjectCredentialManager.updateCredentialLabel(credentialId, userLabel);
    }

    @Override
    public void disableCredentialType(String credentialType) {
        invalidateCacheForEntity();
        subjectCredentialManager.disableCredentialType(credentialType);
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream() {
        return subjectCredentialManager.getDisableableCredentialTypesStream();
    }

    @Override
    public boolean isConfiguredFor(String type) {
        return subjectCredentialManager.isConfiguredFor(type);
    }

    @Override
    public boolean isConfiguredLocally(String type) {
        return subjectCredentialManager.isConfiguredLocally(type);
    }

    @Override
    public Stream<String> getConfiguredUserStorageCredentialTypesStream() {
        return subjectCredentialManager.getConfiguredUserStorageCredentialTypesStream();
    }

    @Override
    public CredentialModel createCredentialThroughProvider(CredentialModel model) {
        invalidateCacheForEntity();
        return subjectCredentialManager.createCredentialThroughProvider(model);
    }

}
