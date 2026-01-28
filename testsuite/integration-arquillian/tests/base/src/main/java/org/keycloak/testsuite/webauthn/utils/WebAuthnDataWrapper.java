/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn.utils;

import java.io.Serializable;

import org.keycloak.common.util.Base64Url;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;

import com.webauthn4j.converter.util.CborConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.authenticator.COSEKey;

import static org.keycloak.models.credential.WebAuthnCredentialModel.createFromCredentialModel;

/**
 * Helper class for WebAuthn data wrapping
 *
 * @author Martin Bartos <mabartos@redhat.com>
 */
public class WebAuthnDataWrapper implements Serializable {
    private static final ObjectConverter converter = new ObjectConverter();

    private final KeycloakSession session;
    private final String username;
    private final String credentialType;
    private WebAuthnCredentialData webAuthnData = null;

    public WebAuthnDataWrapper(KeycloakSession session, String username, String credentialType) {
        this.session = session;
        this.username = username;
        this.credentialType = credentialType;
        init();
    }

    private void init() {
        final UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), username);
        if (user == null) return;

        SubjectCredentialManager userCredentialManager = user.credentialManager();
        if (userCredentialManager == null) return;

        final CredentialModel credential = userCredentialManager
                .getStoredCredentialsByTypeStream(credentialType)
                .findFirst()
                .orElse(null);

        if (credential == null) return;

        this.webAuthnData = createFromCredentialModel(credential).getWebAuthnCredentialData();
    }

    public COSEKey getKey() {
        if (webAuthnData != null) {
            CborConverter cborConverter = converter.getCborConverter();
            return cborConverter.readValue(Base64Url.decode(webAuthnData.getCredentialPublicKey()), COSEKey.class);
        }
        return null;
    }

    public WebAuthnCredentialData getWebAuthnData() {
        return webAuthnData;
    }
}