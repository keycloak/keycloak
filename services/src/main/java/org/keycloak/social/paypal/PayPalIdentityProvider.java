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

package org.keycloak.social.paypal;

import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Petter Lysne (petterlysne at hotmail dot com)
 */
public class PayPalIdentityProvider extends AbstractOAuth2IdentityProvider<PayPalIdentityProviderConfig> implements SocialIdentityProvider<PayPalIdentityProviderConfig>{

  public static final String BASE_URL = "https://api.paypal.com/v1";
  public static final String AUTH_URL = "https://www.paypal.com/signin/authorize";
	public static final String TOKEN_RESOURCE = "/identity/openidconnect/tokenservice";
	public static final String PROFILE_RESOURCE = "/oauth2/token/userinfo?schema=openid";
	public static final String DEFAULT_SCOPE = "openid profile email";

	public PayPalIdentityProvider(KeycloakSession session, PayPalIdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(config.targetSandbox() ? "https://www.sandbox.paypal.com/signin/authorize" : AUTH_URL);
		config.setTokenUrl((config.targetSandbox() ? "https://api.sandbox.paypal.com/v1" : BASE_URL) + TOKEN_RESOURCE);
		config.setUserInfoUrl((config.targetSandbox() ? "https://api.sandbox.paypal.com/v1" : BASE_URL) + PROFILE_RESOURCE);
	}

	@Override
	protected boolean supportsExternalExchange() {
		return true;
	}

	@Override
	protected String getProfileEndpointForValidation(EventBuilder event) {
		return getConfig().getUserInfoUrl();
	}

	@Override
	protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
		BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "user_id"), getConfig());

		user.setUsername(getJsonProperty(profile, "email"));
		user.setName(getJsonProperty(profile, "name"));
		user.setEmail(getJsonProperty(profile, "email"));
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
		return user;
	}


	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			JsonNode profile = SimpleHttp.create(session).doGet(getConfig().getUserInfoUrl()).header("Authorization", "Bearer " + accessToken).asJson();

			return extractIdentityFromProfile(null, profile);
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from paypal.", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
