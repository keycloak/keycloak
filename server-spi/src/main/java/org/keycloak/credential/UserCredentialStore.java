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
package org.keycloak.credential;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserCredentialStore extends Provider {
    void updateCredential(RealmModel realm, UserModel user, CredentialModel cred);
    CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred);
    boolean removeStoredCredential(RealmModel realm, UserModel user, String id);
    CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id);
    List<CredentialModel> getStoredCredentials(RealmModel realm, UserModel user);
    List<CredentialModel> getStoredCredentialsByType(RealmModel realm, UserModel user, String type);
    CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type);

    //list operations
    boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId);

}
