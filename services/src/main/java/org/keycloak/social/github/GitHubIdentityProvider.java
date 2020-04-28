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

package org.keycloak.social.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Iterator;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GitHubIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

    public static final String DEFAULT_BASE_URL = "https://github.com";
    public static final String AUTH_FRAGMENT = "/login/oauth/authorize";
    public static final String TOKEN_FRAGMENT = "/login/oauth/access_token";
    public static final String DEFAULT_AUTH_URL = DEFAULT_BASE_URL + AUTH_FRAGMENT;
    public static final String DEFAULT_TOKEN_URL = DEFAULT_BASE_URL + TOKEN_FRAGMENT;
    /** @deprecated Use {@link #DEFAULT_AUTH_URL} instead. */
    @Deprecated
    public static final String AUTH_URL = DEFAULT_AUTH_URL;
    /** @deprecated Use {@link #DEFAULT_TOKEN_URL} instead. */
    @Deprecated
    public static final String TOKEN_URL = DEFAULT_TOKEN_URL;

    public static final String DEFAULT_API_URL = "https://api.github.com";
    public static final String PROFILE_FRAGMENT = "/user";
    public static final String EMAIL_FRAGMENT = "/user/emails";
    public static final String DEFAULT_PROFILE_URL = DEFAULT_API_URL + PROFILE_FRAGMENT;
    public static final String DEFAULT_EMAIL_URL = DEFAULT_API_URL + EMAIL_FRAGMENT;
    /** @deprecated Use {@link #DEFAULT_PROFILE_URL} instead. */
    @Deprecated
    public static final String PROFILE_URL = DEFAULT_PROFILE_URL;
    /** @deprecated Use {@link #DEFAULT_EMAIL_URL} instead. */
    @Deprecated
    public static final String EMAIL_URL = DEFAULT_EMAIL_URL;

    public static final String DEFAULT_SCOPE = "user:email";

    /** Base URL key in config map. */
    protected static final String BASE_URL_KEY = "baseUrl";
    /** API URL key in config map. */
    protected static final String API_URL_KEY = "apiUrl";
    /** Email URL key in config map. */
    protected static final String EMAIL_URL_KEY = "emailUrl";

    private final String authUrl;
    private final String tokenUrl;
    private final String profileUrl;
    private final String emailUrl;

    public GitHubIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);

        String baseUrl = getUrlFromConfig(config, BASE_URL_KEY, DEFAULT_BASE_URL);
        String apiUrl = getUrlFromConfig(config, API_URL_KEY, DEFAULT_API_URL);

        authUrl = baseUrl + AUTH_FRAGMENT;
        tokenUrl = baseUrl + TOKEN_FRAGMENT;
        profileUrl = apiUrl + PROFILE_FRAGMENT;
        emailUrl = apiUrl + EMAIL_FRAGMENT;

        config.setAuthorizationUrl(authUrl);
        config.setTokenUrl(tokenUrl);
        config.setUserInfoUrl(profileUrl);
        config.getConfig().put(EMAIL_URL_KEY, emailUrl);
    }

    /**
     * Get URL from config with default value fallback.
     *
     * @param config Identity provider configuration.
     * @param key Key to look for value in config's config map.
     * @param defaultValue Default value if value at key is null or empty string.
     * @return URL for specified key in the configuration with default value fallback.
     */
    protected static String getUrlFromConfig(OAuth2IdentityProviderConfig config, String key, String defaultValue) {
        String url = config.getConfig().get(key);
        if (url == null || url.trim().isEmpty()) {
            url = defaultValue;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

	@Override
	protected boolean supportsExternalExchange() {
		return true;
	}

	@Override
	protected String getProfileEndpointForValidation(EventBuilder event) {
		return profileUrl;
	}

	@Override
	protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
		BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "id"));

		String username = getJsonProperty(profile, "login");
		user.setUsername(username);
		user.setName(getJsonProperty(profile, "name"));
		user.setEmail(getJsonProperty(profile, "email"));
		user.setIdpConfig(getConfig());
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		return user;
	}

	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			JsonNode profile = SimpleHttp.doGet(profileUrl, session).header("Authorization", "Bearer " + accessToken).asJson();

			BrokeredIdentityContext user = extractIdentityFromProfile(null, profile);

			if (user.getEmail() == null) {
				user.setEmail(searchEmail(accessToken));
			}

			return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from github.", e);
		}
	}

	private String searchEmail(String accessToken) {
		try {
			ArrayNode emails = (ArrayNode) SimpleHttp.doGet(emailUrl, session).header("Authorization", "Bearer " + accessToken).asJson();

			Iterator<JsonNode> loop = emails.elements();
			while (loop.hasNext()) {
				JsonNode mail = loop.next();
				if (mail.get("primary").asBoolean()) {
					return getJsonProperty(mail, "email");
				}
			}
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user email from github.", e);
		}
		throw new IdentityBrokerException("Primary email from github is not found.");
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
