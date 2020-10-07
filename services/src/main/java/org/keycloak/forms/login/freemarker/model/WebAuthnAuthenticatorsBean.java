/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.forms.login.freemarker.model;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.common.util.Base64Url;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;

public class WebAuthnAuthenticatorsBean {
    private List<WebAuthnAuthenticatorBean> authenticators = new LinkedList<WebAuthnAuthenticatorBean>();

    public WebAuthnAuthenticatorsBean(KeycloakSession session, RealmModel realm, UserModel user, String credentialType) {
        // should consider multiple credentials in the future, but only single credential supported now.
        for (CredentialModel credential : session.userCredentialManager().getStoredCredentialsByType(realm, user, credentialType)) {
            WebAuthnCredentialModel webAuthnCredential = WebAuthnCredentialModel.createFromCredentialModel(credential);

            String credentialId = Base64Url.encodeBase64ToBase64Url(webAuthnCredential.getWebAuthnCredentialData().getCredentialId());
            String label = (webAuthnCredential.getUserLabel()==null || webAuthnCredential.getUserLabel().isEmpty()) ? "label missing" : webAuthnCredential.getUserLabel();
            authenticators.add(new WebAuthnAuthenticatorBean(credentialId, label));
        }
    }

    public List<WebAuthnAuthenticatorBean> getAuthenticators() {
        return authenticators;
    }

    public static class WebAuthnAuthenticatorBean {
        private final String credentialId;
        private final String label;

        public WebAuthnAuthenticatorBean(String credentialId, String label) {
            this.credentialId = credentialId;
            this.label = label;
        }

        public String getCredentialId() {
            return this.credentialId;
        }

        public String getLabel() {
            return this.label;
        }
    }
}
