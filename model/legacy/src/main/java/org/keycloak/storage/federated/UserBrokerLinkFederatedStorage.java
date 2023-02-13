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

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserBrokerLinkFederatedStorage {
    String getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm);
    void addFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel socialLink);
    boolean removeFederatedIdentity(RealmModel realm, String userId, String socialProvider);
    void preRemove(RealmModel realm, IdentityProviderModel provider);
    void updateFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel federatedIdentityModel);

    /**
     * @deprecated Use {@link #getFederatedIdentitiesStream(String, RealmModel) getFederatedIdentitiesStream} instead.
     */
    @Deprecated
    Set<FederatedIdentityModel> getFederatedIdentities(String userId, RealmModel realm);

    /**
     * Obtains the identities of the federated user identified by {@code userId}.
     *
     * @param userId the user identifier.
     * @param realm a reference to the realm.
     * @return a non-null {@link Stream} of federated identities associated with the user.
     */
    default Stream<FederatedIdentityModel> getFederatedIdentitiesStream(String userId, RealmModel realm) {
        Set<FederatedIdentityModel> value = this.getFederatedIdentities(userId, realm);
        return value != null ? value.stream() : Stream.empty();
    }

    FederatedIdentityModel getFederatedIdentity(String userId, String socialProvider, RealmModel realm);

    /**
     * The {@link Streams} interface makes all collection-based methods in {@link UserBrokerLinkFederatedStorage}
     * default by providing implementations that delegate to the {@link Stream}-based variants instead of the other way
     * around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends UserBrokerLinkFederatedStorage {
        @Override
        default Set<FederatedIdentityModel> getFederatedIdentities(String userId, RealmModel realm) {
            return this.getFederatedIdentitiesStream(userId, realm).collect(Collectors.toSet());
        }

        @Override
        Stream<FederatedIdentityModel> getFederatedIdentitiesStream(String userId, RealmModel realm);
    }
}
