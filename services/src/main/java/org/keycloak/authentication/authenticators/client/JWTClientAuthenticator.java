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
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.util.CertificateInfoHelper;

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

    protected static ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    public static final String PROVIDER_ID = "client-jwt";
    public static final String ATTR_PREFIX = "jwt.credential";
    public static final String CERTIFICATE_ATTR = "jwt.credential.certificate";

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        String clientAssertionType = params.getFirst(OAuth2Constants.CLIENT_ASSERTION_TYPE);
        String clientAssertion = params.getFirst(OAuth2Constants.CLIENT_ASSERTION);

        if (clientAssertionType == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Parameter client_assertion_type is missing");
            context.challenge(challengeResponse);
            return;
        }

        if (!clientAssertionType.equals(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT)) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Parameter client_assertion_type has value '"
                    + clientAssertionType + "' but expected is '" + OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT + "'");
            context.challenge(challengeResponse);
            return;
        }

        if (clientAssertion == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "client_assertion parameter missing");
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
            return;
        }

        try {
            JWSInput jws = new JWSInput(clientAssertion);
            JsonWebToken token = jws.readJsonContent(JsonWebToken.class);

            RealmModel realm = context.getRealm();
            String clientId = token.getSubject();
            if (clientId == null) {
                throw new RuntimeException("Can't identify client. Issuer missing on JWT token");
            }

            context.getEvent().client(clientId);
            ClientModel client = realm.getClientByClientId(clientId);
            if (client == null) {
                context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND, null);
                return;
            } else {
                context.setClient(client);
            }

            if (!client.isEnabled()) {
                context.failure(AuthenticationFlowError.CLIENT_DISABLED, null);
                return;
            }

            // Get client key and validate signature
            PublicKey clientPublicKey = getSignatureValidationKey(client, context);
            if (clientPublicKey == null) {
                // Error response already set to context
                return;
            }

            boolean signatureValid;
            try {
                signatureValid = RSAProvider.verify(jws, clientPublicKey);
            } catch (RuntimeException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new RuntimeException("Signature on JWT token failed validation", cause);
            }
            if (!signatureValid) {
                throw new RuntimeException("Signature on JWT token failed validation");
            }

            // Validate other things
            String expectedAudience = Urls.realmIssuer(context.getUriInfo().getBaseUri(), realm.getName());
            if (!token.hasAudience(expectedAudience)) {
                throw new RuntimeException("Token audience doesn't match domain. Realm audience is '" + expectedAudience + "' but audience from token is '" + Arrays.asList(token.getAudience()).toString() + "'");
            }

            if (!token.isActive()) {
                throw new RuntimeException("Token is not active");
            }

            // KEYCLOAK-2986
            if (token.getExpiration() == 0 && token.getIssuedAt() + 10 < Time.currentTime()) {
                throw new RuntimeException("Token is not active");
            }

            context.success();
        } catch (Exception e) {
            logger.errorValidatingAssertion(e);
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Client authentication with signed JWT failed: " + e.getMessage());
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
        }
    }

    protected PublicKey getSignatureValidationKey(ClientModel client, ClientAuthenticationFlowContext context) {
        CertificateRepresentation certInfo = CertificateInfoHelper.getCertificateFromClient(client, ATTR_PREFIX);

        String encodedCertificate = certInfo.getCertificate();
        String encodedPublicKey = certInfo.getPublicKey();

        if (encodedCertificate == null && encodedPublicKey == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Client '" + client.getClientId() + "' doesn't have certificate or publicKey configured");
            context.failure(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED, challengeResponse);
            return null;
        }

        if (encodedCertificate != null && encodedPublicKey != null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Client '" + client.getClientId() + "' has both publicKey and certificate configured");
            context.failure(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED, challengeResponse);
            return null;
        }

        // TODO: Caching of publicKeys / certificates, so it doesn't need to be always computed from pem. For performance reasons...
        if (encodedCertificate != null) {
            X509Certificate clientCert = KeycloakModelUtils.getCertificate(encodedCertificate);
            return clientCert.getPublicKey();
        } else {
            return KeycloakModelUtils.getPublicKey(encodedPublicKey);
        }
    }

    @Override
    public String getDisplayType() {
        return "Signed Jwt";
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
