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

import java.net.URI;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.social.IdentityProvider;
import org.keycloak.social.IdentityProviderCallback;
import org.picketlink.idm.model.SimpleUser;
import org.picketlink.idm.model.User;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TwitterProvider implements IdentityProvider {

    private static final Logger log = Logger.getLogger(TwitterProvider.class);

    @Override
    public String getId() {
        return "twitter";
    }

    @Override
    public URI getAuthUrl(IdentityProviderCallback callback) {
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(callback.getProviderKey(), callback.getProviderSecret());

            RequestToken requestToken = twitter.getOAuthRequestToken();
            callback.putState(requestToken.getToken(), requestToken);
            return callback.createUri(requestToken.getAuthenticationURL()).build();
        } catch (Exception e) {
            log.error("Failed to retrieve login url", e);
            return null;
        }
    }

    @Override
    public String getName() {
        return "Twitter";
    }

    @Override
    public boolean isCallbackHandler(IdentityProviderCallback callback) {
        return callback.containsQueryParam("oauth_token") && callback.containsState(callback.getQueryParam("oauth_token"));
    }

    @Override
    public User processCallback(IdentityProviderCallback callback) {
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(callback.getProviderKey(), callback.getProviderSecret());

            String verifier = callback.getQueryParam("oauth_verifier");
            RequestToken requestToken = callback.getState(callback.getQueryParam("oauth_token"));

            twitter.getOAuthAccessToken(requestToken, verifier);
            twitter4j.User twitterUser = twitter.verifyCredentials();

            User user = new SimpleUser(String.valueOf(twitterUser.getScreenName()));
            user.setFirstName(twitterUser.getName());
            return user;
        } catch (Exception e) {
            log.error("Failed to process callback", e);
            return null;
        }
    }

}
