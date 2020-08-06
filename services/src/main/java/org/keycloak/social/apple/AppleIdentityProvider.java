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

package org.keycloak.social.apple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureSignerContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * @author Emilien Bondu
 */
public class AppleIdentityProvider extends OIDCIdentityProvider implements SocialIdentityProvider<OIDCIdentityProviderConfig> {

    private static final String OAUTH2_PARAMETER_CODE = "code";

    private static final String OAUTH2_PARAMETER_STATE = "state";

    private static final String OAUTH2_PARAMETER_USER = "user";

    private static final String ACCESS_DENIED = "access_denied";

    private static final String AUTH_URL = "https://appleid.apple.com/auth/authorize?response_mode=form_post";

    private static final String TOKEN_URL = "https://appleid.apple.com/auth/token";

    private static final String ISSUER = "https://appleid.apple.com";

    private static final String EMAIL_SCOPE = "email";

    private static final String NAME_SCOPE = "name";

    protected static final Logger logger = Logger.getLogger(AppleIdentityProvider.class);

    public AppleIdentityProvider(KeycloakSession session, AppleIdentityProviderConfig config) {
        super(session, config);

        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setClientAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_POST);
        config.setIssuer(ISSUER);

        if (!isValidSecret(config.getClientSecret())) {
            config.setClientSecret(generateJWS(
                    config.getP8Content(),
                    config.getKeyId(),
                    config.getTeamId())
            );
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new Endpoint(realm, callback, event);
    }

    @Override
    protected String getDefaultScopes() {
        return NAME_SCOPE+ " " + EMAIL_SCOPE;
    }

    protected class Endpoint {
        protected RealmModel realm;

        protected AuthenticationCallback callback;

        protected EventBuilder event;

        @Context
        protected KeycloakSession session;

        @Context
        protected ClientConnection clientConnection;

        @Context
        protected HttpHeaders headers;

        public Endpoint(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
            this.realm = realm;
            this.callback = callback;
            this.event = event;
        }

        @POST
        public Response authResponse(@FormParam(AppleIdentityProvider.OAUTH2_PARAMETER_STATE) String state,
                                     @FormParam(AppleIdentityProvider.OAUTH2_PARAMETER_CODE) String authorizationCode,
                                     @FormParam(AppleIdentityProvider.OAUTH2_PARAMETER_USER) String user,
                                     @FormParam(OAuth2Constants.ERROR) String error) {
            IdentityBrokerState idpState = IdentityBrokerState.encoded(state);
            String clientId = idpState.getClientId();
            String tabId = idpState.getTabId();
            if (clientId == null || tabId == null) {
                logger.errorf("Invalid state parameter: %s", state);
                event.event(EventType.LOGIN);
                event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            if (error != null) {
                logger.error(error + " for broker login " + getConfig().getProviderId());
                if (error.equals(ACCESS_DENIED)) {
                    return callback.cancelled();
                } else if (error.equals(OAuthErrorException.LOGIN_REQUIRED) || error.equals(OAuthErrorException.INTERACTION_REQUIRED)) {
                    return callback.error(error);
                } else {
                    return callback.error(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }
            }

            ClientModel client = realm.getClientByClientId(clientId);
            AuthenticationSessionModel authSession = ClientSessionCode.getClientSession(state, tabId, session, realm, client, event, AuthenticationSessionModel.class);

            try {
                if (authorizationCode != null) {

                    String response = generateTokenRequest(authorizationCode).asString();

                    BrokeredIdentityContext federatedIdentity = getFederatedIdentity(user, response);
                    federatedIdentity.setIdpConfig(getConfig());
                    federatedIdentity.setIdp(AppleIdentityProvider.this);
                    federatedIdentity.setAuthenticationSession(authSession);
                    return callback.authenticated(federatedIdentity);
                }
            } catch (WebApplicationException e) {
                return e.getResponse();
            } catch (Exception e) {
                logger.error("Failed to make identity provider oauth callback", e);
            }
            event.event(EventType.LOGIN);
            event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
            return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
        }

        public BrokeredIdentityContext getFederatedIdentity(String userData, String response) throws JsonProcessingException {
            BrokeredIdentityContext user = AppleIdentityProvider.this.getFederatedIdentity(response);

            if (userData != null) {
                JsonNode profile = mapper.readTree(userData);

                JsonNode email = profile.get("email");
                if(email != null) {
                    user.setEmail(email.asText());
                }

                JsonNode nameNode = profile.get("name");
                if (nameNode != null) {
                    JsonNode firstNameNode = nameNode.get("firstName");
                    if (firstNameNode != null) {
                        user.setFirstName(firstNameNode.asText());
                    }
                    JsonNode lastNameNode = nameNode.get("lastName");
                    if (lastNameNode != null) {
                        user.setLastName(lastNameNode.asText());
                    }
                    if (firstNameNode != null && lastNameNode != null) {
                        user.setUsername(firstNameNode.asText() + " " + lastNameNode.asText());
                    }
                }

                AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
            }

            return user;
        }

        public SimpleHttp generateTokenRequest(String authorizationCode) {
            KeycloakContext context = session.getContext();
            SimpleHttp tokenRequest = SimpleHttp.doPost(getConfig().getTokenUrl(), session)
                    .param(OAUTH2_PARAMETER_CODE, authorizationCode)
                    .param(OAUTH2_PARAMETER_REDIRECT_URI, Urls.identityProviderAuthnResponse(context.getUri().getBaseUri(),
                            getConfig().getAlias(), context.getRealm().getName()).toString())
                    .param(OAUTH2_PARAMETER_GRANT_TYPE, OAUTH2_GRANT_TYPE_AUTHORIZATION_CODE);
            return authenticateTokenRequest(tokenRequest);
        }
    }

    private String generateJWS(String p8Content, String keyId, String teamId) {
        try {
            KeyFactory kf = KeyFactory.getInstance("ECDSA");

            // PemUtil.pemToDer do not seems no works
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(
                    p8Content
                            .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                            .replaceAll("-----END PRIVATE KEY-----", "")
                            .replaceAll("\\n", "")
                            .replaceAll(" ", "")
            ));
            PrivateKey privateKey = kf.generatePrivate(keySpecPKCS8);
            KeyWrapper keyWrapper = new KeyWrapper();
            keyWrapper.setAlgorithm("ES256");
            keyWrapper.setPrivateKey(privateKey);
            keyWrapper.setKid(keyId);

            return new JWSBuilder()
                    .jsonContent(generateClientToken(teamId))
                    .sign(new ServerECDSASignatureSignerContext(keyWrapper));
        } catch (Exception e) {
            logger.error("Unable to generate JWS");
        }
        return null;
    }

    private boolean isValidSecret(String clientSecret) {
        if (clientSecret != null  && clientSecret.length() > 0) {
            try {
                JWSInput jws = new JWSInput(clientSecret);
                JsonWebToken token = jws.readJsonContent(JsonWebToken.class);
                return !token.isExpired();
            } catch (JWSInputException e) {
                logger.debug("Secret is not a valid JWS");
            }
        }
        return false;
    }

    private JsonWebToken generateClientToken(String teamId) {
        JsonWebToken jwt = new JsonWebToken();
        jwt.issuer(teamId);
        jwt.subject(getConfig().getClientId());
        jwt.audience(ISSUER);
        jwt.iat(Long.valueOf(Time.currentTime()));
        jwt.exp(jwt.getIat() + 86400 * 180);
        return jwt;
    }
}
