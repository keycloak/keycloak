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

package org.keycloak.models;

import org.keycloak.component.ComponentModel;
import org.keycloak.provider.Provider;
import org.keycloak.storage.user.UserBulkUpdateProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserProvider extends Provider,
        UserLookupProvider,
        UserQueryProvider,
        UserRegistrationProvider,
        UserBulkUpdateProvider {
    // Note: The reason there are so many query methods here is for layering a cache on top of an persistent KeycloakSession

    void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink);
    boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider);
    void preRemove(RealmModel realm, IdentityProviderModel provider);
    void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel);

    /**
     * @deprecated Use {@link #getFederatedIdentitiesStream(UserModel, RealmModel) getFederatedIdentitiesStream} instead.
     */
    @Deprecated
    Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm);

    /**
     * Obtains the federated identities of the specified user.
     *
     * @param user a reference to the user.
     * @param realm a reference to the realm.
     * @return a non-null {@link Stream} of federated identities associated with the user.
     */
    default Stream<FederatedIdentityModel> getFederatedIdentitiesStream(UserModel user, RealmModel realm) {
        Set<FederatedIdentityModel> value = this.getFederatedIdentities(user, realm);
        return value != null ? value.stream() : Stream.empty();
    }

    FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm);
    UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm);

    void addConsent(RealmModel realm, String userId, UserConsentModel consent);
    UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientInternalId);

    /**
     * @deprecated Use {@link #getConsentsStream(RealmModel, String) getConsentsStream} instead.
     */
    @Deprecated
    List<UserConsentModel> getConsents(RealmModel realm, String userId);

    /**
     * Obtains the consents associated with the user identified by the specified {@code userId}.
     *
     * @param realm a reference to the realm.
     * @param userId the user identifier.
     * @return a non-null {@link Stream} of consents associated with the user.
     */
    default Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId) {
        List<UserConsentModel> value = this.getConsents(realm, userId);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     *
     * @param realm
     * @param userId
     * @param consent
     * @throws ModelException when consent doesn't exist for the userId
     */
    void updateConsent(RealmModel realm, String userId, UserConsentModel consent);
    boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId);

    void setNotBeforeForUser(RealmModel realm, UserModel user, int notBefore);
    int getNotBeforeOfUser(RealmModel realm, UserModel user);

    /**
     *
     * @param client
     * @throws IllegalArgumentException when there are more service accounts associated with the given clientId
     * @return
     */
    UserModel getServiceAccount(ClientModel client);

    /**
     * @deprecated Use {@link #getUsersStream(RealmModel, boolean) getUsersStream} instead.
     */
    @Deprecated
    List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts);

    /**
     * Obtains the users associated with the specified realm.
     *
     * @param realm a reference to the realm being used for the search.
     * @param includeServiceAccounts {@code true} if service accounts should be included in the result; {@code false} otherwise.
     * @return a non-null {@link Stream} of users associated withe the realm.
     */
    default Stream<UserModel> getUsersStream(RealmModel realm, boolean includeServiceAccounts) {
        List<UserModel> value = this.getUsers(realm, includeServiceAccounts);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * @deprecated Use {@link #getUsersStream(RealmModel, Integer, Integer, boolean) getUsersStream} instead.
     */
    @Deprecated
    List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts);

    /**
     * Obtains the users associated with the specified realm.
     *
     * @param realm a reference to the realm being used for the search.
     * @param firstResult first result to return. Ignored if negative.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @param includeServiceAccounts {@code true} if service accounts should be included in the result; {@code false} otherwise.
     * @return a non-null {@link Stream} of users associated withe the realm.
     */
    default Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults, boolean includeServiceAccounts) {
        List<UserModel> value = this.getUsers(realm, firstResult == null ? -1 : firstResult, maxResults == null ? -1 : maxResults, includeServiceAccounts);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * only used for local storage
     *
     * @param realm
     * @param id
     * @param username
     * @param addDefaultRoles
     * @param addDefaultRequiredActions
     * @return
     */
    UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions);
    void preRemove(RealmModel realm);

    /**
     * Removes any imported users from a specific User Storage Provider.
     *
     * @param realm
     * @param storageProviderId
     */
    void removeImportedUsers(RealmModel realm, String storageProviderId);

    /**
     * Set federation link to null to imported users of a specific User Storage Provider
     *
     * @param realm
     * @param storageProviderId
     */
    void unlinkUsers(RealmModel realm, String storageProviderId);

    void preRemove(RealmModel realm, RoleModel role);
    void preRemove(RealmModel realm, GroupModel group);

    void preRemove(RealmModel realm, ClientModel client);
    void preRemove(ProtocolMapperModel protocolMapper);
    void preRemove(ClientScopeModel clientScope);

    void close();

    void preRemove(RealmModel realm, ComponentModel component);

    interface Streams extends UserProvider, UserQueryProvider.Streams {
        @Override
        default Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
            return this.getFederatedIdentitiesStream(user, realm).collect(Collectors.toSet());
        }

        @Override
        Stream<FederatedIdentityModel> getFederatedIdentitiesStream(UserModel user, RealmModel realm);

        @Override
        default List<UserConsentModel> getConsents(RealmModel realm, String userId) {
            return this.getConsentsStream(realm, userId).collect(Collectors.toList());
        }

        @Override
        Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId);

        @Override
        default List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
            return this.getUsersStream(realm, includeServiceAccounts).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> getUsersStream(RealmModel realm, boolean includeServiceAccounts);

        @Override
        default List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts) {
            return this.getUsersStream(realm, firstResult, maxResults, includeServiceAccounts).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults, boolean includeServiceAccounts);
    }
}
