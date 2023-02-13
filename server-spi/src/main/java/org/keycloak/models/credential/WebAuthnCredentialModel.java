/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.models.credential;

import java.io.IOException;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.models.credential.dto.WebAuthnSecretData;
import org.keycloak.util.JsonSerialization;

import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnCredentialModel extends CredentialModel {

    // Credential type used for WebAuthn two factor credentials
    public static final String TYPE_TWOFACTOR = "webauthn";

    // Credential type used for WebAuthn passwordless credentials
    public static final String TYPE_PASSWORDLESS = "webauthn-passwordless";

    // Either
    private final WebAuthnCredentialData credentialData;
    private final WebAuthnSecretData secretData;

    private WebAuthnCredentialModel(String credentialType, WebAuthnCredentialData credentialData, WebAuthnSecretData secretData) {
        this.credentialData = credentialData;
        this.secretData = secretData;
        setType(credentialType);
    }

    public static WebAuthnCredentialModel create(String credentialType, String userLabel, String aaguid, String credentialId,
                                                 String attestationStatement, String credentialPublicKey, long counter, String attestationStatementFormat) {
        return create(credentialType, userLabel, aaguid, credentialId, attestationStatement, credentialPublicKey, counter, attestationStatementFormat, Collections.emptySet());
    }

    public static WebAuthnCredentialModel create(String credentialType, String userLabel, String aaguid, String credentialId,
                                                 String attestationStatement, String credentialPublicKey, long counter, String attestationStatementFormat, Set<String> transports) {
        WebAuthnCredentialData credentialData = new WebAuthnCredentialData(aaguid, credentialId, counter, attestationStatement, credentialPublicKey, attestationStatementFormat, transports);
        WebAuthnSecretData secretData = new WebAuthnSecretData();

        WebAuthnCredentialModel credentialModel = new WebAuthnCredentialModel(credentialType, credentialData, secretData);
        credentialModel.fillCredentialModelFields();
        credentialModel.setUserLabel(userLabel);
        return credentialModel;
    }


    public static WebAuthnCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        try {
            WebAuthnCredentialData credentialData = JsonSerialization.readValue(credentialModel.getCredentialData(), WebAuthnCredentialData.class);
            WebAuthnSecretData secretData = JsonSerialization.readValue(credentialModel.getSecretData(), WebAuthnSecretData.class);

            WebAuthnCredentialModel webAuthnCredentialModel = new WebAuthnCredentialModel(credentialModel.getType(), credentialData, secretData);
            webAuthnCredentialModel.setUserLabel(credentialModel.getUserLabel());
            webAuthnCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
            webAuthnCredentialModel.setType(credentialModel.getType());
            webAuthnCredentialModel.setId(credentialModel.getId());
            webAuthnCredentialModel.setSecretData(credentialModel.getSecretData());
            webAuthnCredentialModel.setCredentialData(credentialModel.getCredentialData());
            return webAuthnCredentialModel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCounter(long counter) {
        credentialData.setCounter(counter);
        try {
            setCredentialData(JsonSerialization.writeValueAsString(credentialData));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public WebAuthnCredentialData getWebAuthnCredentialData() {
        return credentialData;
    }


    public WebAuthnSecretData getWebAuthnSecretData() {
        return secretData;
    }


    private void fillCredentialModelFields() {
        try {
            setCredentialData(JsonSerialization.writeValueAsString(credentialData));
            setSecretData(JsonSerialization.writeValueAsString(secretData));
            setCreatedDate(Time.currentTimeMillis());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String toString() {
        return "WebAuthnCredentialModel { " +
                getType() +
                ", " + credentialData +
                ", " + secretData +
                " }";
    }
}
