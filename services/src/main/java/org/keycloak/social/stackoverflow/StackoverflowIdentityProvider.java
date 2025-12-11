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
package org.keycloak.social.stackoverflow;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.KeycloakSession;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * Stackoverflow social provider. See https://api.stackexchange.com/docs/authentication
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class StackoverflowIdentityProvider extends AbstractOAuth2IdentityProvider<StackOverflowIdentityProviderConfig> implements SocialIdentityProvider<StackOverflowIdentityProviderConfig> {

	private static final Logger log = Logger.getLogger(StackoverflowIdentityProvider.class);

	public static final String AUTH_URL = "https://stackexchange.com/oauth";
	public static final String TOKEN_URL = "https://stackexchange.com/oauth/access_token";
	public static final String PROFILE_URL = "https://api.stackexchange.com/2.2/me?order=desc&sort=name&site=stackoverflow";
	public static final String DEFAULT_SCOPE = "";

	public StackoverflowIdentityProvider(KeycloakSession session, StackOverflowIdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
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
	protected SimpleHttpRequest buildUserInfoRequest(String subjectToken, String userInfoUrl) {
		String URL = PROFILE_URL + "&access_token=" + subjectToken + "&key=" + getConfig().getKey();
		return SimpleHttp.create(session).doGet(URL);
	}

	@Override
	protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode node) {
		JsonNode profile = node.get("items").get(0);

		BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "user_id"), getConfig());

		String username = extractUsernameFromProfileURL(getJsonProperty(profile, "link"));
		user.setUsername(username);
		user.setName(unescapeHtml3(getJsonProperty(profile, "display_name")));
		// email is not provided
		// user.setEmail(getJsonProperty(profile, "email"));
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		return user;
	}

	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		log.debug("doGetFederatedIdentity()");
		try {

			String URL = PROFILE_URL + "&access_token=" + accessToken + "&key=" + getConfig().getKey();
			if (log.isDebugEnabled()) {
				log.debug("StackOverflow profile request to: " + URL);
			}
			return extractIdentityFromProfile(null, SimpleHttp.create(session).doGet(URL).asJson());
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from Stackoverflow: " + e.getMessage(), e);
		}
	}

	protected static String extractUsernameFromProfileURL(String profileURL) {
		if (isNotBlank(profileURL)) {

			try {
				log.debug("go to extract username from profile URL " + profileURL);
				URL u = new URL(profileURL);
				String path = u.getPath();
				if (isNotBlank(path) && path.length() > 1) {
					if (path.startsWith("/")) {
						path = path.substring(1);
					}
					String[] pe = path.split("/");
					if (pe.length >= 3) {
						return URLDecoder.decode(pe[2], StandardCharsets.UTF_8);
					} else {
						log.warn("Stackoverflow profile URL path is without third part: " + profileURL);
					}
				} else {
					log.warn("Stackoverflow profile URL is without path part: " + profileURL);
				}
			} catch (MalformedURLException e) {
				log.warn("Stackoverflow profile URL is malformed: " + profileURL);
			} catch (Exception e) {
				log.warn("Stackoverflow profile URL " + profileURL + " username extraction failed due: " + e.getMessage());
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

	public static final String unescapeHtml3(final String input) {
		if (input == null)
			return null;
		StringWriter writer = null;
		int len = input.length();
		int i = 1;
		int st = 0;
		while (true) {
			// look for '&'
			while (i < len && input.charAt(i - 1) != '&')
				i++;
			if (i >= len)
				break;

			// found '&', look for ';'
			int j = i;
			while (j < len && j < i + MAX_ESCAPE + 1 && input.charAt(j) != ';')
				j++;
			if (j == len || j < i + MIN_ESCAPE || j == i + MAX_ESCAPE + 1) {
				i++;
				continue;
			}

			// found escape
			if (input.charAt(i) == '#') {
				// numeric escape
				int k = i + 1;
				int radix = 10;

				final char firstChar = input.charAt(k);
				if (firstChar == 'x' || firstChar == 'X') {
					k++;
					radix = 16;
				}

				try {
					int entityValue = Integer.parseInt(input.substring(k, j), radix);

					if (writer == null)
						writer = new StringWriter(input.length());
					writer.append(input.substring(st, i - 1));

					if (entityValue > 0xFFFF) {
						final char[] chrs = Character.toChars(entityValue);
						writer.write(chrs[0]);
						writer.write(chrs[1]);
					} else {
						writer.write(entityValue);
					}

				} catch (NumberFormatException ex) {
					i++;
					continue;
				}
			} else {
				// named escape
				CharSequence value = lookupMap.get(input.substring(i, j));
				if (value == null) {
					i++;
					continue;
				}

				if (writer == null)
					writer = new StringWriter(input.length());
				writer.append(input.substring(st, i - 1));

				writer.append(value);
			}

			// skip escape
			st = j + 1;
			i = st;
		}

		if (writer != null) {
			writer.append(input.substring(st, len));
			return writer.toString();
		}
		return input;
	}

	private static final String[][] ESCAPES = { { "\"", "quot" }, // " - double-quote
			{ "&", "amp" }, // & - ampersand
			{ "<", "lt" }, // < - less-than
			{ ">", "gt" }, // > - greater-than
	};

	private static final int MIN_ESCAPE = 2;
	private static final int MAX_ESCAPE = 6;

	private static final HashMap<String, CharSequence> lookupMap;
	static {
		lookupMap = new HashMap<String, CharSequence>();
		for (final CharSequence[] seq : ESCAPES)
			lookupMap.put(seq[1].toString(), seq[0]);
	}
}
