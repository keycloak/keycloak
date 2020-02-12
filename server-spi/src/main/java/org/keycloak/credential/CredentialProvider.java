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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface CredentialProvider<T extends CredentialModel> extends Provider {

    @Override
    default void close() {

    }

    String getType();

    CredentialModel createCredential(RealmModel realm, UserModel user, T credentialModel);

    void deleteCredential(RealmModel realm, UserModel user, String credentialId);

    T getCredentialFromModel(CredentialModel model);

    default T getDefaultCredential(KeycloakSession session, RealmModel realm, UserModel user) {
        List<CredentialModel> models = session.userCredentialManager().getStoredCredentialsByType(realm, user, getType());
        if (models.isEmpty()) {
            return null;
        }
        return getCredentialFromModel(models.get(0));
    }

    CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext);
}
