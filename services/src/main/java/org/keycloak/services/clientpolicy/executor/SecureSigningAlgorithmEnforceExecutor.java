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
import java.util.List;

import org.jboss.logging.Logger;

import org.keycloak.OAuthErrorException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class SecureSigningAlgorithmEnforceExecutor implements ClientPolicyExecutorProvider {

    private static final Logger logger = Logger.getLogger(SecureSigningAlgorithmEnforceExecutor.class);
    private static final String LOGMSG_PREFIX = "CLIENT-POLICY";
    private String logMsgPrefix() {
        return LOGMSG_PREFIX + "@" + session.hashCode() + " :: EXECUTOR";
    }

    private final KeycloakSession session;

    private static final List<String> sigTargets = Arrays.asList(
            OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG,
            OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG,
            OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG,
            OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG);

    private static final List<String> sigTargetsAdminRestApiOnly = Arrays.asList(
            OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG);

    public SecureSigningAlgorithmEnforceExecutor(KeycloakSession session) {
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
        return SecureSigningAlgorithmEnforceExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
            if (context instanceof AdminClientRegisterContext) {
                verifySecureSigningAlgorithm(((AdminClientRegisterContext)context).getProposedClientRepresentation(), true, false);
            } else if (context instanceof DynamicClientRegisterContext) {
                verifySecureSigningAlgorithm(((DynamicClientRegisterContext)context).getProposedClientRepresentation(), false, false);
            } else {
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
            }
            break;
        case UPDATE:
            if (context instanceof AdminClientUpdateContext) {
                verifySecureSigningAlgorithm(((AdminClientUpdateContext)context).getProposedClientRepresentation(), true, true);
            } else if (context instanceof DynamicClientUpdateContext) {
                verifySecureSigningAlgorithm(((DynamicClientUpdateContext)context).getProposedClientRepresentation(), false, true);
            } else {
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
            }
            break;
        default:
            return;
        }
    }

    private void verifySecureSigningAlgorithm(ClientRepresentation clientRep, boolean byAdminRestApi, boolean isUpdate) throws ClientPolicyException {
        if (clientRep.getAttributes() == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "no signature algorithm was specified.");
        }

        for (String sigTarget : sigTargets) {
            verifySecureSigningAlgorithm(sigTarget, clientRep.getAttributes().get(sigTarget));
        }

        // no client metadata found in RFC 7591 OAuth Dynamic Client Registration Metadata
        if (byAdminRestApi) {
            for (String sigTarget : sigTargetsAdminRestApiOnly) {
                verifySecureSigningAlgorithm(sigTarget, clientRep.getAttributes().get(sigTarget));
            }
        }
    }

    private void verifySecureSigningAlgorithm(String sigTarget, String sigAlg) throws ClientPolicyException {
        if (sigAlg == null) {
            ClientPolicyLogger.logv(logger, "{0} :: Signing algorithm not specified explicitly. signature target = {1}", logMsgPrefix(), sigTarget);
            return;
        }
        switch (sigAlg) {
        case Algorithm.PS256:
        case Algorithm.PS384:
        case Algorithm.PS512:
        case Algorithm.ES256:
        case Algorithm.ES384:
        case Algorithm.ES512:
            ClientPolicyLogger.logv(logger, "{0} :: Passed. signature target = {1}, signature algorithm = {2}", logMsgPrefix(),sigTarget, sigAlg);
            return;
        }
        ClientPolicyLogger.logv(logger, "{0} :: NOT allowed signatureAlgorithm. signature target = {1}, signature algorithm = {2}", logMsgPrefix(), sigTarget, sigAlg);
        throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed signature algorithm.");
    }

}
