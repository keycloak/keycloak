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

package org.keycloak.credential;

import org.keycloak.authentication.authenticators.browser.WebAuthnMetadataService;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.credential.WebAuthnCredentialModel;

import com.webauthn4j.converter.util.ObjectConverter;

/**
 * Credential provider for WebAuthn passwordless credential of the user
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnPasswordlessCredentialProvider extends WebAuthnCredentialProvider {

    public WebAuthnPasswordlessCredentialProvider(KeycloakSession session, WebAuthnMetadataService metadataService, ObjectConverter objectConverter) {
        super(session, metadataService, objectConverter);
    }

    @Override
    public String getType() {
        return WebAuthnCredentialModel.TYPE_PASSWORDLESS;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.PASSWORDLESS)
                .displayName("webauthn-passwordless-display-name")
                .helpText("webauthn-passwordless-help-text")
                .iconCssClass("kcAuthenticatorWebAuthnPasswordlessClass")
                .createAction(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID)
                .removeable(true)
                .build(getKeycloakSession());
    }

    @Override
    protected WebAuthnPolicy getWebAuthnPolicy() {
        return getKeycloakSession().getContext().getRealm().getWebAuthnPolicyPasswordless();
    }
}
