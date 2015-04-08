/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.keycloak.broker.oidc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.models.IdentityProviderModel;

/**
 * Unit test for {@link AbstractOAuth2IdentityProvider}
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
		Map<String, String> notes = new HashMap<>();
		tested.getFederatedIdentity(notes, "cosi=sss");
	}

	@Test(expected = IdentityBrokerException.class)
	public void getFederatedIdentity_responseJSON_tokenNotFound() {
		TestProvider tested = getTested();
		Map<String, String> notes = new HashMap<>();
		tested.getFederatedIdentity(notes, "{\"cosi\":\"sss\"}");
	}

	@Test(expected = IdentityBrokerException.class)
	public void getFederatedIdentity_responseJSON_invalidFormat() {
		TestProvider tested = getTested();
		Map<String, String> notes = new HashMap<>();
		tested.getFederatedIdentity(notes, "{\"cosi\":\"sss\"");
	}

	@Test(expected = IdentityBrokerException.class)
	public void getFederatedIdentity_responseJSON_emptyTokenField() {
		TestProvider tested = getTested();
		Map<String, String> notes = new HashMap<>();
		tested.getFederatedIdentity(notes, "{\""
				+ AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_ACCESS_TOKEN + "\" : \"\"}");
	}

	@Test(expected = IdentityBrokerException.class)
	public void getFederatedIdentity_responseJSON_nullTokenField() {
		TestProvider tested = getTested();
		Map<String, String> notes = new HashMap<>();
		tested.getFederatedIdentity(notes, "{\""
				+ AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_ACCESS_TOKEN + "\" : null}");
	}

	@Test
	public void getFederatedIdentity_responseJSON() {
		TestProvider tested = getTested();
		Map<String, String> notes = new HashMap<>();
		FederatedIdentity fi = tested.getFederatedIdentity(notes, "{\""
				+ AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_ACCESS_TOKEN + "\" : \"458rt\"}");
		Assert.assertNotNull(fi);
		Assert.assertEquals("458rt", fi.getId());
	}

	@Test
	public void getFederatedIdentity_responseUrlLine() {
		TestProvider tested = getTested();
		Map<String, String> notes = new HashMap<>();
		FederatedIdentity fi = tested.getFederatedIdentity(notes, "cosi=sss&"
				+ AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_ACCESS_TOKEN + "=458rtf&kdesi=ss}");
		Assert.assertNotNull(fi);
		Assert.assertEquals("458rtf", fi.getId());
	}

	private TestProvider getTested() {
		IdentityProviderModel model = new IdentityProviderModel();
		OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig(model);
		return new TestProvider(config);
	}

	private static class TestProvider extends AbstractOAuth2IdentityProvider<OAuth2IdentityProviderConfig> {

		public TestProvider(OAuth2IdentityProviderConfig config) {
			super(config);
		}

		@Override
		protected String getDefaultScopes() {
			return "default";
		}

		protected FederatedIdentity doGetFederatedIdentity(String accessToken) {
			return new FederatedIdentity(accessToken);
		};

	};

}
