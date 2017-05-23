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
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TwitterIdentityProvider extends AbstractIdentityProvider<OAuth2IdentityProviderConfig> implements
        SocialIdentityProvider<OAuth2IdentityProviderConfig> {

    protected static final Logger logger = Logger.getLogger(TwitterIdentityProvider.class);

    private static final String TWITTER_TOKEN = "twitter_token";
    private static final String TWITTER_TOKENSECRET = "twitter_tokenSecret";

    public TwitterIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new Endpoint(realm, callback);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(getConfig().getClientId(), getConfig().getClientSecret());

            URI uri = new URI(request.getRedirectUri() + "?state=" + request.getState().getEncodedState());

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

    protected class Endpoint {
        protected RealmModel realm;
        protected AuthenticationCallback callback;

        @Context
        protected KeycloakSession session;

        @Context
        protected ClientConnection clientConnection;

        @Context
        protected HttpHeaders headers;

        @Context
        protected UriInfo uriInfo;

        public Endpoint(RealmModel realm, AuthenticationCallback callback) {
            this.realm = realm;
            this.callback = callback;
        }

        @GET
        public Response authResponse(@QueryParam("state") String state,
                                     @QueryParam("denied") String denied,
                                     @QueryParam("oauth_verifier") String verifier) {
            if (denied != null) {
                return callback.cancelled(state);
            }

            Response errorResponse = null;

            try {
                Twitter twitter = new TwitterFactory().getInstance();

                twitter.setOAuthConsumer(getConfig().getClientId(), getConfig().getClientSecret());

                AuthenticationSessionModel authSession = ClientSessionCode.getClientSession(state, session, realm, AuthenticationSessionModel.class);

                String twitterToken = authSession.getAuthNote(TWITTER_TOKEN);
                String twitterSecret = authSession.getAuthNote(TWITTER_TOKENSECRET);

                RequestToken requestToken = new RequestToken(twitterToken, twitterSecret);

                AccessToken oAuthAccessToken = twitter.getOAuthAccessToken(requestToken, verifier);
                twitter4j.User twitterUser = twitter.verifyCredentials();

                BrokeredIdentityContext identity = new BrokeredIdentityContext(Long.toString(twitterUser.getId()));
                identity.setIdp(TwitterIdentityProvider.this);

                identity.setUsername(twitterUser.getScreenName());
                identity.setName(twitterUser.getName());

                StringBuilder tokenBuilder = new StringBuilder();

                tokenBuilder.append("{");
                tokenBuilder.append("\"oauth_token\":").append("\"").append(oAuthAccessToken.getToken()).append("\"").append(",");
                tokenBuilder.append("\"oauth_token_secret\":").append("\"").append(oAuthAccessToken.getTokenSecret()).append("\"").append(",");
                tokenBuilder.append("\"screen_name\":").append("\"").append(oAuthAccessToken.getScreenName()).append("\"").append(",");
                tokenBuilder.append("\"user_id\":").append("\"").append(oAuthAccessToken.getUserId()).append("\"");
                tokenBuilder.append("}");

                identity.setToken(tokenBuilder.toString());
                identity.setIdpConfig(getConfig());
                identity.setCode(state);

                return callback.authenticated(identity);
            } catch (WebApplicationException e) {
                sendErrorEvent();
                return e.getResponse();
            } catch (Exception e) {
                logger.error("Could get user profile from twitter.", e);
                sendErrorEvent();
                return ErrorPage.error(session, Messages.UNEXPECTED_ERROR_HANDLING_RESPONSE);
            }
        }

        private void sendErrorEvent() {
            EventBuilder event = new EventBuilder(realm, session, clientConnection);
            event.event(EventType.LOGIN);
            event.error("twitter_login_failed");
        }

    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        return Response.ok(identity.getToken()).type(MediaType.APPLICATION_JSON).build();
    }
}
