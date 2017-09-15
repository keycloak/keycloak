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
package org.keycloak.broker.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Pedro Igor
 */
public abstract class AbstractOAuth2IdentityProvider<C extends OAuth2IdentityProviderConfig> extends AbstractIdentityProvider<C> implements ExchangeTokenToIdentityProviderToken {
    protected static final Logger logger = Logger.getLogger(AbstractOAuth2IdentityProvider.class);

    public static final String OAUTH2_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH2_GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String FEDERATED_ACCESS_TOKEN = "FEDERATED_ACCESS_TOKEN";
    public static final String FEDERATED_REFRESH_TOKEN = "FEDERATED_REFRESH_TOKEN";
    public static final String FEDERATED_TOKEN_EXPIRATION = "FEDERATED_TOKEN_EXPIRATION";
    public static final String ACCESS_DENIED = "access_denied";
    protected static ObjectMapper mapper = new ObjectMapper();

    public static final String OAUTH2_PARAMETER_ACCESS_TOKEN = "access_token";
    public static final String OAUTH2_PARAMETER_SCOPE = "scope";
    public static final String OAUTH2_PARAMETER_STATE = "state";
    public static final String OAUTH2_PARAMETER_RESPONSE_TYPE = "response_type";
    public static final String OAUTH2_PARAMETER_REDIRECT_URI = "redirect_uri";
    public static final String OAUTH2_PARAMETER_CODE = "code";
    public static final String OAUTH2_PARAMETER_CLIENT_ID = "client_id";
    public static final String OAUTH2_PARAMETER_CLIENT_SECRET = "client_secret";
    public static final String OAUTH2_PARAMETER_GRANT_TYPE = "grant_type";


    public AbstractOAuth2IdentityProvider(KeycloakSession session, C config) {
        super(session, config);

        if (config.getDefaultScope() == null || config.getDefaultScope().isEmpty()) {
            config.setDefaultScope(getDefaultScopes());
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new Endpoint(callback, realm, event);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        try {
            URI authorizationUrl = createAuthorizationUrl(request).build();

            return Response.seeOther(authorizationUrl).build();
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not create authentication request.", e);
        }
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        return Response.ok(identity.getToken()).build();
    }

    @Override
    public C getConfig() {
        return super.getConfig();
    }

    protected String extractTokenFromResponse(String response, String tokenName) {
    	  if(response == null)
    	  	return null;
    	  
        if (response.startsWith("{")) {
            try {
            		JsonNode node = mapper.readTree(response);
            		if(node.has(tokenName)){
            			String s = node.get(tokenName).textValue();
            			if(s == null || s.trim().isEmpty())
            				return null;
                  return s;
            		} else {
            			return null;
            		}
            } catch (IOException e) {
                throw new IdentityBrokerException("Could not extract token [" + tokenName + "] from response [" + response + "] due: " + e.getMessage(), e);
            }
        } else {
            Matcher matcher = Pattern.compile(tokenName + "=([^&]+)").matcher(response);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    @Override
    public Response exchangeFromToken(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject, AccessToken token, MultivaluedMap<String, String> params) {
        String requestedType = params.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        if (requestedType != null && !requestedType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
            return exchangeUnsupportedRequiredType();
        }
        if (!getConfig().isStoreToken()) {
            String brokerId = tokenUserSession.getNote(Details.IDENTITY_PROVIDER);
            if (brokerId == null || !brokerId.equals(getConfig().getAlias())) {
                return exchangeNotLinkedNoStore(uriInfo, authorizedClient, tokenUserSession, tokenSubject, token);
            }
            return exchangeSessionToken(uriInfo, authorizedClient, tokenUserSession, tokenSubject, token);
        } else {
            return exchangeStoredToken(uriInfo, authorizedClient, tokenUserSession, tokenSubject, token);
        }
    }

    protected Response exchangeStoredToken(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject, AccessToken token) {
        FederatedIdentityModel model = session.users().getFederatedIdentity(tokenSubject, getConfig().getAlias(), authorizedClient.getRealm());
        if (model == null || model.getToken() == null) {
            return exchangeNotLinked(uriInfo, authorizedClient, tokenUserSession, tokenSubject, token);
        }
        String accessToken = extractTokenFromResponse(model.getToken(), getAccessTokenResponseParameter());
        if (accessToken == null) {
            model.setToken(null);
            session.users().updateFederatedIdentity(authorizedClient.getRealm(), tokenSubject, model);
            return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject, token);
        }
        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setToken(accessToken);
        tokenResponse.setIdToken(null);
        tokenResponse.setRefreshToken(null);
        tokenResponse.setRefreshExpiresIn(0);
        tokenResponse.getOtherClaims().clear();
        tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);
        tokenResponse.getOtherClaims().put(ACCOUNT_LINK_URL, getLinkingUrl(uriInfo, authorizedClient, tokenUserSession, token));
        return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    protected Response exchangeSessionToken(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject, AccessToken token) {
        String accessToken = tokenUserSession.getNote(FEDERATED_ACCESS_TOKEN);
        if (accessToken == null) {
            return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject, token);
        }
        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setToken(accessToken);
        tokenResponse.setIdToken(null);
        tokenResponse.setRefreshToken(null);
        tokenResponse.setRefreshExpiresIn(0);
        tokenResponse.getOtherClaims().clear();
        tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);
        tokenResponse.getOtherClaims().put(ACCOUNT_LINK_URL, getLinkingUrl(uriInfo, authorizedClient, tokenUserSession, token));
        return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
    }


    public BrokeredIdentityContext getFederatedIdentity(String response) {
        String accessToken = extractTokenFromResponse(response, getAccessTokenResponseParameter());

        if (accessToken == null) {
            throw new IdentityBrokerException("No access token available in OAuth server response: " + response);
        }

        BrokeredIdentityContext context = doGetFederatedIdentity(accessToken);
        context.getContextData().put(FEDERATED_ACCESS_TOKEN, accessToken);
        return context;
    }

    protected String getAccessTokenResponseParameter() {
        return OAUTH2_PARAMETER_ACCESS_TOKEN;
    }


    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        return null;
    }


    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        final UriBuilder uriBuilder = UriBuilder.fromUri(getConfig().getAuthorizationUrl())
                .queryParam(OAUTH2_PARAMETER_SCOPE, getConfig().getDefaultScope())
                .queryParam(OAUTH2_PARAMETER_STATE, request.getState().getEncodedState())
                .queryParam(OAUTH2_PARAMETER_RESPONSE_TYPE, "code")
                .queryParam(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                .queryParam(OAUTH2_PARAMETER_REDIRECT_URI, request.getRedirectUri());

        String loginHint = request.getAuthenticationSession().getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        if (getConfig().isLoginHint() && loginHint != null) {
            uriBuilder.queryParam(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        }
        return uriBuilder;
    }

    /**
     * Get JSON property as text. JSON numbers and booleans are converted to text. Empty string is converted to null. 
     * 
     * @param jsonNode to get property from
     * @param name of property to get
     * @return string value of the property or null.
     */
    public String getJsonProperty(JsonNode jsonNode, String name) {
        if (jsonNode.has(name) && !jsonNode.get(name).isNull()) {
        	  String s = jsonNode.get(name).asText();
        	  if(s != null && !s.isEmpty())
        	  		return s;
        	  else
      	  			return null;
        }

        return null;
    }

    public JsonNode asJsonNode(String json) throws IOException {
        return mapper.readTree(json);
    }

    protected abstract String getDefaultScopes();

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
        String token = (String) context.getContextData().get(FEDERATED_ACCESS_TOKEN);
        if (token != null) authSession.setUserSessionNote(FEDERATED_ACCESS_TOKEN, token);
    }

    protected class Endpoint {
        protected AuthenticationCallback callback;
        protected RealmModel realm;
        protected EventBuilder event;

        @Context
        protected KeycloakSession session;

        @Context
        protected ClientConnection clientConnection;

        @Context
        protected HttpHeaders headers;

        @Context
        protected UriInfo uriInfo;

        public Endpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event) {
            this.callback = callback;
            this.realm = realm;
            this.event = event;
        }

        @GET
        public Response authResponse(@QueryParam(AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_STATE) String state,
                                     @QueryParam(AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_CODE) String authorizationCode,
                                     @QueryParam(OAuth2Constants.ERROR) String error) {
            if (error != null) {
                //logger.error("Failed " + getConfig().getAlias() + " broker login: " + error);
                if (error.equals(ACCESS_DENIED)) {
                    logger.error(ACCESS_DENIED + " for broker login " + getConfig().getProviderId());
                    return callback.cancelled(state);
                } else {
                    logger.error(error + " for broker login " + getConfig().getProviderId());
                    return callback.error(state, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }
            }

            try {

                if (authorizationCode != null) {
                    String response = generateTokenRequest(authorizationCode).asString();

                    BrokeredIdentityContext federatedIdentity = getFederatedIdentity(response);

                    if (getConfig().isStoreToken()) {
                        // make sure that token wasn't already set by getFederatedIdentity();
                        // want to be able to allow provider to set the token itself.
                        if (federatedIdentity.getToken() == null)federatedIdentity.setToken(response);
                    }

                    federatedIdentity.setIdpConfig(getConfig());
                    federatedIdentity.setIdp(AbstractOAuth2IdentityProvider.this);
                    federatedIdentity.setCode(state);

                    return callback.authenticated(federatedIdentity);
                }
            } catch (WebApplicationException e) {
                return e.getResponse();
            } catch (Exception e) {
                logger.error("Failed to make identity provider oauth callback", e);
            }
            event.event(EventType.LOGIN);
            event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
            return ErrorPage.error(session, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
        }

        public SimpleHttp generateTokenRequest(String authorizationCode) {
            return SimpleHttp.doPost(getConfig().getTokenUrl(), session)
                    .param(OAUTH2_PARAMETER_CODE, authorizationCode)
                    .param(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                    .param(OAUTH2_PARAMETER_CLIENT_SECRET, getConfig().getClientSecret())
                    .param(OAUTH2_PARAMETER_REDIRECT_URI, uriInfo.getAbsolutePath().toString())
                    .param(OAUTH2_PARAMETER_GRANT_TYPE, OAUTH2_GRANT_TYPE_AUTHORIZATION_CODE);
        }
    }
}
