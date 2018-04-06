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

package org.keycloak.social.bitbucket;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BitbucketIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	public static final String AUTH_URL = "https://bitbucket.org/site/oauth2/authorize";
	public static final String TOKEN_URL = "https://bitbucket.org/site/oauth2/access_token";
	public static final String USER_URL = "https://api.bitbucket.org/2.0/user";
	public static final String USER_EMAIL_URL = "https://api.bitbucket.org/2.0/user/emails";
	public static final String EMAIL_SCOPE = "email";
	public static final String ACCOUNT_SCOPE = "account";
	public static final String DEFAULT_SCOPE = ACCOUNT_SCOPE;

	public BitbucketIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		String defaultScope = config.getDefaultScope();

		if (defaultScope ==  null || defaultScope.trim().equals("")) {
			config.setDefaultScope(ACCOUNT_SCOPE + " " + EMAIL_SCOPE);
		}
	}

	@Override
	protected boolean supportsExternalExchange() {
		return true;
	}

	@Override
	protected String getProfileEndpointForValidation(EventBuilder event) {
		return USER_URL;
	}

	@Override
	protected BrokeredIdentityContext validateExternalTokenThroughUserInfo(EventBuilder event, String subjectToken, String subjectTokenType) {
		event.detail("validation_method", "user info");
		SimpleHttp.Response response = null;
		int status = 0;
		try {
			String userInfoUrl = getProfileEndpointForValidation(event);
			response = buildUserInfoRequest(subjectToken, userInfoUrl).asResponse();
			status = response.getStatus();
		} catch (IOException e) {
			logger.debug("Failed to invoke user info for external exchange", e);
		}
		if (status != 200) {
			logger.debug("Failed to invoke user info status: " + status);
			event.detail(Details.REASON, "user info call failure");
			event.error(Errors.INVALID_TOKEN);
			throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
		}
		JsonNode profile = null;
		try {
			profile = response.asJson();
		} catch (IOException e) {
			event.detail(Details.REASON, "user info call failure");
			event.error(Errors.INVALID_TOKEN);
			throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
		}
		String type = getJsonProperty(profile, "type");
		if (type == null) {
			event.detail(Details.REASON, "no type data in user info response");
			event.error(Errors.INVALID_TOKEN);
			throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);

		}
		if (type.equals("error")) {
			JsonNode errorNode = profile.get("error");
			if (errorNode != null) {
				String errorMsg = getJsonProperty(errorNode, "message");
				event.detail(Details.REASON, "user info call failure: " + errorMsg);
				event.error(Errors.INVALID_TOKEN);
				throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
			} else {
				event.detail(Details.REASON, "user info call failure");
				event.error(Errors.INVALID_TOKEN);
				throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
			}
		}
		if (!type.equals("user")) {
			event.detail(Details.REASON, "no user info in response");
			event.error(Errors.INVALID_TOKEN);
			throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);

		}
		String id = getJsonProperty(profile, "account_id");
		if (id == null) {
				event.detail(Details.REASON, "user info call failure");
				event.error(Errors.INVALID_TOKEN);
				throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);

		}
		return extractUserInfo(subjectToken, profile);
	}

	private BrokeredIdentityContext extractUserInfo(String subjectToken, JsonNode profile) {
		BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "account_id"));


		String username = getJsonProperty(profile, "username");
		user.setUsername(username);
		user.setName(getJsonProperty(profile, "display_name"));
		user.setIdpConfig(getConfig());
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		try {
			JsonNode emails = SimpleHttp.doGet(USER_EMAIL_URL, session).header("Authorization", "Bearer " + subjectToken).asJson();

			// {"pagelen":10,"values":[{"is_primary":true,"is_confirmed":true,"type":"email","email":"bburke@redhat.com","links":{"self":{"href":"https://api.bitbucket.org/2.0/user/emails/bburke@redhat.com"}}}],"page":1,"size":1}
			JsonNode emailJson = emails.get("values");
			if (emailJson != null) {
                if (emailJson.isArray()) {
                    emailJson = emailJson.get(0);
                }
                if (emailJson != null && "email".equals(getJsonProperty(emailJson, "type"))) {
                    user.setEmail(getJsonProperty(emailJson, "email"));

                }
            }
		} catch (Exception ignore) {
			logger.debug("failed to get email from BitBucket", ignore);

		}
		return user;
	}

	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			JsonNode profile = SimpleHttp.doGet(USER_URL, session).header("Authorization", "Bearer " + accessToken).asJson();

			String type = getJsonProperty(profile, "type");
			if (type == null) {
				throw new IdentityBrokerException("Could not obtain account information from bitbucket.");

			}
			if (type.equals("error")) {
				JsonNode errorNode = profile.get("error");
				if (errorNode != null) {
					String errorMsg = getJsonProperty(errorNode, "message");
					throw new IdentityBrokerException("Could not obtain account information from bitbucket.  Error: " + errorMsg);
				} else {
					throw new IdentityBrokerException("Could not obtain account information from bitbucket.");
				}
			}
			if (!type.equals("user")) {
				logger.debug("Unknown object type: " + type);
				throw new IdentityBrokerException("Could not obtain account information from bitbucket.");

			}
			return extractUserInfo(accessToken, profile);
		} catch (Exception e) {
			if (e instanceof IdentityBrokerException) throw (IdentityBrokerException)e;
			throw new IdentityBrokerException("Could not obtain user profile from github.", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
