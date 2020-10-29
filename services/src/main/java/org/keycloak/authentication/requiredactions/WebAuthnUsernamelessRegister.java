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

package org.keycloak.authentication.requiredactions;

import com.webauthn4j.validator.attestation.trustworthiness.certpath.CertPathTrustworthinessValidator;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.credential.WebAuthnUsernamelessCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.credential.WebAuthnCredentialModel;

/**
 * Required action for register WebAuthn usernameless credential for the user. This class is temporary and will be likely
 * removed in the future during future improvements in authentication SPI
 *
 */
public class WebAuthnUsernamelessRegister extends WebAuthnRegister {

    public WebAuthnUsernamelessRegister(KeycloakSession session, CertPathTrustworthinessValidator certPathtrustValidator) {
        super(session, certPathtrustValidator);
    }

    @Override
    protected WebAuthnPolicy getWebAuthnPolicy(RequiredActionContext context) {
        return context.getRealm().getWebAuthnPolicyUsernameless();
    }

    @Override
    protected String getCredentialType() {
        return WebAuthnCredentialModel.TYPE_USERNAMELESS;
    }

    @Override
    protected String getCredentialProviderId() {
        return WebAuthnUsernamelessCredentialProviderFactory.PROVIDER_ID;
    }


}
