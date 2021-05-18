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

import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.TokenRequestContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class PKCEEnforcerExecutor implements ClientPolicyExecutorProvider<PKCEEnforcerExecutor.Configuration> {

    private static final Pattern VALID_CODE_CHALLENGE_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");
    private static final Pattern VALID_CODE_VERIFIER_PATTERN  = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");

    private final KeycloakSession session;
    private Configuration configuration;

    public PKCEEnforcerExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("auto-configure")
        protected Boolean autoConfigure;

        public Boolean isAutoConfigure() {
            return autoConfigure;
        }

        public void setAutoConfigure(Boolean autoConfigure) {
            this.autoConfigure = autoConfigure;
        }
    }

    @Override
    public String getProviderId() {
        return PKCEEnforcerExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
            case UPDATE:
                ClientCRUDContext clientUpdateContext = (ClientCRUDContext)context;
                autoConfigure(clientUpdateContext.getProposedClientRepresentation());
                validate(clientUpdateContext.getProposedClientRepresentation());
                break;
            case AUTHORIZATION_REQUEST:
                AuthorizationRequestContext authorizationRequestContext = (AuthorizationRequestContext)context;
                executeOnAuthorizationRequest(authorizationRequestContext.getparsedResponseType(),
                    authorizationRequestContext.getAuthorizationEndpointRequest(),
                    authorizationRequestContext.getRedirectUri());
                return;
            case TOKEN_REQUEST:
                TokenRequestContext tokenRequestContext = (TokenRequestContext)context;
                executeOnTokenRequest(tokenRequestContext.getParams(), tokenRequestContext.getParseResult());
                return;
            default:
                return;
        }
    }

    private void autoConfigure(ClientRepresentation rep) {
        if (configuration.isAutoConfigure())
            OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setPkceCodeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
    }

    private void validate(ClientRepresentation rep) throws ClientPolicyException {
        String pkceMethod = OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).getPkceCodeChallengeMethod();
        if (pkceMethod != null && pkceMethod.equals(OAuth2Constants.PKCE_METHOD_S256)) return;
        throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: code_challenge_method");
    }

    private void executeOnAuthorizationRequest(
            OIDCResponseType parsedResponseType,
            AuthorizationEndpointRequest request,
            String redirectUri) throws ClientPolicyException {
        ClientModel client = session.getContext().getClient();
        String codeChallenge = request.getCodeChallenge();
        String codeChallengeMethod = request.getCodeChallengeMethod();
        String pkceCodeChallengeMethod = OIDCAdvancedConfigWrapper.fromClientModel(client).getPkceCodeChallengeMethod();

        // check whether code challenge method is specified
        if (codeChallengeMethod == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter: code_challenge_method");
        }

        // check whether acceptable code challenge method is specified
        if (!isAcceptableCodeChallengeMethod(codeChallengeMethod)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter: invalid code_challenge_method");
        }

        // check whether specified code challenge method is configured one in advance
        if (pkceCodeChallengeMethod != null && !codeChallengeMethod.equals(pkceCodeChallengeMethod)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter: code challenge method is not configured one");
        }

        // check whether code challenge is specified
        if (codeChallenge == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter: code_challenge");
        }

        // check whether code challenge is formatted along with the PKCE specification
        if (!isValidPkceCodeChallenge(codeChallenge)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter: code_challenge");
        }

    }

    private boolean isAcceptableCodeChallengeMethod(String method) {
        return OAuth2Constants.PKCE_METHOD_S256.equals(method);
     }

    private boolean isValidPkceCodeChallenge(String codeChallenge) {
        if (codeChallenge.length() < OIDCLoginProtocol.PKCE_CODE_CHALLENGE_MIN_LENGTH) {
            return false;
        }
        if (codeChallenge.length() > OIDCLoginProtocol.PKCE_CODE_CHALLENGE_MAX_LENGTH) {
            return false;
        }
        Matcher m = VALID_CODE_CHALLENGE_PATTERN.matcher(codeChallenge);
        return m.matches();
    }

    private void executeOnTokenRequest(
            MultivaluedMap<String, String> params,
            OAuth2CodeParser.ParseResult parseResult) throws ClientPolicyException {
        String codeVerifier = params.getFirst(OAuth2Constants.CODE_VERIFIER);
        OAuth2Code codeData = parseResult.getCodeData();
        String codeChallenge = codeData.getCodeChallenge();
        String codeChallengeMethod = codeData.getCodeChallengeMethod();

        checkParamsForPkceEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod);
    };

    private void checkParamsForPkceEnforcedClient(String codeVerifier, String codeChallenge, String codeChallengeMethod) throws ClientPolicyException {
        // check whether code verifier is specified
        if (codeVerifier == null) {
            throw new ClientPolicyException(Errors.CODE_VERIFIER_MISSING, "PKCE code verifier not specified");
        }
        verifyCodeVerifier(codeVerifier, codeChallenge, codeChallengeMethod);
    }

    private void verifyCodeVerifier(String codeVerifier, String codeChallenge, String codeChallengeMethod) throws ClientPolicyException {
        // check whether code verifier is formatted along with the PKCE specification

        if (!isValidFormattedCodeVerifier(codeVerifier)) {
            throw new ClientPolicyException(Errors.INVALID_CODE_VERIFIER, "PKCE invalid code verifier");
        }

        String codeVerifierEncoded = codeVerifier;
        try {
            if (codeChallengeMethod != null && codeChallengeMethod.equals(OAuth2Constants.PKCE_METHOD_S256)) {
                codeVerifierEncoded = generateS256CodeChallenge(codeVerifier);
            } else {
                codeVerifierEncoded = codeVerifier;
            }
        } catch (Exception nae) {
            throw new ClientPolicyException(Errors.PKCE_VERIFICATION_FAILED, "PKCE code verification failed, not supported algorithm specified");
        }
        if (!codeChallenge.equals(codeVerifierEncoded)) {
            throw new ClientPolicyException(Errors.PKCE_VERIFICATION_FAILED, "PKCE verification failed");
        }
    }

    private boolean isValidFormattedCodeVerifier(String codeVerifier) {
        if (codeVerifier.length() < OIDCLoginProtocol.PKCE_CODE_VERIFIER_MIN_LENGTH) {
            return false;
        }
        if (codeVerifier.length() > OIDCLoginProtocol.PKCE_CODE_VERIFIER_MAX_LENGTH) {
            return false;
        }
        Matcher m = VALID_CODE_VERIFIER_PATTERN.matcher(codeVerifier);
        return m.matches();
    }

    private String generateS256CodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(codeVerifier.getBytes("ISO_8859_1"));
        byte[] digestBytes = md.digest();
        String codeVerifierEncoded = Base64Url.encode(digestBytes);
        return codeVerifierEncoded;
    }


}
