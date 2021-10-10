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
 * {@link org.keycloak.storage.UserStorageProvider UserStorageProvider} that supports validating users. You must
 * implement this interface if your storage imports users into the Keycloak local storage and you want to sync these
 * users with your storage. The idea is, that whenever keycloak queries users imported from your storage, the method
 * {@link #validate(RealmModel, UserModel) validate()} is called and if it returns null, the user is removed from
 * local storage and reloaded from your storage by corresponding method.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ImportedUserValidation {
    /**
     * If this method returns null, then the user in local storage will be removed
     *
     * @param realm
     * @param user
     * @return null if user no longer valid
     */
    UserModel validate(RealmModel realm, UserModel user);
}
