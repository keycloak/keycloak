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
 * SPI for plugging in federation storage.  This class is instantiated once per session/request and is closed after
 * the session/request is finished.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserFederationProvider extends Provider {

    public static final String USERNAME = UserModel.USERNAME;
    public static final String EMAIL = UserModel.EMAIL;
    public static final String FIRST_NAME = UserModel.FIRST_NAME;
    public static final String LAST_NAME = UserModel.LAST_NAME;

    /**
     * Optional type that can be by implementations to describe edit mode of federation storage
     *
     */
    enum EditMode {
        /**
         * federation storage is read-only
         */
        READ_ONLY,
        /**
         * federation storage is writable
         *
         */
        WRITABLE,
        /**
         * updates to user are stored locally and not synced with federation storage.
         *
         */
        UNSYNCED
    }


    /**
     * Gives the provider an option to validate if user still exists in federation backend and then proxy UserModel loaded from local storage.
     * This method is called whenever a UserModel is pulled from Keycloak local storage.
     * For example, the LDAP provider proxies the UserModel and does on-demand synchronization with
     * LDAP whenever UserModel update methods are invoked.  It also overrides UserModel.updateCredential for the
     * credential types it supports
     *
     * @param realm
     * @param local
     * @return null if user is no longer valid or proxy object otherwise
     */
    UserModel validateAndProxy(RealmModel realm, UserModel local);

    /**
     * Should user registrations be synchronized with this provider?
     * FYI, only one provider will be chosen (by priority) to have this synchronization
     *
     * @return
     */
    boolean synchronizeRegistrations();

    /**
     * Called if this federation provider has priority and supports synchronized registrations.
     *
     * @param realm
     * @param user
     * @return
     */
    UserModel register(RealmModel realm, UserModel user);
    boolean removeUser(RealmModel realm, UserModel user);

    /**
     * Keycloak will search for user in local storage first.  If it can't find the UserModel is local storage,
     * it will call this method.  You are required to import the returned UserModel into local storage.
     *
     * @param realm
     * @param username
     * @return
     */
    UserModel getUserByUsername(RealmModel realm, String username);

    /**
     * Keycloak will search for user in local storage first.  If it can't find the UserModel is local storage,
     * it will call this method.  You are required to import the returned UserModel into local storage.
     *
     * @param realm
     * @param email
     * @return
     */
    UserModel getUserByEmail(RealmModel realm, String email);

    /**
     * Keycloak does not search in local storage first before calling this method.  The implementation must check
     * to see if user is already in local storage (KeycloakSession.userStorage()) before doing an import.
     * Currently only attributes USERNAME, EMAIL, FIRST_NAME and LAST_NAME will be used.
     *
     * @param attributes
     * @param realm
     * @return
     */
    List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults);

    /**
     * Return group members from federation storage. Useful if info about group memberships is stored in the federation storage.
     * Return empty list if your federation provider doesn't support storing user-group memberships
     *
     * @param realm
     * @param group
     * @param firstResult
     * @param maxResults
     * @return
     */
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults);

    /**
     * called whenever a Realm is removed
     *
     * @param realm
     */
    void preRemove(RealmModel realm);

    /**
     * called before a role is removed.
     *
     * @param realm
     * @param role
     */
    void preRemove(RealmModel realm, RoleModel role);

    /**
     * called before a role is removed.
     *
     * @param realm
     * @param group
     */
    void preRemove(RealmModel realm, GroupModel group);

    /**
     * Is the Keycloak UserModel still valid and/or existing in federated storage?  Keycloak may call this method
     * in various user operations.  The local storage may be deleted if this method returns false.
     *
     * @param realm
     * @param local
     * @return
     */
    boolean isValid(RealmModel realm, UserModel local);

    /**
     * What UserCredentialModel types should be handled by this provider for this user?  Keycloak will only call
     * validCredentials() with the credential types specified in this method.
     *
     * @return
     */
    Set<String> getSupportedCredentialTypes(UserModel user);

    /**
     * What UserCredentialModel types should be handled by this provider? This is called in scenarios when we don't know user,
     * who is going to authenticate (For example Kerberos authentication).
     *
     * @return
     */
    Set<String> getSupportedCredentialTypes();

    /**
     * Validate credentials for this user.  This method will only be called with credential parameters supported
     * by this provider
     *
     * @param realm
     * @param user
     * @param input
     * @return
     */
    boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input);
    boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input);

    /**
     * Validate credentials of unknown user. The authenticated user is recognized based on provided credentials and returned back in CredentialValidationOutput
     * @param realm
     * @param credential
     * @return
     */
    CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel credential);

    /**
     * This method is called at the end of requests.
     *
     */
    void close();



}
