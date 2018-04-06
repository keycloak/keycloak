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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.models.IdentityProviderModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

}
