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
import org.keycloak.storage.user.UserCredentialValidatorProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserProvider extends Provider,
        UserLookupProvider,
        UserQueryProvider,
        UserCredentialValidatorProvider,
        UserRegistrationProvider {
    // Note: The reason there are so many query methods here is for layering a cache on top of an persistent KeycloakSession

    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink);
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider);
    void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel);
    Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm);
    FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm);
    UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm);

    void addConsent(RealmModel realm, UserModel user, UserConsentModel consent);
    UserConsentModel getConsentByClient(RealmModel realm, UserModel user, String clientInternalId);
    List<UserConsentModel> getConsents(RealmModel realm, UserModel user);
    void updateConsent(RealmModel realm, UserModel user, UserConsentModel consent);
    boolean revokeConsentForClient(RealmModel realm, UserModel user, String clientInternalId);


    UserModel getServiceAccount(ClientModel client);
    List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts);
    List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts);

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

    void preRemove(RealmModel realm, UserFederationProviderModel link);

    void preRemove(RealmModel realm, RoleModel role);
    void preRemove(RealmModel realm, GroupModel group);

    void preRemove(RealmModel realm, ClientModel client);
    void preRemove(ProtocolMapperModel protocolMapper);


    boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, UserCredentialModel... input);
    CredentialValidationOutput validCredentials(KeycloakSession session, RealmModel realm, UserCredentialModel... input);


    void close();

    void preRemove(RealmModel realm, ComponentModel component);
}
