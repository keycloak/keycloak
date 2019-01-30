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
package org.keycloak.social.linkedin;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;

/**
 * LinkedIn social provider. See https://developer.linkedin.com/docs/oauth2
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class LinkedInIdentityProvider extends AbstractOAuth2IdentityProvider<OAuth2IdentityProviderConfig> implements SocialIdentityProvider<OAuth2IdentityProviderConfig> {

	private static final Logger log = Logger.getLogger(LinkedInIdentityProvider.class);

	public static final String AUTH_URL = "https://www.linkedin.com/oauth/v2/authorization";
	public static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
	public static final String PROFILE_URL = "https://api.linkedin.com/v2/me";
	public static final String EMAIL_URL = "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";
	public static final String EMAIL_SCOPE = "r_emailaddress";
	public static final String DEFAULT_SCOPE = "r_liteprofile " + EMAIL_SCOPE;

	public LinkedInIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
		// email scope is mandatory in order to resolve the username using the email address
		if (!config.getDefaultScope().contains(EMAIL_SCOPE)) {
			config.setDefaultScope(config.getDefaultScope() + " " + EMAIL_SCOPE);
		}
	}

	@Override
	protected boolean supportsExternalExchange() {
		return true;
	}

	@Override
	protected String getProfileEndpointForValidation(EventBuilder event) {
		return PROFILE_URL;
	}

	@Override
	protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
		BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "id"));

		user.setFirstName(getFirstMultiLocaleString(profile, "firstName"));
		user.setLastName(getFirstMultiLocaleString(profile, "lastName"));
		user.setIdpConfig(getConfig());
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		return user;
	}


	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		log.debug("doGetFederatedIdentity()");
		try {
			BrokeredIdentityContext identity = extractIdentityFromProfile(null, doHttpGet(PROFILE_URL, accessToken));

			identity.setEmail(fetchEmailAddress(accessToken, identity));

			if (identity.getUsername() == null) {
				identity.setUsername(identity.getEmail());
			}

			return identity;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from linkedIn.", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}

	private String fetchEmailAddress(String accessToken, BrokeredIdentityContext identity) {
		if (identity.getEmail() == null && getConfig().getDefaultScope() != null && getConfig().getDefaultScope().contains(EMAIL_SCOPE)) {
			try {
				JsonNode emailAddressNode = doHttpGet(EMAIL_URL, accessToken).findPath("emailAddress");

				if (emailAddressNode != null) {
					return emailAddressNode.asText();
				}
			} catch (IOException cause) {
				throw new RuntimeException("Failed to retrieve user email", cause);
			}
		}

		return null;
	}

	private JsonNode doHttpGet(String url, String bearerToken) throws IOException {
		JsonNode response = SimpleHttp.doGet(url, session).header("Authorization", "Bearer " + bearerToken).asJson();

		if (response.hasNonNull("serviceErrorCode")) {
			throw new IdentityBrokerException("Could not obtain response from [" + url + "]. Response from server: " + response);
		}

		return response;
	}

	private String getFirstMultiLocaleString(JsonNode node, String name) {
		JsonNode claim = node.get(name);

		if (claim != null) {
			JsonNode localized = claim.get("localized");

			if (localized != null) {
				Iterator<JsonNode> iterator = localized.iterator();

				if (iterator.hasNext()) {
					return iterator.next().asText();
				}
			}
		}

		return null;
	}
}
