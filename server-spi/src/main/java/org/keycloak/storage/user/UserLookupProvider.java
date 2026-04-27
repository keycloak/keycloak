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
package org.keycloak.storage.user;

import org.keycloak.credential.CredentialInput;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * This is an optional capability interface that is intended to be implemented by any
 * <code>UserStorageProvider</code> that supports basic user querying. You must
 * implement this interface if you want to be able to log in to keycloak using users from your storage.
 * <p/>
 * Note that all methods in this interface should limit search only to data available within the storage that is
 * represented by this provider. They should not lookup other storage providers for additional information.
 * Optional capability interface implemented by UserStorageProviders.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserLookupProvider {

    /**
     * Returns a user with the given id belonging to the realm
     *
     * @param id id of the user
     * @param realm the realm model
     * @return found user model, or {@code null} if no such user exists
     */
    UserModel getUserById(RealmModel realm, String id);

    /**
     * Exact search for a user by its username.
     * Returns a user with the given username belonging to the realm
     *
     * @param username (case-sensitivity is controlled by storage)
     * @param realm the realm model
     * @return found user model, or {@code null} if no such user exists
     * @throws org.keycloak.models.ModelDuplicateException when searched with case 
     * insensitive mode and there are more users with username which differs only 
     * by case 
     */
    UserModel getUserByUsername(RealmModel realm, String username);

    default CredentialValidationOutput getUserByCredential(RealmModel realm, CredentialInput input) {
        return null;
    }

    /**
     * Returns a user with the given email belonging to the realm
     *
     * @param email email address
     * @param realm the realm model
     * @return found user model, or {@code null} if no such user exists
     *
     * @throws org.keycloak.models.ModelDuplicateException when there are more users with same email
     */
    UserModel getUserByEmail(RealmModel realm, String email);
}
