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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import org.keycloak.OAuthErrorException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureSigningAlgorithmExecutor implements ClientPolicyExecutorProvider<SecureSigningAlgorithmExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureSigningAlgorithmExecutor.class);

    private final KeycloakSession session;
    private Configuration configuration;

    private static final List<String> sigTargets = Arrays.asList(
            OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG,
            OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG,
            OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG,
            OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG);

    private static final List<String> sigTargetsAdminRestApiOnly = Arrays.asList(
            OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG);

    private static final String DEFAULT_ALGORITHM_VALUE = Algorithm.PS256;

    public SecureSigningAlgorithmExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return SecureSigningAlgorithmExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void setupConfiguration(SecureSigningAlgorithmExecutor.Configuration config) {
        this.configuration = Optional.ofNullable(config).orElse(createDefaultConfiguration());
        if (config.getDefaultAlgorithm() == null || !isSecureAlgorithm(config.getDefaultAlgorithm())) config.setDefaultAlgorithm(DEFAULT_ALGORITHM_VALUE);
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("default-algorithm")
        protected String defaultAlgorithm;

        public String getDefaultAlgorithm() {
            return defaultAlgorithm;
        }

        public void setDefaultAlgorithm(String defaultAlgorithm) {
            if (isSecureAlgorithm(defaultAlgorithm)) {
                this.defaultAlgorithm = defaultAlgorithm;
            } else {
                logger.tracev("defaultAlgorithm = {0}, fall back to {1}.", defaultAlgorithm, DEFAULT_ALGORITHM_VALUE);
                this.defaultAlgorithm = DEFAULT_ALGORITHM_VALUE;
            }
        }
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
            if (context instanceof AdminClientRegisterContext) {
                verifyAndEnforceSecureSigningAlgorithm(((AdminClientRegisterContext)context).getProposedClientRepresentation(), true, false);
            } else if (context instanceof DynamicClientRegisterContext) {
                verifyAndEnforceSecureSigningAlgorithm(((DynamicClientRegisterContext)context).getProposedClientRepresentation(), false, false);
            } else {
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
            }
            break;
        case UPDATE:
            if (context instanceof AdminClientUpdateContext) {
                verifyAndEnforceSecureSigningAlgorithm(((AdminClientUpdateContext)context).getProposedClientRepresentation(), true, true);
            } else if (context instanceof DynamicClientUpdateContext) {
                verifyAndEnforceSecureSigningAlgorithm(((DynamicClientUpdateContext)context).getProposedClientRepresentation(), false, true);
            } else {
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
            }
            break;
        default:
            return;
        }
    }

    private Configuration createDefaultConfiguration() {
        Configuration conf = new Configuration();
        conf.setDefaultAlgorithm(DEFAULT_ALGORITHM_VALUE);
        return conf;
    }

    private void verifyAndEnforceSecureSigningAlgorithm(ClientRepresentation clientRep, boolean byAdminRestApi, boolean isUpdate) throws ClientPolicyException {
        for (String sigTarget : sigTargets) {
            verifyAndEnforceSecureSigningAlgorithm(sigTarget, clientRep);
        }

        // no client metadata found in RFC 7591 OAuth Dynamic Client Registration Metadata
        if (byAdminRestApi) {
            for (String sigTarget : sigTargetsAdminRestApiOnly) {
                verifyAndEnforceSecureSigningAlgorithm(sigTarget, clientRep);
            }
        }
    }

    private void verifyAndEnforceSecureSigningAlgorithm(String sigTarget, ClientRepresentation clientRep) throws ClientPolicyException {
        Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
        String sigAlg = attributes.get(sigTarget);
        if (sigAlg == null) {
            logger.tracev("Signing algorithm not specified explicitly, signature target = {0}. set default algorithm = {1}.", sigTarget, configuration.getDefaultAlgorithm());
            attributes.put(sigTarget, configuration.getDefaultAlgorithm());
            clientRep.setAttributes(attributes);
            return;
        }

        if (isSecureAlgorithm(sigAlg)) {
            logger.tracev("Passed. signature target = {0}, signature algorithm = {1}", sigTarget, sigAlg);
            return;
        }

        logger.tracev("NOT allowed signatureAlgorithm. signature target = {0}, signature algorithm = {1}", sigTarget, sigAlg);
        throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed signature algorithm.");
    }

    private static boolean isSecureAlgorithm(String sigAlg) {
        return FapiConstant.ALLOWED_ALGORITHMS.contains(sigAlg);
    }

}
