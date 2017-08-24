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
package org.keycloak.test.broker.oidc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.sessions.AuthenticationSessionModel;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unit test for {@link org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class AbstractOAuth2IdentityProviderTest {
	
	@Test
	public void constructor_defaultScopeHandling(){
		TestProvider tested = getTested();
		
		//default scope is set from the provider if not provided in the configuration
		Assert.assertEquals( tested.getDefaultScopes(), tested.getConfig().getDefaultScope());
		
		//default scope is preserved if provided in the configuration
		IdentityProviderModel model = new IdentityProviderModel();
		OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig(model);
		config.setDefaultScope("myscope");
		tested = new TestProvider(config);

		Assert.assertEquals("myscope", tested.getConfig().getDefaultScope());
		
	}

	@Test
	public void getJsonProperty_asJsonNode() throws IOException {
		TestProvider tested = getTested();

		JsonNode jsonNode = tested
				.asJsonNode("{\"nullone\":null, \"emptyone\":\"\", \"blankone\": \" \", \"withvalue\" : \"my value\", \"withbooleanvalue\" : true, \"withnumbervalue\" : 10}");
		Assert.assertNull(tested.getJsonProperty(jsonNode, "nonexisting"));
		Assert.assertNull(tested.getJsonProperty(jsonNode, "nullone"));
		Assert.assertNull(tested.getJsonProperty(jsonNode, "emptyone"));
		Assert.assertEquals(" ", tested.getJsonProperty(jsonNode, "blankone"));
		Assert.assertEquals("my value", tested.getJsonProperty(jsonNode, "withvalue"));
		Assert.assertEquals("true", tested.getJsonProperty(jsonNode, "withbooleanvalue"));
		Assert.assertEquals("10", tested.getJsonProperty(jsonNode, "withnumbervalue"));
	}

	@Test(expected = IdentityBrokerException.class)
	public void getFederatedIdentity_responseUrlLine_tokenNotFound() {
		TestProvider tested = getTested();
		tested.getFederatedIdentity("cosi=sss");
	}

	@Test(expected = IdentityBrokerException.class)
	public void getFederatedIdentity_responseJSON_tokenNotFound() {
		TestProvider tested = getTested();
		Map<String, String> notes = new HashMap<>();
		tested.getFederatedIdentity("{\"cosi\":\"sss\"}");
	}

	@Test(expected = IdentityBrokerException.class)
	public void getFederatedIdentity_responseJSON_invalidFormat() {
		TestProvider tested = getTested();
		Map<String, String> notes = new HashMap<>();
		tested.getFederatedIdentity("{\"cosi\":\"sss\"");
	}

	@Test(expected = IdentityBrokerException.class)
	public void getFederatedIdentity_responseJSON_emptyTokenField() {
		TestProvider tested = getTested();
		tested.getFederatedIdentity("{\""
				+ AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_ACCESS_TOKEN + "\" : \"\"}");
	}

	@Test(expected = IdentityBrokerException.class)
	public void getFederatedIdentity_responseJSON_nullTokenField() {
		TestProvider tested = getTested();
		tested.getFederatedIdentity("{\""
				+ AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_ACCESS_TOKEN + "\" : null}");
	}

	@Test
	public void getFederatedIdentity_responseJSON() {
		TestProvider tested = getTested();
		BrokeredIdentityContext fi = tested.getFederatedIdentity("{\""
				+ AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_ACCESS_TOKEN + "\" : \"458rt\"}");
		Assert.assertNotNull(fi);
		Assert.assertEquals("458rt", fi.getId());
	}

	@Test
	public void getFederatedIdentity_responseUrlLine() {
		TestProvider tested = getTested();
        BrokeredIdentityContext fi = tested.getFederatedIdentity("cosi=sss&"
				+ AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_ACCESS_TOKEN + "=458rtf&kdesi=ss}");
		Assert.assertNotNull(fi);
		Assert.assertEquals("458rtf", fi.getId());
	}

	@Test
	public void testCreateAuthorizationUrl() {
		final TestProvider tested = getTested("http://service/auth", "scope", "clientId", Boolean.TRUE);
		final AuthenticationRequest authenticationRequest = getAuthenticationRequest("state", "redirect", "login_hint");

		final Response response = tested.performLogin(authenticationRequest);
		
		Assert.assertEquals("http://service/auth?scope=scope&state=state&response_type=code&client_id=clientId&redirect_uri=redirect&login_hint=login_hint", response.getLocation().toString());
	}

	@Test
	public void testCreateAuthorizationUrlNoLoginHint() {
		final TestProvider tested = getTested("http://service/auth", "scope", "clientId", Boolean.FALSE);
		final AuthenticationRequest authenticationRequest = getAuthenticationRequest("state", "redirect", "login_hint");
		
		final Response response = tested.performLogin(authenticationRequest);
		
		Assert.assertEquals("http://service/auth?scope=scope&state=state&response_type=code&client_id=clientId&redirect_uri=redirect", response.getLocation().toString());
	}

	private AuthenticationRequest getAuthenticationRequest(final String state, final String redirectUri, final String loginHint) {
		final KeycloakSession keycloakSession = new DefaultKeycloakSessionFactory().create();
		final IdentityBrokerState identityBrokerState = IdentityBrokerState.encoded(state);
		final TestAuthenticationSessionModel testAuthenticationSessionModel = new TestAuthenticationSessionModel();
		testAuthenticationSessionModel.setClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
		return new AuthenticationRequest(keycloakSession, null, testAuthenticationSessionModel, null, null, identityBrokerState, redirectUri);
	}

	private TestProvider getTested(final String autorizationUrl, final String defaultScope, final String clientId, final Boolean isLoginHint) {
		OAuth2IdentityProviderConfig config = getConfig(autorizationUrl, defaultScope, clientId, isLoginHint);
		return new TestProvider(config);
	}

	/**
	 * Default provider with no specific value
	 */
	private TestProvider getTested() {		
		return new TestProvider(getConfig(null, null, null, Boolean.FALSE));
	}

	private OAuth2IdentityProviderConfig getConfig(final String autorizationUrl, final String defaultScope, final String clientId, final Boolean isLoginHint) {
		IdentityProviderModel model = new IdentityProviderModel();
		OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig(model);
		config.setAuthorizationUrl(autorizationUrl);
		config.setDefaultScope(defaultScope);
		config.setClientId(clientId);
		config.setLoginHint(isLoginHint);
		return config;
	}

	private static class TestProvider extends AbstractOAuth2IdentityProvider<OAuth2IdentityProviderConfig> {

		public TestProvider(OAuth2IdentityProviderConfig config) {
			super(null, config);
		}

		@Override
		protected String getDefaultScopes() {
			return "default";
		}

		protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
			return new BrokeredIdentityContext(accessToken);
		};

	};
	
	private class TestAuthenticationSessionModel implements AuthenticationSessionModel {

		private final Map<String, String> clientNodes = new HashMap<>();
		
		@Override
		public Map<String, ExecutionStatus> getExecutionStatus() {
			return null;
		}

		@Override
		public void setExecutionStatus(final String authenticator, final ExecutionStatus status) {

		}

		@Override
		public void clearExecutionStatus() {

		}

		@Override
		public UserModel getAuthenticatedUser() {
			return null;
		}

		@Override
		public void setAuthenticatedUser(final UserModel user) {

		}

		@Override
		public Set<String> getRequiredActions() {
			return null;
		}

		@Override
		public void addRequiredAction(final String action) {

		}

		@Override
		public void removeRequiredAction(final String action) {

		}

		@Override
		public void addRequiredAction(final RequiredAction action) {

		}

		@Override
		public void removeRequiredAction(final RequiredAction action) {

		}

		@Override
		public void setUserSessionNote(final String name, final String value) {

		}

		@Override
		public Map<String, String> getUserSessionNotes() {
			return null;
		}

		@Override
		public void clearUserSessionNotes() {

		}

		@Override
		public String getAuthNote(final String name) {
			return null;
		}

		@Override
		public void setAuthNote(final String name, final String value) {

		}

		@Override
		public void removeAuthNote(final String name) {

		}

		@Override
		public void clearAuthNotes() {

		}

		@Override
		public String getClientNote(final String name) {
			return clientNodes.get(name);
		}

		@Override
		public void setClientNote(final String name, final String value) {
			clientNodes.put(name, value);
		}

		@Override
		public void removeClientNote(final String name) {

		}

		@Override
		public Map<String, String> getClientNotes() {
			return null;
		}

		@Override
		public void clearClientNotes() {

		}

		@Override
		public void updateClient(final ClientModel client) {

		}

		@Override
		public void restartSession(final RealmModel realm, final ClientModel client) {

		}

		@Override
		public String getRedirectUri() {
			return null;
		}

		@Override
		public void setRedirectUri(final String uri) {

		}

		@Override
		public String getId() {
			return null;
		}

		@Override
		public RealmModel getRealm() {
			return null;
		}

		@Override
		public ClientModel getClient() {
			return null;
		}

		@Override
		public int getTimestamp() {
			return 0;
		}

		@Override
		public void setTimestamp(final int timestamp) {

		}

		@Override
		public String getAction() {
			return null;
		}

		@Override
		public void setAction(final String action) {

		}

		@Override
		public String getProtocol() {
			return null;
		}

		@Override
		public void setProtocol(final String method) {

		}

		@Override
		public Set<String> getRoles() {
			return null;
		}

		@Override
		public void setRoles(final Set<String> roles) {

		}

		@Override
		public Set<String> getProtocolMappers() {
			return null;
		}

		@Override
		public void setProtocolMappers(final Set<String> protocolMappers) {

		}
	}

}
