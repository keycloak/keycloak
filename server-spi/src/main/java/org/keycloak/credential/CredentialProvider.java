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

    boolean deleteCredential(RealmModel realm, UserModel user, String credentialId);

    T getCredentialFromModel(CredentialModel model);

    /**
     * Get the credential (usually stored credential retrieved from the DB) and decorates it with additional metadata
     * to be present for example in the admin console. Those additional metadata could be various metadata, which are not saved in the DB,
     * but can be retrieved from saved data to be presented to admins/users in the nice way (For example display "authenticator Provider"
     * for WebAuthn credential based on the AAGUID of WebAuthn credential)
     *
     * @param model stored credential retrieved from the DB
     * @return credential model useful for the presentation (not necessarily only stored data, but possibly some other metadata added)
     */
    default T getCredentialForPresentationFromModel(CredentialModel model) {
        T presentationModel = getCredentialFromModel(model);
        presentationModel.setFederationLink(model.getFederationLink());
        return presentationModel;
    }

    default T getDefaultCredential(KeycloakSession session, RealmModel realm, UserModel user) {
        CredentialModel model = user.credentialManager().getStoredCredentialsByTypeStream(getType())
                .findFirst().orElse(null);
        return model != null ? getCredentialFromModel(model) : null;
    }

    CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext);

    default CredentialMetadata getCredentialMetadata(T credentialModel, CredentialTypeMetadata credentialTypeMetadata) {
        CredentialMetadata credentialMetadata = new CredentialMetadata();
        credentialMetadata.setCredentialModel(credentialModel);
        return credentialMetadata;
    }

    default boolean supportsCredentialType(CredentialModel credential) {
        return supportsCredentialType(credential.getType());
    }

    default boolean supportsCredentialType(String type) {
        return getType().equals(type);
    }
}
