/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.verifier;

import org.keycloak.common.VerificationException;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.util.MtlsHoKTokenUtil;

public class MtlsTokenVerifierProvider implements TokenVerifierProvider {

    @Override
    public void additionalAccessTokenVerifications(TokenVerifierProviderContext context) {
        // mTLS adds only a binding check against the certificate presented in the request.
    }

    @Override
    public void verifyTokenBinding(AccessToken token, TokenVerificationContext context) throws VerificationException {
        if (isCertificateBound(token)) {
            verifyCertificate(token, context);
        } else {
            ClientModel client = context.realm().getClientByClientId(token.getIssuedFor());
            if (client != null && OIDCAdvancedConfigWrapper.fromClientModel(client).isUseMtlsHokToken()) {
                throw new TokenBindingException(Errors.NOT_ALLOWED, MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC);
            }
        }
    }

    @Override
    public void verifyTokenExchangeBinding(AccessToken token, TokenVerificationContext context) throws VerificationException {
        if (isCertificateBound(token)) {
            verifyCertificate(token, context);
        }
    }

    private boolean isCertificateBound(AccessToken token) {
        return token.getConfirmation() != null && token.getConfirmation().getCertThumbprint() != null;
    }

    private void verifyCertificate(AccessToken token, TokenVerificationContext context) throws VerificationException {
        if (!MtlsHoKTokenUtil.verifyTokenBindingWithClientCertificate(token, context.request(), context.session())) {
            throw new TokenBindingException(Errors.NOT_ALLOWED, MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC);
        }
    }
}
