/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.light;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.SubjectCredentialManager;

/**
 *
 * @author hmlnarik
 */
class EmptyCredentialManager implements SubjectCredentialManager {

    public static final EmptyCredentialManager INSTANCE = new EmptyCredentialManager();

    @Override
    public boolean isValid(List<CredentialInput> inputs) {
        return false;
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        // no-op
        return false;
    }

    @Override
    public void updateStoredCredential(CredentialModel cred) {
        // no-op
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        // no-op
        return null;
    }

    @Override
    public boolean removeStoredCredentialById(String id) {
        // no-op
        return false;
    }

    @Override
    public CredentialModel getStoredCredentialById(String id) {
        return null;
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream() {
        return Stream.empty();
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
        return Stream.empty();
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
        return null;
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        return false;
    }

    @Override
    public void updateCredentialLabel(String credentialId, String credentialLabel) {
        // no-op
    }

    @Override
    public void disableCredentialType(String credentialType) {
        // no-op
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream() {
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(String type) {
        return false;
    }

    @Override
    public boolean isConfiguredLocally(String type) {
        return false;
    }

    @Override
    public Stream<String> getConfiguredUserStorageCredentialTypesStream() {
        return Stream.empty();
    }

    @Override
    public CredentialModel createCredentialThroughProvider(CredentialModel model) {
        return null;
    }

}
