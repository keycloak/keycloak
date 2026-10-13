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

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * This is an optional capability interface that is intended to be implemented by any
 * <code>UserStorageProvider</code> that supports addition of new users. You must
 * implement this interface if you want to use this storage for registering new users.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserRegistrationProvider {

    /**
     * All storage providers that implement this interface will be looped through.
     * If this method returns null, then the next storage provider's addUser() method will be called.
     * If no storage providers handle the add, then the user will be created in local storage.
     *
     * Returning null is useful when you want optional support for adding users.  For example,
     * our LDAP provider can enable and disable the ability to add users.
     *
     * @param realm a reference to the realm
     * @param username a username the created user will be assigned
     * @return a model of created user
     */
    UserModel addUser(RealmModel realm, String username);

    /**
     * Called if user originated from this provider.
     *
     *
     * If a local user is linked to this provider, this method will be called before
     * local storage's removeUser() method is invoked.

     * If you are using an import strategy, and this is a local user linked to this provider,
     * this method will be called before local storage's removeUser() method is invoked.  Also,
     * you DO NOT need to remove the imported user.  The runtime will handle this for you.
     *
     * @param realm a reference to the realm
     * @param user a reference to the user that is removed
     * @return true if the user was removed, false otherwise
     */
    boolean removeUser(RealmModel realm, UserModel user);


}
