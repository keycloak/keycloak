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
package org.keycloak.storage;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.provider.Provider;

/**
 * A class implementing this interface represents a user storage provider to Keycloak.
 * <p/>
 * This interface contains only very basic methods for manipulating users. However, the storage provider capabilities
 * are extended by implementing one or more of the following capability interfaces:
 * <ul>
 *     <li>{@link org.keycloak.storage.user.UserLookupProvider UserLookupProvider} - Provide basic lookup methods. After implementing it is possible to login using users from the storage.</li>
 *     <li>{@link org.keycloak.storage.user.UserQueryMethodsProvider UserQueryMethodsProvider} - Provide complex lookup methods. After implementing it is possible to manage users from admin console.</li>
 *     <li>{@link org.keycloak.storage.user.UserCountMethodsProvider UserCountMethodsProvider} - Provide complex count methods. After implementing it is possible to leverage optimizations during querying for users.</li>
 *     <li>{@link org.keycloak.storage.user.UserQueryProvider UserQueryProvider} - This interface is combined capability of {@code UserQueryMethodsProvider} and {@code UserCountMethodsProvider}.</li>
 *     <li>{@link org.keycloak.storage.user.UserRegistrationProvider UserRegistrationProvider} - Provide methods for adding users. After implementing it is possible to store registered users in the storage.</li>
 *     <li>{@link org.keycloak.storage.user.UserBulkUpdateProvider UserBulkUpdateProvider} - After implementing it is possible to perform bulk operations on all users from storage (for example, addition of a role to all users).</li>
 *     <li>{@link org.keycloak.storage.user.ImportedUserValidation ImportedUserValidation} - Provider method for validating users within Keycloak local storage that are imported from the storage.</li>
 * </ul>
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserStorageProvider extends Provider {


    /**
     * Callback when a realm is removed.  Implement this if, for example, you want to do some
     * cleanup in your user storage when a realm is removed
     *
     * @param realm
     */
    default void preRemove(RealmModel realm) {

    }

    /**
     * Callback when a group is removed.  Allows you to do things like remove a user
     * group mapping in your external store if appropriate
     *
     * @param realm
     * @param group
     */
    default void preRemove(RealmModel realm, GroupModel group) {

    }

    /**
     * Callback when a role is removed.  Allows you to do things like remove a user
     * role mapping in your external store if appropriate
     *
     * @param realm
     * @param role
     */
    default void preRemove(RealmModel realm, RoleModel role) {

    }

    /**
     * Optional type that can be used by implementations to
     * describe edit mode of user storage
     */
    enum EditMode {
        /**
         * user storage is read-only
         */
        READ_ONLY,
        /**
         * user storage is writable
         */
        WRITABLE,
        /**
         * updates to user are stored locally and not synced with user storage.
         */
        UNSYNCED
    }
}
