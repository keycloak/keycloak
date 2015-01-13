/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.social.twitter;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.AuthenticationResponse;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.social.SocialIdentityProvider;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TwitterIdentityProvider extends AbstractIdentityProvider<OAuth2IdentityProviderConfig> implements
        SocialIdentityProvider<OAuth2IdentityProviderConfig> {

    public TwitterIdentityProvider(OAuth2IdentityProviderConfig config) {
        super(config);
    }

    @Override
    public AuthenticationResponse handleRequest(AuthenticationRequest request) {
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(getConfig().getClientId(), getConfig().getClientSecret());

            URI uri = new URI(request.getRedirectUri() + "?state=" + request.getState());

            RequestToken requestToken = twitter.getOAuthRequestToken(uri.toString());
            ClientSessionModel clientSession = request.getClientSession();

            clientSession.setNote("twitter_token", requestToken.getToken());
            clientSession.setNote("twitter_tokenSecret", requestToken.getTokenSecret());

            URI authenticationUrl = URI.create(requestToken.getAuthenticationURL());

            return AuthenticationResponse.temporaryRedirect(authenticationUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getRelayState(AuthenticationRequest request) {
        UriInfo uriInfo = request.getUriInfo();
        return uriInfo.getQueryParameters().getFirst("state");
    }

    @Override
    public AuthenticationResponse handleResponse(AuthenticationRequest request) {
        MultivaluedMap<String, String> queryParameters = request.getUriInfo().getQueryParameters();

        if (queryParameters.getFirst("denied") != null) {
            throw new RuntimeException("Access denied.");
        }

        try {
            Twitter twitter = new TwitterFactory().getInstance();

            twitter.setOAuthConsumer(getConfig().getClientId(), getConfig().getClientSecret());

            String verifier = queryParameters.getFirst("oauth_verifier");

            ClientSessionModel clientSession = request.getClientSession();

            String twitterToken = clientSession.getNote("twitter_token");
            String twitterSecret = clientSession.getNote("twitter_tokenSecret");

            RequestToken requestToken = new RequestToken(twitterToken, twitterSecret);

            twitter.getOAuthAccessToken(requestToken, verifier);
            twitter4j.User twitterUser = twitter.verifyCredentials();

            FederatedIdentity user = new FederatedIdentity(Long.toString(twitterUser.getId()));

            user.setUsername(twitterUser.getScreenName());
            user.setName(twitterUser.getName());

            return AuthenticationResponse.end(user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
