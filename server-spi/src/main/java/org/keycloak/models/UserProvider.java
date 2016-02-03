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

import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserProvider extends Provider {
    // Note: The reason there are so many query methods here is for layering a cache on top of an persistent KeycloakSession

    UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions);
    UserModel addUser(RealmModel realm, String username);
    boolean removeUser(RealmModel realm, UserModel user);

    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink);
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider);
    void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel);

    UserModel getUserById(String id, RealmModel realm);
    UserModel getUserByUsername(String username, RealmModel realm);
    UserModel getUserByEmail(String email, RealmModel realm);

    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults);

    UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm);
    UserModel getUserByServiceAccountClient(ClientModel client);
    List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts);

    // Service account is included for counts
    int getUsersCount(RealmModel realm);
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group);
    List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts);
    List<UserModel> searchForUser(String search, RealmModel realm);
    List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults);
    List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm);
    List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults);

    // Searching by UserModel.attribute (not property)
    List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm);

    Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm);
    FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm);

    void grantToAllUsers(RealmModel realm, RoleModel role);

    void preRemove(RealmModel realm);

    void preRemove(RealmModel realm, UserFederationProviderModel link);

    void preRemove(RealmModel realm, RoleModel role);
    void preRemove(RealmModel realm, GroupModel group);

    void preRemove(RealmModel realm, ClientModel client);
    void preRemove(ProtocolMapperModel protocolMapper);

    boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, List<UserCredentialModel> input);
    boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, UserCredentialModel... input);
    CredentialValidationOutput validCredentials(KeycloakSession session, RealmModel realm, UserCredentialModel... input);

    void close();
}
