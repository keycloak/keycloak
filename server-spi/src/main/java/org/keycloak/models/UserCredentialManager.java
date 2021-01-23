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

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserCredentialManager extends UserCredentialStore {

    /**
     * Validates list of credentials.  Will call UserStorageProvider and UserFederationProviders first, then loop through
     * each CredentialProvider.
     *
     * @param realm
     * @param user
     * @param inputs
     * @return
     */
    boolean isValid(RealmModel realm, UserModel user, List<CredentialInput> inputs);

    /**
     * Validates list of credentials.  Will call UserStorageProvider and UserFederationProviders first, then loop through
     * each CredentialProvider.
     *
     * @param realm
     * @param user
     * @param inputs
     * @return
     */
    boolean isValid(RealmModel realm, UserModel user, CredentialInput... inputs);

    /**
     * Updates a credential.  Will call UserStorageProvider and UserFederationProviders first, then loop through
     * each CredentialProvider.  Update is finished whenever any one provider returns true.
     *
     * @param realm
     * @param user
     * @return true if credential was successfully updated by UserStorage or any CredentialInputUpdater
     */
    boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input);

    /**
     * Creates a credential from the credentialModel, by looping through the providers to find a match for the type
     * @param realm
     * @param user
     * @param model
     * @return
     */
    CredentialModel createCredentialThroughProvider(RealmModel realm, UserModel user, CredentialModel model);

    /**
     * Updates the credential label and invalidates the cache for the user.
     * @param realm
     * @param user
     * @param credentialId
     * @param userLabel
     */
    void updateCredentialLabel(RealmModel realm, UserModel user, String credentialId, String userLabel);

    /**
     * Calls disableCredential on UserStorageProvider and UserFederationProviders first, then loop through
     * each CredentialProvider.
     *
     * @param realm
     * @param user
     * @param credentialType
     */
    void disableCredentialType(RealmModel realm, UserModel user, String credentialType);

    /**
     * Returns a set of credential types that can be disabled by disableCredentialType() method
     *
     * @param realm
     * @param user
     * @return
     * @deprecated Use {@link #getDisableableCredentialTypesStream(RealmModel, UserModel) getDisableableCredentialTypesStream}
     * instead.
     */
    @Deprecated
    Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user);

    /**
     * Obtains the credential types that can be disabled by means of the {@link #disableCredentialType(RealmModel, UserModel, String)}
     * method.
     *
     * @param realm a reference to the realm.
     * @param user the user whose credentials are being searched.
     * @return a non-null {@link Stream} of credential types.
     */
    default Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        Set<String> result = this.getDisableableCredentialTypes(realm, user);
        return result != null ? result.stream() : Stream.empty();
    }

    /**
     * Checks to see if user has credential type configured.  Looks in UserStorageProvider or UserFederationProvider first,
     * then loops through each CredentialProvider.
     *
     * @param realm
     * @param user
     * @param type
     * @return
     */
    boolean isConfiguredFor(RealmModel realm, UserModel user, String type);

    /**
     * Only loops through each CredentialProvider to see if credential type is configured for the user.
     * This allows UserStorageProvider and UserFederationProvider isValid() implementations to punt to local storage
     * when validating a credential that has been overriden in Keycloak storage.
     *
     * @param realm
     * @param user
     * @param type
     * @return
     */
    boolean isConfiguredLocally(RealmModel realm, UserModel user, String type);

    /**
     * Given a CredentialInput, authenticate the user.  This is used in the case where the credential must be processed
     * to determine and find the user.  An example is Kerberos where the kerberos token might be validated and processed
     * by a variety of different storage providers.
     *
     *
     * @param session
     * @param realm
     * @param input
     * @return
     */
    CredentialValidationOutput authenticate(KeycloakSession session, RealmModel realm, CredentialInput input);

    /**
     * Return credential types, which are provided by the user storage where user is stored. Returned values can contain for example "password", "otp" etc.
     * This will always return empty list for "local" users, which are not backed by any user storage
     *
     * @return
     * @deprecated Use {@link #getConfiguredUserStorageCredentialTypesStream(RealmModel, UserModel) getConfiguredUserStorageCredentialTypesStream}
     * instead.
     */
    @Deprecated
    List<String> getConfiguredUserStorageCredentialTypes(RealmModel realm, UserModel user);

    /**
     * Obtains the credential types provided by the user storage where the specified user is stored. Examples of returned
     * values are "password", "otp", etc.
     * <p/>
     * This method will always return an empty stream for "local" users - i.e. users that are not backed by any user storage.
     *
     * @param realm a reference to the realm.
     * @param user a reference to the user.
     * @return a non-null {@link Stream} of credential types.
     */
    default Stream<String> getConfiguredUserStorageCredentialTypesStream(RealmModel realm, UserModel user) {
        List<String> result = this.getConfiguredUserStorageCredentialTypes(realm, user);
        return result != null ? result.stream() : Stream.empty();
    }

    /**
     * The {@link UserCredentialManager.Streams} interface makes all collection-based methods in {@link UserCredentialManager}
     * default by providing implementations that delegate to the {@link Stream}-based variants instead of the other way around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends UserCredentialManager, UserCredentialStore.Streams {
        @Override
        default Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
            return this.getDisableableCredentialTypesStream(realm, user).collect(Collectors.toSet());
        }

        @Override
        Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user);

        @Override
        default List<String> getConfiguredUserStorageCredentialTypes(RealmModel realm, UserModel user) {
            return this.getConfiguredUserStorageCredentialTypesStream(realm, user).collect(Collectors.toList());
        }

        @Override
        Stream<String> getConfiguredUserStorageCredentialTypesStream(RealmModel realm, UserModel user);
    }
}
