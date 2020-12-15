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
package org.keycloak.storage.federated;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserFederatedUserCredentialStore extends Provider {
    void updateCredential(RealmModel realm, String userId, CredentialModel cred);
    CredentialModel createCredential(RealmModel realm, String userId, CredentialModel cred);
    boolean removeStoredCredential(RealmModel realm, String userId, String id);
    CredentialModel getStoredCredentialById(RealmModel realm, String userId, String id);

    /**
     * @deprecated Use {@link #getStoredCredentialsStream(RealmModel, String) getStoredCredentialsStream} instead.
     */
    @Deprecated
    List<CredentialModel> getStoredCredentials(RealmModel realm, String userId);

    /**
     * Obtains the credentials associated with the federated user identified by {@code userId}.
     *
     * @param realm a reference to the realm.
     * @param userId the user identifier.
     * @return a non-null {@link Stream} of credentials.
     */
    default Stream<CredentialModel> getStoredCredentialsStream(RealmModel realm, String userId) {
        List<CredentialModel> value = this.getStoredCredentials(realm, userId);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * @deprecated Use {@link #getStoredCredentialsByTypeStream(RealmModel, String, String) getStoredCredentialsByTypeStream} instead.
     */
    @Deprecated
    List<CredentialModel> getStoredCredentialsByType(RealmModel realm, String userId, String type);

    /**
     * Obtains the credentials of type {@code type} that are associated with the federated user identified by {@code userId}.
     *
     * @param realm a reference to the realm.
     * @param userId the user identifier.
     * @param type the credential type.
     * @return a non-null {@link Stream} of credentials.
     */
    default Stream<CredentialModel> getStoredCredentialsByTypeStream(RealmModel realm, String userId, String type) {
        List<CredentialModel> value = this.getStoredCredentialsByType(realm, userId, type);
        return value != null ? value.stream() : Stream.empty();
    }

    CredentialModel getStoredCredentialByNameAndType(RealmModel realm, String userId, String name, String type);

    /**
     * The {@link Streams} interface makes all collection-based methods in {@link UserFederatedUserCredentialStore}
     * default by providing implementations that delegate to the {@link Stream}-based variants instead of the other way
     * around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends UserFederatedUserCredentialStore {
        @Override
        default List<CredentialModel> getStoredCredentials(RealmModel realm, String userId) {
            return this.getStoredCredentialsStream(realm, userId).collect(Collectors.toList());
        }

        @Override
        Stream<CredentialModel> getStoredCredentialsStream(RealmModel realm, String userId);

        @Override
        default List<CredentialModel> getStoredCredentialsByType(RealmModel realm, String userId, String type) {
            return this.getStoredCredentialsByTypeStream(realm, userId, type).collect(Collectors.toList());
        }

        @Override
        Stream<CredentialModel> getStoredCredentialsByTypeStream(RealmModel realm, String userId, String type);
    }
}
