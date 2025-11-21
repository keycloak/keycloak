/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.client;


import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.crypto.ClientSignatureVerifierProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ServicesLogger;

import static org.keycloak.models.TokenManager.DEFAULT_VALIDATOR;

/**
 * Client authentication based on JWT signed by client private key .
 * See <a href="https://tools.ietf.org/html/rfc7519">specs</a> for more details.
 *
 * This is server side, which verifies JWT from client_assertion parameter, where the assertion was created on adapter side by
 * org.keycloak.adapters.authentication.JWTClientCredentialsProvider
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWTClientAuthenticator extends AbstractClientAuthenticator {

    public static final String PROVIDER_ID = "client-jwt";
    public static final String ATTR_PREFIX = "jwt.credential";
    public static final String CERTIFICATE_ATTR = "jwt.credential.certificate";


    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        try {
            JWTClientValidator validator = new JWTClientValidator(context, this::verifySignature, getId());
            if (!validator.validate()) return;

            context.success();
        } catch (Exception e) {
            ServicesLogger.LOGGER.errorValidatingAssertion(e);
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), OAuthErrorException.INVALID_CLIENT, "Client authentication with signed JWT failed: " + e.getMessage());
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
        }
    }

    public boolean verifySignature(AbstractJWTClientValidator validator) {
        ClientAuthenticationFlowContext context = validator.getContext();
        ClientModel client = validator.getClient();

        // Get client key and validate signature
        PublicKey clientPublicKey = getSignatureValidationKey(client, context, validator.getJws());
        if (clientPublicKey == null) {
            // Error response already set to context
            return false;
        }

        boolean signatureValid;
        try {
            JsonWebToken jwt = context.getSession().tokens().decodeClientJWT(validator.getClientAssertion(), client, (jose, validatedClient) -> {
                DEFAULT_VALIDATOR.accept(jose, validatedClient);
                String signatureAlgorithm = jose.getHeader().getRawAlgorithm();
                ClientSignatureVerifierProvider signatureProvider = context.getSession().getProvider(ClientSignatureVerifierProvider.class, signatureAlgorithm);
                if (signatureProvider == null) {
                    throw new RuntimeException("Algorithm not supported");
                }
                if (!signatureProvider.isAsymmetricAlgorithm()) {
                    throw new RuntimeException("Algorithm is not asymmetric");
                }
            }, JsonWebToken.class);
            signatureValid = jwt != null;
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("Signature on JWT token failed validation", cause);
        }
        if (!signatureValid) {
            throw new RuntimeException("Signature on JWT token failed validation");
        }
        return true;
    }

    protected PublicKey getSignatureValidationKey(ClientModel client, ClientAuthenticationFlowContext context, JWSInput jws) {
        PublicKey publicKey = PublicKeyStorageManager.getClientPublicKey(context.getSession(), client, jws);
        if (publicKey == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), OAuthErrorException.INVALID_CLIENT, "Unable to load public key");
            context.failure(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED, challengeResponse);
            return null;
        } else {
            return publicKey;
        }
    }

    @Override
    public String getDisplayType() {
        return "Signed JWT";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Validates client based on signed JWT issued by client and signed with the Client private key";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList<>();
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        // This impl doesn't use generic screen in admin console, but has its own screen. So no need to return anything here
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
        Map<String, Object> props = new HashMap<>();
        props.put("client-keystore-file", "REPLACE WITH THE LOCATION OF YOUR KEYSTORE FILE");
        props.put("client-keystore-type", "jks");
        props.put("client-keystore-password", "REPLACE WITH THE KEYSTORE PASSWORD");
        props.put("client-key-password", "REPLACE WITH THE KEY PASSWORD IN KEYSTORE");
        props.put("client-key-alias", client.getClientId());
        props.put("token-timeout", 10);
        String algorithm = client.getAttribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG);
        if (algorithm != null) {
            props.put("algorithm", algorithm);
        }

        Map<String, Object> config = new HashMap<>();
        config.put("jwt", props);
        return config;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        if (loginProtocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            Set<String> results = new HashSet<>();
            results.add(OIDCLoginProtocol.PRIVATE_KEY_JWT);
            return results;
        } else {
            return Collections.emptySet();
        }
    }
}
