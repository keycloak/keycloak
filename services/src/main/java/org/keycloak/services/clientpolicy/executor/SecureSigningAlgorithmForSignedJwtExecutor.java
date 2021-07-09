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

import java.util.Optional;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SecureSigningAlgorithmForSignedJwtExecutor implements ClientPolicyExecutorProvider<SecureSigningAlgorithmForSignedJwtExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureSigningAlgorithmForSignedJwtExecutor.class);

    private final KeycloakSession session;
    private Configuration configuration;

    public SecureSigningAlgorithmForSignedJwtExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(SecureSigningAlgorithmForSignedJwtExecutor.Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public String getProviderId() {
        return SecureSigningAlgorithmForSignedJwtExecutorFactory.PROVIDER_ID;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("require-client-assertion")
        protected Boolean requireClientAssertion;

        public Boolean isRequireClientAssertion() {
            return requireClientAssertion;
        }

        public void setRequireClientAssertion(Boolean requireClientAssertion) {
            this.requireClientAssertion = requireClientAssertion;
        }
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case TOKEN_REQUEST:
            case TOKEN_REFRESH:
            case TOKEN_REVOKE:
            case TOKEN_INTROSPECT:
            case LOGOUT_REQUEST:
                boolean isRequireClientAssertion = Optional.ofNullable(configuration.isRequireClientAssertion()).orElse(Boolean.FALSE).booleanValue();
                HttpRequest req = session.getContext().getContextObject(HttpRequest.class);
                String clientAssertion = req.getDecodedFormParameters().getFirst(OAuth2Constants.CLIENT_ASSERTION);
                if (!isRequireClientAssertion && ObjectUtil.isBlank(clientAssertion)) {
                    break;
                }

                JWSInput jws = null;
                try {
                    jws = new JWSInput(clientAssertion);
                } catch (JWSInputException e) {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                verifySecureSigningAlgorithm(jws.getHeader().getAlgorithm().name());
                break;
            default:
                return;
        }
    }

    private void verifySecureSigningAlgorithm(String signatureAlgorithm) throws ClientPolicyException {
        if (FapiConstant.ALLOWED_ALGORITHMS.contains(signatureAlgorithm)) {
            logger.tracev("Passed. signatureAlgorithm = {0}", signatureAlgorithm);
            return;
        }

        logger.tracev("NOT allowed signatureAlgorithm = {0}", signatureAlgorithm);
        throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed signature algorithm.");
    }

}
