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

package org.keycloak.services.clientpolicy.executor;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class SecureSigningAlgorithmForSignedJwtEnforceExecutor implements ClientPolicyExecutorProvider {

    private static final Logger logger = Logger.getLogger(SecureSigningAlgorithmForSignedJwtEnforceExecutor.class);
    private static final String LOGMSG_PREFIX = "CLIENT-POLICY";
    private String logMsgPrefix() {
        return LOGMSG_PREFIX + "@" + session.hashCode() + " :: EXECUTOR";
    }

    private final KeycloakSession session;

    public SecureSigningAlgorithmForSignedJwtEnforceExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Object config) {
        // no setup configuration item
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration {
    }

    @Override
    public String getProviderId() {
        return SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case TOKEN_REQUEST:
            case TOKEN_REFRESH:
            case TOKEN_REVOKE:
            case TOKEN_INTROSPECT:
            case LOGOUT_REQUEST:
                HttpRequest req = session.getContext().getContextObject(HttpRequest.class);
                String clientAssertion = req.getDecodedFormParameters().getFirst(OAuth2Constants.CLIENT_ASSERTION);
                JWSInput jws = null;
                try {
                    jws = new JWSInput(clientAssertion);
                } catch (JWSInputException e) {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                String alg = jws.getHeader().getAlgorithm().name();
                verifySecureSigningAlgorithm(alg);
                break;
            default:
                return;
        }
    }

    private void verifySecureSigningAlgorithm(String signatureAlgorithm) throws ClientPolicyException {
        if (signatureAlgorithm == null) {
            ClientPolicyLogger.logv(logger, "{0} :: Signing algorithm not specified explicitly.", logMsgPrefix());
            return;
        }

        // Please change also SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory.getHelpText() if you are changing any algorithms here.
        switch (signatureAlgorithm) {
            case Algorithm.PS256:
            case Algorithm.PS384:
            case Algorithm.PS512:
            case Algorithm.ES256:
            case Algorithm.ES384:
            case Algorithm.ES512:
                ClientPolicyLogger.logv(logger, "{0} :: Passed. signatureAlgorithm = {1}", logMsgPrefix(), signatureAlgorithm);
                return;
        }
        ClientPolicyLogger.logv(logger, "{0} :: NOT allowed signatureAlgorithm = {1}", logMsgPrefix(), signatureAlgorithm);
        throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed signature algorithm.");
    }

}
