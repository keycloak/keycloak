/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.social.linkedin;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.oidc.util.JsonSimpleHttp;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.logging.KeycloakLogger;

/**
 * LinkedIn social provider. See https://developer.linkedin.com/docs/oauth2
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class LinkedInIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	private static final KeycloakLogger logger = Logger.getMessageLogger(KeycloakLogger.class, LinkedInIdentityProvider.class.getName());

	public static final String AUTH_URL = "https://www.linkedin.com/uas/oauth2/authorization";
	public static final String TOKEN_URL = "https://www.linkedin.com/uas/oauth2/accessToken";
	public static final String PROFILE_URL = "https://api.linkedin.com/v1/people/~:(id,formatted-name,email-address,public-profile-url)?format=json";
	public static final String DEFAULT_SCOPE = "r_basicprofile r_emailaddress";

	public LinkedInIdentityProvider(OAuth2IdentityProviderConfig config) {
		super(config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		logger.debug("doGetFederatedIdentity()");
		try {
			JsonNode profile = JsonSimpleHttp.asJson(SimpleHttp.doGet(PROFILE_URL).header("Authorization", "Bearer " + accessToken));

			BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "id"));

			String username = extractUsernameFromProfileURL(getJsonProperty(profile, "publicProfileUrl"));
			user.setUsername(username);
			user.setName(getJsonProperty(profile, "formattedName"));
			user.setEmail(getJsonProperty(profile, "emailAddress"));
			user.setIdpConfig(getConfig());
			user.setIdp(this);

			AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

			return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from linkedIn.", e);
		}
	}

	protected static String extractUsernameFromProfileURL(String profileURL) {
		if (isNotBlank(profileURL)) {

			try {
				logger.debug("go to extract username from profile URL " + profileURL);
				URL u = new URL(profileURL);
				String path = u.getPath();
				if (isNotBlank(path) && path.length() > 1) {
					if (path.startsWith("/")) {
						path = path.substring(1);
					}
					String[] pe = path.split("/");
					if (pe.length >= 2) {
						return URLDecoder.decode(pe[1], "UTF-8");
					} else {
						logger.IDP.linkedInProfileUrlIsWithoutSecondPart(profileURL);
					}
				} else {
					logger.IDP.linkedInProfileUrlIsWithoutPath(profileURL);
				}
			} catch (MalformedURLException e) {
				logger.IDP.linkedInProfileUrlIsMalformed(profileURL);
			} catch (Exception e) {
				logger.IDP.linkedInProfileUsernameExtractionFailed(e, profileURL);
			}
		}
		return null;
	}

	private static boolean isNotBlank(String s) {
		return s != null && s.trim().length() > 0;
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
