/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.credential;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ReadOnlyException;

import java.util.stream.Stream;

/**
 * Credential store used as a substitute for disabled user storage providers.
 *
 * @author Michal Růžička
 */
public class DisabledCredentialStore implements UserCredentialStore {

    public static final DisabledCredentialStore INSTANCE = new DisabledCredentialStore();

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        throw readOnlyException();
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        throw readOnlyException();
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        throw readOnlyException();
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        return null;
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(RealmModel realm, UserModel user, String type) {
        return Stream.empty();
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        return null;
    }

    @Override
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId) {
        throw readOnlyException();
    }

    @Override
    public void close() {
    }

    private ReadOnlyException readOnlyException() {
        return new ReadOnlyException("user is disabled");
    }

}
