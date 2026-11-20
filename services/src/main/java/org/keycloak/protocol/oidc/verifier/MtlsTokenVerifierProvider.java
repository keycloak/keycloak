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
        boolean certificateBound = token.getConfirmation() != null && token.getConfirmation().getCertThumbprint() != null;
        boolean valid = certificateBound
                ? MtlsHoKTokenUtil.verifyTokenBindingWithClientCertificate(token, context.request(), context.session())
                : !requiresCertificate(context, token);
        if (!valid) {
            throw new TokenBindingException(Errors.NOT_ALLOWED, MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC);
        }
    }

    private boolean requiresCertificate(TokenVerificationContext context, AccessToken token) {
        if (!context.mode().isResourceRequest()) {
            return false;
        }
        ClientModel client = context.realm().getClientByClientId(token.getIssuedFor());
        return client != null && OIDCAdvancedConfigWrapper.fromClientModel(client).isUseMtlsHokToken();
    }
}
