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
package org.keycloak.social.twitter;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.vault.VaultStringSecret;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TwitterIdentityProvider extends AbstractIdentityProvider<OAuth2IdentityProviderConfig> implements
        SocialIdentityProvider<OAuth2IdentityProviderConfig>, ExchangeTokenToIdentityProviderToken {

    String TWITTER_TOKEN_TYPE="twitter";


    protected static final Logger logger = Logger.getLogger(TwitterIdentityProvider.class);

    private static final String TWITTER_TOKEN = "twitter_token";
    private static final String TWITTER_TOKENSECRET = "twitter_tokenSecret";

    public TwitterIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new Endpoint(realm, callback, event);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret()));

            URI uri = new URI(request.getRedirectUri() + "?state=" + request.getState().getEncoded());

            RequestToken requestToken = twitter.getOAuthRequestToken(uri.toString());
            AuthenticationSessionModel authSession = request.getAuthenticationSession();

            authSession.setAuthNote(TWITTER_TOKEN, requestToken.getToken());
            authSession.setAuthNote(TWITTER_TOKENSECRET, requestToken.getTokenSecret());

            URI authenticationUrl = URI.create(requestToken.getAuthenticationURL());

            return Response.seeOther(authenticationUrl).build();
        } catch (Exception e) {
            throw new IdentityBrokerException("Could send authentication request to twitter.", e);
        }
    }

    @Override
    public Response exchangeFromToken(UriInfo uriInfo, EventBuilder builder, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject, MultivaluedMap<String, String> params) {
        String requestedType = params.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        if (requestedType != null && !requestedType.equals(TWITTER_TOKEN_TYPE)) {
            return exchangeUnsupportedRequiredType();
        }
        if (!getConfig().isStoreToken()) {
            String brokerId = tokenUserSession.getNote(Details.IDENTITY_PROVIDER);
            if (brokerId == null || !brokerId.equals(getConfig().getAlias())) {
                return exchangeNotLinkedNoStore(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
            }
            return exchangeSessionToken(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        } else {
            return exchangeStoredToken(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
    }

    protected Response exchangeStoredToken(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        FederatedIdentityModel model = session.users().getFederatedIdentity(authorizedClient.getRealm(), tokenSubject, getConfig().getAlias());
        if (model == null || model.getToken() == null) {
            return exchangeNotLinked(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
        String accessToken = model.getToken();
        if (accessToken == null) {
            model.setToken(null);
            session.users().updateFederatedIdentity(authorizedClient.getRealm(), tokenSubject, model);
            return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setToken(accessToken);
        tokenResponse.setIdToken(null);
        tokenResponse.setRefreshToken(null);
        tokenResponse.setRefreshExpiresIn(0);
        tokenResponse.getOtherClaims().clear();
        tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, TWITTER_TOKEN_TYPE);
        tokenResponse.getOtherClaims().put(ACCOUNT_LINK_URL, getLinkingUrl(uriInfo, authorizedClient, tokenUserSession));
        return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    protected Response exchangeSessionToken(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        String accessToken = tokenUserSession.getNote(IdentityProvider.FEDERATED_ACCESS_TOKEN);
        if (accessToken == null) {
            return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setToken(accessToken);
        tokenResponse.setIdToken(null);
        tokenResponse.setRefreshToken(null);
        tokenResponse.setRefreshExpiresIn(0);
        tokenResponse.getOtherClaims().clear();
        tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, TWITTER_TOKEN_TYPE);
        tokenResponse.getOtherClaims().put(ACCOUNT_LINK_URL, getLinkingUrl(uriInfo, authorizedClient, tokenUserSession));
        return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
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

        @GET
        public Response authResponse(@QueryParam("state") String state,
                                     @QueryParam("denied") String denied,
                                     @QueryParam("oauth_verifier") String verifier) {
            IdentityBrokerState idpState = IdentityBrokerState.encoded(state);
            String clientId = idpState.getClientId();
            String tabId = idpState.getTabId();
            if (clientId == null || tabId == null) {
                logger.errorf("Invalid state parameter: %s", state);
                sendErrorEvent();
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            ClientModel client = realm.getClientByClientId(clientId);
            AuthenticationSessionModel authSession = ClientSessionCode.getClientSession(state, tabId, session, realm, client, event, AuthenticationSessionModel.class);

            if (denied != null) {
                return callback.cancelled();
            }

            try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
                Twitter twitter = new TwitterFactory(new ConfigurationBuilder().setIncludeEmailEnabled(true).build()).getInstance();
                twitter.setOAuthConsumer(getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret()));

                String twitterToken = authSession.getAuthNote(TWITTER_TOKEN);
                String twitterSecret = authSession.getAuthNote(TWITTER_TOKENSECRET);

                RequestToken requestToken = new RequestToken(twitterToken, twitterSecret);

                AccessToken oAuthAccessToken = twitter.getOAuthAccessToken(requestToken, verifier);
                twitter4j.User twitterUser = twitter.verifyCredentials();

                BrokeredIdentityContext identity = new BrokeredIdentityContext(Long.toString(twitterUser.getId()));
                identity.setIdp(TwitterIdentityProvider.this);

                identity.setUsername(twitterUser.getScreenName());
                identity.setEmail(twitterUser.getEmail());
                identity.setName(twitterUser.getName());


                StringBuilder tokenBuilder = new StringBuilder();

                tokenBuilder.append("{");
                tokenBuilder.append("\"oauth_token\":").append("\"").append(oAuthAccessToken.getToken()).append("\"").append(",");
                tokenBuilder.append("\"oauth_token_secret\":").append("\"").append(oAuthAccessToken.getTokenSecret()).append("\"").append(",");
                tokenBuilder.append("\"screen_name\":").append("\"").append(oAuthAccessToken.getScreenName()).append("\"").append(",");
                tokenBuilder.append("\"user_id\":").append("\"").append(oAuthAccessToken.getUserId()).append("\"");
                tokenBuilder.append("}");
                String token = tokenBuilder.toString();
                if (getConfig().isStoreToken()) {
                    identity.setToken(token);
                }
                identity.getContextData().put(IdentityProvider.FEDERATED_ACCESS_TOKEN, token);

                identity.setIdpConfig(getConfig());
                identity.setAuthenticationSession(authSession);

                return callback.authenticated(identity);
            } catch (WebApplicationException e) {
                sendErrorEvent();
                return e.getResponse();
            } catch (Exception e) {
                logger.error("Couldn't get user profile from twitter.", e);
                sendErrorEvent();
                return ErrorPage.error(session, authSession, Response.Status.BAD_GATEWAY, Messages.UNEXPECTED_ERROR_HANDLING_RESPONSE);
            }
        }

        private void sendErrorEvent() {
            event.event(EventType.LOGIN);
            event.error("twitter_login_failed");
        }

    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        return Response.ok(identity.getToken()).type(MediaType.APPLICATION_JSON).build();
    }

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
        authSession.setUserSessionNote(IdentityProvider.FEDERATED_ACCESS_TOKEN, (String)context.getContextData().get(IdentityProvider.FEDERATED_ACCESS_TOKEN));

    }

}
