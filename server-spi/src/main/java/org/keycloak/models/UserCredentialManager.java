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
import org.keycloak.credential.UserCredentialStore;

import java.util.List;
import java.util.Set;

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
     * @return
     */
    void updateCredential(RealmModel realm, UserModel user, CredentialInput input);

    /**
     * Calls disableCredential on UserStorageProvider and UserFederationProviders first, then loop through
     * each CredentialProvider.
     *
     * @param realm
     * @param user
     * @param credentialType
     */
    void disableCredential(RealmModel realm, UserModel user, String credentialType);

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
     * This allows UserStorageProvider and UserFederationProvider to look to abort isValid
     *
     * @param realm
     * @param user
     * @param type
     * @return
     */
    boolean isConfiguredLocally(RealmModel realm, UserModel user, String type);
}
