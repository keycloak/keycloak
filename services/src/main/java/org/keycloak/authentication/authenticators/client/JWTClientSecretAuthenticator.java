/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.crypto.ClientSignatureVerifierProvider;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCClientSecretConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ServicesLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.TokenManager.DEFAULT_VALIDATOR;

/**
 * Client authentication based on JWT signed by client secret instead of private key .
 * See <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">specs</a> for more details.
 * <p>
 * This is server side, which verifies JWT from client_assertion parameter, where the assertion was created on adapter side by
 * org.keycloak.adapters.authentication.JWTClientSecretCredentialsProvider
 * <p>
 */
public class JWTClientSecretAuthenticator extends AbstractClientAuthenticator {

    public static final String PROVIDER_ID = "client-secret-jwt";

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        try {
            JWTClientValidator validator = new JWTClientValidator(context, this::verifySignature, getId());
            if (!validator.validate()) return;

            context.success();
        } catch (Exception e) {
            ServicesLogger.LOGGER.errorValidatingAssertion(e);
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Client authentication with client secret signed JWT failed: " + e.getMessage());
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
        }
    }

    public boolean verifySignature(AbstractJWTClientValidator validator) {
        ClientAuthenticationFlowContext context = validator.getContext();
        ClientModel client = validator.getClient();

        String clientSecretString = client.getSecret();
        if (clientSecretString == null) {
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, null);
            return false;
        }

        //
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientModel(client);
        if (wrapper.isClientSecretExpired()) {
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, null);
            return false;
        }
        //

        boolean signatureValid;
        try {
            JsonWebToken jwt = context.getSession().tokens().decodeClientJWT(validator.getClientAssertion(), client, (jose, validatedClient) -> {
                DEFAULT_VALIDATOR.accept(jose, validatedClient);
                String signatureAlgorithm = jose.getHeader().getRawAlgorithm();
                ClientSignatureVerifierProvider signatureProvider = context.getSession().getProvider(ClientSignatureVerifierProvider.class, signatureAlgorithm);
                if (signatureProvider == null) {
                    throw new RuntimeException("Algorithm not supported");
                }
                if (signatureProvider.isAsymmetricAlgorithm()) {
                    throw new RuntimeException("Algorithm is not symmetric");
                }
            }, JsonWebToken.class);
            signatureValid = jwt != null;
            //try authenticate with client rotated secret
            if (!signatureValid && wrapper.hasRotatedSecret() && !wrapper.isClientRotatedSecretExpired()) {
                jwt = context.getSession().tokens().decodeClientJWT(validator.getClientAssertion(), wrapper.toRotatedClientModel(), JsonWebToken.class);
                signatureValid = jwt != null;
            }
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("Signature on JWT token by client secret failed validation", cause);
        }
        if (!signatureValid) {
            throw new RuntimeException("Signature on JWT token by client secret  failed validation");
        }
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        // This impl doesn't use generic screen in admin console, but has its own screen. So no need to return anything here
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
        // e.g. client adapter's keycloak.json
        // "credentials": {
        //   "secret-jwt": {
        //     "secret": "234234-234234-234234",
        //     "algorithm": "HS256"
        //   }
        // }
        Map<String, Object> props = new HashMap<>();
        props.put("secret", client.getSecret());
        String algorithm = client.getAttribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG);
        if (algorithm != null) {
            props.put("algorithm", algorithm);
        }

        Map<String, Object> config = new HashMap<>();
        config.put("secret-jwt", props);
        return config;
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        if (loginProtocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            Set<String> results = new HashSet<>();
            results.add(OIDCLoginProtocol.CLIENT_SECRET_JWT);
            return results;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean supportsSecret() {
        return true;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Signed Jwt with Client Secret";
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Validates client based on signed JWT issued by client and signed with the Client Secret";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList<>();
    }

}
