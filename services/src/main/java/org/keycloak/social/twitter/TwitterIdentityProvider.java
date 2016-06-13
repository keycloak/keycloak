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
import org.keycloak.common.ClientConnection;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.ErrorPage;
import org.keycloak.broker.social.SocialIdentityProvider;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.keycloak.models.ClientSessionModel.Action.AUTHENTICATE;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TwitterIdentityProvider extends AbstractIdentityProvider<OAuth2IdentityProviderConfig> implements
        SocialIdentityProvider<OAuth2IdentityProviderConfig> {

    protected static final Logger logger = Logger.getLogger(TwitterIdentityProvider.class);
    public TwitterIdentityProvider(OAuth2IdentityProviderConfig config) {
        super(config);
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

            URI uri = new URI(request.getRedirectUri() + "?state=" + request.getState());

            RequestToken requestToken = twitter.getOAuthRequestToken(uri.toString());
            ClientSessionModel clientSession = request.getClientSession();

            clientSession.setNote("twitter_token", requestToken.getToken());
            clientSession.setNote("twitter_tokenSecret", requestToken.getTokenSecret());

            URI authenticationUrl = URI.create(requestToken.getAuthenticationURL());

            return Response.temporaryRedirect(authenticationUrl).build();
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

            try {
                Twitter twitter = new TwitterFactory().getInstance();

                twitter.setOAuthConsumer(getConfig().getClientId(), getConfig().getClientSecret());

                ClientSessionModel clientSession = parseClientSessionCode(state).getClientSession();

                String twitterToken = clientSession.getNote("twitter_token");
                String twitterSecret = clientSession.getNote("twitter_tokenSecret");

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
                identity.setCode(state);
                identity.setIdpConfig(getConfig());

                return callback.authenticated(identity);
            } catch (Exception e) {
                logger.error("Could get user profile from twitter.", e);
            }
            EventBuilder event = new EventBuilder(realm, session, clientConnection);
            event.event(EventType.LOGIN);
            event.error("twitter_login_failed");
            return ErrorPage.error(session, Messages.UNEXPECTED_ERROR_HANDLING_RESPONSE);
        }

        private ClientSessionCode parseClientSessionCode(String code) {
            ClientSessionCode clientCode = ClientSessionCode.parse(code, this.session, this.realm);

            if (clientCode != null && clientCode.isValid(AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
                ClientSessionModel clientSession = clientCode.getClientSession();

                if (clientSession != null) {
                    ClientModel client = clientSession.getClient();

                    if (client == null) {
                        throw new IdentityBrokerException("Invalid client");
                    }

                    logger.debugf("Got authorization code from client [%s].", client.getClientId());
                }

                logger.debugf("Authorization code is valid.");

                return clientCode;
            }

            throw new IdentityBrokerException("Invalid code, please login again through your application.");
        }

    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        return Response.ok(identity.getToken()).type(MediaType.APPLICATION_JSON).build();
    }
}
