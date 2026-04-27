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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.util.BasicAuthHelper;

import com.fasterxml.jackson.databind.JsonNode;

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
    public static final String APPLICATIONS_FRAGMENT = "/applications";
    public static final String PROFILE_FRAGMENT = "/user";
    public static final String EMAIL_FRAGMENT = "/user/emails";
    public static final String DEFAULT_APPLICATIONS_URL = DEFAULT_API_URL + APPLICATIONS_FRAGMENT;
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
    /** API URL key in config map. */
    protected static final String GITHUB_JSON_FORMAT_KEY = "githubJsonFormat";
    /** Email URL key in config map. */
    protected static final String EMAIL_URL_KEY = "emailUrl";

    private final String authUrl;
    private final String tokenUrl;
    private final String profileUrl;
    private final String emailUrl;
    private final boolean githubJsonFormat;

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
        githubJsonFormat = Boolean.parseBoolean(config.getConfig().getOrDefault(GITHUB_JSON_FORMAT_KEY, "false"));
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
    public SimpleHttpRequest authenticateTokenRequest(SimpleHttpRequest tokenRequest) {
        SimpleHttpRequest simpleHttp = super.authenticateTokenRequest(tokenRequest);
        if (githubJsonFormat) {
            simpleHttp.acceptJson();
        }
        return simpleHttp;
    }

    @Override
	protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
		BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "id"), getConfig());

		String username = getJsonProperty(profile, "login");
		user.setUsername(username);
		user.setName(getJsonProperty(profile, "name"));
		user.setEmail(getJsonProperty(profile, "email"));
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		return user;
	}

	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try (SimpleHttpResponse response = SimpleHttp.create(session).doGet(profileUrl)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "application/json")
                        .asResponse()) {

                    if (Response.Status.fromStatusCode(response.getStatus()).getFamily() != Response.Status.Family.SUCCESSFUL) {
                        logger.warnf("Profile endpoint returned an error (%d): %s", response.getStatus(), response.asString());
                        throw new IdentityBrokerException("Profile could not be retrieved from the github endpoint");
                    }

                    JsonNode profile = response.asJson();
                    logger.tracef("profile retrieved from github: %s", profile);
                    BrokeredIdentityContext user = extractIdentityFromProfile(null, profile);

                    if (user.getEmail() == null) {
                        user.setEmail(searchEmail(accessToken));
                    }
                    return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Profile could not be retrieved from the github endpoint", e);
		}
	}

	private String searchEmail(String accessToken) {
		try (SimpleHttpResponse response = SimpleHttp.create(session).doGet(emailUrl)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "application/json")
                        .asResponse()) {

                    if (Response.Status.fromStatusCode(response.getStatus()).getFamily() != Response.Status.Family.SUCCESSFUL) {
                        logger.warnf("Primary email endpoint returned an error (%d): %s", response.getStatus(), response.asString());
                        throw new IdentityBrokerException("Primary email could not be retrieved from the github endpoint");
                    }

                    JsonNode emails = response.asJson();
                    logger.tracef("emails retrieved from github: %s", emails);
                    if (emails.isArray()) {
                        Iterator<JsonNode> loop = emails.elements();
                        while (loop.hasNext()) {
                            JsonNode mail = loop.next();
                            JsonNode primary = mail.get("primary");
                            if (primary != null && primary.asBoolean()) {
                                return getJsonProperty(mail, "email");
                            }
                        }
                    }

                    throw new IdentityBrokerException("Primary email from github is not found in the user's email list.");
		} catch (Exception e) {
			throw new IdentityBrokerException("Primary email could not be retrieved from the github endpoint", e);
		}
	}

    private void verifyToken(String accessToken) throws IOException {
        String tokenUrl = DEFAULT_APPLICATIONS_URL + "/" + getConfig().getClientId() + "/token";
        SimpleHttpResponse response = SimpleHttp.create(session).doPost(tokenUrl)
                .header("Authorization",  BasicAuthHelper.createHeader(getConfig().getClientId(), getConfig().getClientSecret()))
                .json(Map.of("access_token", accessToken)).asResponse();

        JsonNode jsonNodeResponse = response.asJson();
        if (response.getStatus() != 200) {
            String errorMessage = getJsonProperty(jsonNodeResponse, "message");
            throw new RuntimeException("Error message: " + errorMessage);
        }

        JsonNode appNode = jsonNodeResponse.get("app");
        if (appNode == null || appNode.isNull()) {
            throw new RuntimeException("Invalid token check response: 'app' field is missing.");
        }

        String clientId = getJsonProperty(appNode, "client_id");
        if (!getConfig().getClientId().equals(clientId)) {
            throw new RuntimeException("Client ID does not match the client_id in the access token check response.");
        }
    }

    @Override
    protected BrokeredIdentityContext exchangeExternalTokenV2Impl(TokenExchangeContext tokenExchangeContext) {
        String subjectToken = tokenExchangeContext.getFormParams().getFirst(OAuth2Constants.SUBJECT_TOKEN);
        if (subjectToken == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "token not set", Response.Status.BAD_REQUEST);
        }
        try {
            verifyToken(subjectToken);
            return doGetFederatedIdentity(subjectToken);
        }
        catch (Exception e) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
