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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
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
import org.keycloak.util.Booleans;
import org.keycloak.vault.VaultStringSecret;

import org.jboss.logging.Logger;
import twitter4j.AccessToken;
import twitter4j.OAuthAuthorization;
import twitter4j.RequestToken;
import twitter4j.Twitter;
import twitter4j.v1.User;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TwitterIdentityProvider extends AbstractIdentityProvider<OAuth2IdentityProviderConfig> implements
        SocialIdentityProvider<OAuth2IdentityProviderConfig>, ExchangeTokenToIdentityProviderToken {

    String TWITTER_TOKEN_TYPE="twitter";

    protected static final Logger logger = Logger.getLogger(TwitterIdentityProvider.class);

    private static final String TWITTER_TOKEN = "twitter_token";

    private final OAuthAuthorization oAuthAuthorization;

    public TwitterIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);
        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            oAuthAuthorization = OAuthAuthorization.newBuilder()
                    .oAuthConsumer(getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret()))
                    .build();
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new Endpoint(session, callback, event, this);
    }

    private static String base64EncodeRequestToken(RequestToken requestToken) throws IOException {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
              ObjectOutputStream oos = new ObjectOutputStream(Base64.getEncoder().wrap(baos))) {
          oos.writeObject(requestToken);
          oos.close();
          return baos.toString(StandardCharsets.US_ASCII);
      }
    }

    protected static RequestToken base64DecodeRequestToken(String serialized) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(serialized)))) {
            return (RequestToken) in.readObject();
        }
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        try {
            URI uri = new URI(request.getRedirectUri() + "?state=" + request.getState().getEncoded());
            RequestToken requestToken = oAuthAuthorization.getOAuthRequestToken(uri.toString());
            AuthenticationSessionModel authSession = request.getAuthenticationSession();

            authSession.setAuthNote(TWITTER_TOKEN, base64EncodeRequestToken(requestToken));

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
        if (Booleans.isFalse(getConfig().isStoreToken())) {
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
        String accessToken = tokenUserSession.getNote(UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN);
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


    protected static class Endpoint {
        protected final RealmModel realm;
        protected final AuthenticationCallback callback;
        protected final EventBuilder event;
        private final TwitterIdentityProvider provider;

        protected final KeycloakSession session;

        protected final ClientConnection clientConnection;

        protected final HttpHeaders headers;

        public Endpoint(KeycloakSession session, AuthenticationCallback callback, EventBuilder event, TwitterIdentityProvider provider) {
            this.session = session;
            this.realm = session.getContext().getRealm();
            this.clientConnection = session.getContext().getConnection();
            this.callback = callback;
            this.event = event;
            this.provider = provider;
            this.headers = session.getContext().getRequestHeaders();
        }

        @GET
        public Response authResponse(@QueryParam("state") String state,
                                     @QueryParam("denied") String denied,
                                     @QueryParam("oauth_verifier") String verifier) {
            IdentityBrokerState idpState = IdentityBrokerState.encoded(state, realm);
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
                return callback.cancelled(provider.getConfig());
            }

            OAuth2IdentityProviderConfig providerConfig = provider.getConfig();

            try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(providerConfig.getClientSecret())) {
                String twitterToken = authSession.getAuthNote(TWITTER_TOKEN);
                RequestToken requestToken = base64DecodeRequestToken(twitterToken);

                AccessToken oAuthAccessToken = provider.oAuthAuthorization.getOAuthAccessToken(requestToken, verifier);

                Twitter twitter = Twitter.newBuilder()
                        .oAuthConsumer(providerConfig.getClientId(), vaultStringSecret.get().orElse(providerConfig.getClientSecret()))
                        .oAuthAccessToken(oAuthAccessToken)
                        .build();
                User twitterUser = twitter.v1().users().verifyCredentials();

                BrokeredIdentityContext identity = new BrokeredIdentityContext(Long.toString(twitterUser.getId()), providerConfig);
                identity.setIdp(provider);

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
                if (Booleans.isTrue(providerConfig.isStoreToken())) {
                    identity.setToken(token);
                }
                identity.getContextData().put(UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN, token);

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
        authSession.setUserSessionNote(UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN, (String) context.getContextData().get(UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN));

    }

}
