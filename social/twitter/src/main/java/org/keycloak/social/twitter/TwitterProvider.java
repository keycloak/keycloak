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

import org.keycloak.models.ClientSessionModel;
import org.keycloak.social.AuthCallback;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.SocialAccessDeniedException;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;

import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TwitterProvider implements SocialProvider {

    @Override
    public String getId() {
        return "twitter";
    }

    @Override
    public AuthRequest getAuthUrl(ClientSessionModel clientSession, SocialProviderConfig config, String state) throws SocialProviderException {
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(config.getKey(), config.getSecret());

            URI uri = new URI(config.getCallbackUrl() + "?state=" + state);

            RequestToken requestToken = twitter.getOAuthRequestToken(uri.toString());
            clientSession.setNote("twitter_token", requestToken.getToken());
            clientSession.setNote("twitter_tokenSecret", requestToken.getTokenSecret());
            return AuthRequest.create(requestToken.getAuthenticationURL())
                    .build();
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

    @Override
    public String getName() {
        return "Twitter";
    }

    @Override
    public SocialUser processCallback(ClientSessionModel clientSession, SocialProviderConfig config, AuthCallback callback) throws SocialProviderException {
        if (callback.getQueryParam("denied") != null) {
            throw new SocialAccessDeniedException();
        }

        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(config.getKey(), config.getSecret());

            String token = callback.getQueryParam("oauth_token");
            String verifier = callback.getQueryParam("oauth_verifier");
            String twitterToken = clientSession.getNote("twitter_token");
            String twitterSecret = clientSession.getNote("twitter_tokenSecret");

            RequestToken requestToken = new RequestToken(twitterToken, twitterSecret);

            twitter.getOAuthAccessToken(requestToken, verifier);
            twitter4j.User twitterUser = twitter.verifyCredentials();

            SocialUser user = new SocialUser(Long.toString(twitterUser.getId()), twitterUser.getScreenName());
            user.setName(twitterUser.getName());

            return user;
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

}
