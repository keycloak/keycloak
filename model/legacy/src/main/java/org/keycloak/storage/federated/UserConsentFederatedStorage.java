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

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserConsentFederatedStorage {
    void addConsent(RealmModel realm, String userId, UserConsentModel consent);
    UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientInternalId);

    /**
     * @deprecated Use {@link #getConsentsStream(RealmModel, String) getConsentsStream} instead.
     */
    @Deprecated
    List<UserConsentModel> getConsents(RealmModel realm, String userId);

    /**
     * Obtains the consents associated with the federated user identified by {@code userId}.
     *
     * @param realm a reference to the realm.
     * @param userId the user identifier.
     * @return a non-null {@link Stream} of consents associated with the user.
     */
    default Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId) {
        List<UserConsentModel> value = this.getConsents(realm, userId);
        return value != null ? value.stream() : Stream.empty();
    }

    void updateConsent(RealmModel realm, String userId, UserConsentModel consent);
    boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId);

    /**
     * The {@link Streams} interface makes all collection-based methods in {@link UserConsentFederatedStorage}
     * default by providing implementations that delegate to the {@link Stream}-based variants instead of the other way
     * around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends UserConsentFederatedStorage {
        @Override
        default List<UserConsentModel> getConsents(RealmModel realm, String userId) {
            return this.getConsentsStream(realm, userId).collect(Collectors.toList());
        }

        @Override
        Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId);
    }
}
