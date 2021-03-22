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
package org.keycloak.storage.federated;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserFederatedUserCredentialStore extends Provider {
    void updateCredential(RealmModel realm, String userId, CredentialModel cred);
    CredentialModel createCredential(RealmModel realm, String userId, CredentialModel cred);
    boolean removeStoredCredential(RealmModel realm, String userId, String id);
    CredentialModel getStoredCredentialById(RealmModel realm, String userId, String id);
    List<CredentialModel> getStoredCredentials(RealmModel realm, String userId);
    List<CredentialModel> getStoredCredentialsByType(RealmModel realm, String userId, String type);
    CredentialModel getStoredCredentialByNameAndType(RealmModel realm, String userId, String name, String type);
}
