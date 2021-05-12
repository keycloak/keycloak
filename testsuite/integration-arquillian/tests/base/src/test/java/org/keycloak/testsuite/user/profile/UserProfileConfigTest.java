/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.testsuite.user.profile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.user.profile.config.UPConfigUtils.ROLE_USER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.user.profile.config.DeclarativeUserProfileProvider;
import org.keycloak.testsuite.user.profile.config.UPAttribute;
import org.keycloak.testsuite.user.profile.config.UPAttributeRequired;
import org.keycloak.testsuite.user.profile.config.UPConfig;
import org.keycloak.testsuite.user.profile.config.UPConfigUtils;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.util.JsonSerialization;
import org.keycloak.validate.validators.LengthValidator;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserProfileConfigTest extends AbstractUserProfileTest {

	protected static final String ATT_ADDRESS = "address";

	@Override
	public void configureTestRealm(RealmRepresentation testRealm) {
		KeycloakModelUtils.createClient(testRealm, "client-a");
		KeycloakModelUtils.createClient(testRealm, "client-b");
	}

	@Test
	public void testConfigurationSetInvalid() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testConfigurationSetInvalid);
	}

	private static void testConfigurationSetInvalid(KeycloakSession session) {
		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);

		try {
			provider.setConfiguration("{\"validateConfigAttribute\": true}");
			fail("Should fail validation");
		} catch (ComponentValidationException ve) {
			// OK
		}

	}

	@Test
	public void testConfigurationGetSet() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testConfigurationGetSet);
	}

	private static void testConfigurationGetSet(KeycloakSession session) throws IOException {
		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
		ComponentModel component = provider.getComponentModel();

		assertNotNull(component);

		// generate big configuration to test slicing in the persistence/component config
		UPConfig config = new UPConfig();
		for (int i = 0; i < 80; i++) {
			UPAttribute attribute = new UPAttribute();
			attribute.setName(UserModel.USERNAME+i);
			Map<String, Object> validatorConfig = new HashMap<>();
			validatorConfig.put("min", 3);
			attribute.addValidation("length", validatorConfig);
			config.addAttribute(attribute);
		}
		String newConfig = JsonSerialization.writeValueAsString(config);

		provider.setConfiguration(newConfig);
		// assert config is persisted in 2 pieces
		Assert.assertEquals("2", component.get(DeclarativeUserProfileProvider.UP_PIECES_COUNT_COMPONENT_CONFIG_KEY));
		// assert config is returned correctly 
		Assert.assertEquals(newConfig, provider.getConfiguration());
	}

	@Test
	public void testConfigurationGetSetDefault() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testConfigurationGetSetDefault);
	}

	private static void testConfigurationGetSetDefault(KeycloakSession session) throws IOException {
		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);

		provider.setConfiguration(null);

		Assert.assertNull(provider.getComponentModel().get(DeclarativeUserProfileProvider.UP_PIECES_COUNT_COMPONENT_CONFIG_KEY));

		ComponentModel component = provider.getComponentModel();

		assertNotNull(component);

		Assert.assertTrue(component.getConfig().isEmpty());
	}

	@Test
	public void testDefaultConfigForUpdateProfile() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testDefaultConfigForUpdateProfile);
	}

	private static void testDefaultConfigForUpdateProfile(KeycloakSession session) throws IOException {
		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);

		// reset configuration to default
		provider.setConfiguration(null);

		// failed required validations
		UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, Collections.emptyMap());

		try {
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(UserModel.USERNAME));
		}

		// failed for blank values also
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(UserModel.FIRST_NAME, "");
		attributes.put(UserModel.LAST_NAME, " ");
		attributes.put(UserModel.EMAIL, "");

		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

		try {
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(UserModel.USERNAME));
			assertTrue(ve.isAttributeOnError(UserModel.FIRST_NAME));
			assertTrue(ve.isAttributeOnError(UserModel.LAST_NAME));
			assertTrue(ve.isAttributeOnError(UserModel.EMAIL));
		}

		// all OK
		attributes.put(UserModel.USERNAME, "jdoeusername");
		attributes.put(UserModel.FIRST_NAME, "John");
		attributes.put(UserModel.LAST_NAME, "Doe");
		attributes.put(UserModel.EMAIL, "jdoe@acme.org");

		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		profile.validate();
	}

	@Test
	public void testAdditionalValidationForUsername() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testAdditionalValidationForUsername);
	}

	private static void testAdditionalValidationForUsername(KeycloakSession session) throws IOException {
		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
		ComponentModel component = provider.getComponentModel();

		assertNotNull(component);

		UPConfig config = new UPConfig();
		UPAttribute attribute = new UPAttribute();

		attribute.setName(UserModel.USERNAME);

		Map<String, Object> validatorConfig = new HashMap<>();

		validatorConfig.put("min", 4);

		attribute.addValidation(LengthValidator.ID, validatorConfig);

		config.addAttribute(attribute);

		provider.setConfiguration(JsonSerialization.writeValueAsString(config));

		Map<String, Object> attributes = new HashMap<>();

		attributes.put(UserModel.USERNAME, "us");

		UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

		try {
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(UserModel.USERNAME));
			assertTrue(ve.hasError(LengthValidator.MESSAGE_INVALID_LENGTH));
		}

		attributes.put(UserModel.USERNAME, "user");

		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

		profile.validate();

		provider.setConfiguration(null);

		attributes.put(UserModel.USERNAME, "us");

		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

		profile.validate();
	}
	
	@Test
	public void testFirstLastNameCanBeOptional() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testFirstLastNameCanBeOptional);
	}
	
	private static void testFirstLastNameCanBeOptional(KeycloakSession session) throws IOException {

		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
		ComponentModel component = provider.getComponentModel();

		assertNotNull(component);

		UPConfig config = new UPConfig();
		UPAttribute attribute = new UPAttribute();
		attribute.setName(UserModel.FIRST_NAME);
		Map<String, Object> validatorConfig = new HashMap<>();
		validatorConfig.put(LengthValidator.KEY_MAX, 4);
		attribute.addValidation(LengthValidator.ID, validatorConfig);
		config.addAttribute(attribute);
		
		attribute = new UPAttribute();
		attribute.setName(UserModel.LAST_NAME);
		attribute.addValidation(LengthValidator.ID, validatorConfig);
		config.addAttribute(attribute);
		
		provider.setConfiguration(JsonSerialization.writeValueAsString(config));
		
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(UserModel.USERNAME, "user");

		// not present attributes are OK
		UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		profile.validate();
		
		//empty attributes are OK
		attributes.put(UserModel.FIRST_NAME, "");
		attributes.put(UserModel.LAST_NAME, "");
		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		profile.validate();

		//filled attributes are OK
		attributes.put(UserModel.FIRST_NAME, "John");
		attributes.put(UserModel.LAST_NAME, "Doe");
		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		profile.validate();

		// fails due to additional length validation so it is executed correctly
		attributes.put(UserModel.FIRST_NAME, "JohnTooLong");
		attributes.put(UserModel.LAST_NAME, "DoeTooLong");		
		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		try {
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(UserModel.FIRST_NAME));
			assertTrue(ve.isAttributeOnError(UserModel.LAST_NAME));
		}
	}

	@Test
	public void testCustomAttribute_Required() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testCustomAttribute_Required);
	}

	private static void testCustomAttribute_Required(KeycloakSession session) throws IOException {
		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
		ComponentModel component = provider.getComponentModel();

		assertNotNull(component);

		UPConfig config = new UPConfig();
		UPAttribute attribute = new UPAttribute();

		attribute.setName(ATT_ADDRESS);

		Map<String, Object> validatorConfig = new HashMap<>();

		validatorConfig.put(LengthValidator.KEY_MIN, 4);

		attribute.addValidation(LengthValidator.ID, validatorConfig);

		// make it ALWAYS required
		UPAttributeRequired requirements = new UPAttributeRequired();
		attribute.setRequired(requirements);

		config.addAttribute(attribute);

		provider.setConfiguration(JsonSerialization.writeValueAsString(config));

		Map<String, Object> attributes = new HashMap<>();

		attributes.put(UserModel.USERNAME, "user");

		UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

		// fails on required validation
		try {
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}

		// fails on length validation
		attributes.put(ATT_ADDRESS, "adr");
		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		try {
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}

		// all OK
		attributes.put(ATT_ADDRESS, "adress ok");
		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		profile.validate();
	}
	
	@Test
    public void testCustomAttribute_Optional() {
        getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testCustomAttribute_Optional);
    }

    private static void testCustomAttribute_Optional(KeycloakSession session) throws IOException {
        configureSessionRealm(session);
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
        ComponentModel component = provider.getComponentModel();

        assertNotNull(component);

        UPConfig config = new UPConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName(ATT_ADDRESS);

        Map<String, Object> validatorConfig = new HashMap<>();
        validatorConfig.put(LengthValidator.KEY_MIN, 4);
        attribute.addValidation(LengthValidator.ID, validatorConfig);

        config.addAttribute(attribute);

        provider.setConfiguration(JsonSerialization.writeValueAsString(config));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(UserModel.USERNAME, "user");

        // null is OK as attribute is optional
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();

        //blank String have to be OK as it is what UI forms send for not filled in optional attributes
        attributes.put(ATT_ADDRESS, "");
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();
        
        // fails on length validation
        attributes.put(ATT_ADDRESS, "adr");
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        try {
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
        }

        // all OK
        attributes.put(ATT_ADDRESS, "adress ok");
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();

    }

	@Test
	public void testRequiredByUserRole_USER() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testRequiredByUserRole_USER);
	}

	private static void testRequiredByUserRole_USER(KeycloakSession session) throws IOException {
		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
		ComponentModel component = provider.getComponentModel();

		assertNotNull(component);

		UPConfig config = new UPConfig();
		UPAttribute attribute = new UPAttribute();

		attribute.setName(ATT_ADDRESS);

		UPAttributeRequired requirements = new UPAttributeRequired();

		List<String> roles = new ArrayList<>();
		roles.add(ROLE_USER);
		requirements.setRoles(roles);

		attribute.setRequired(requirements);

		config.addAttribute(attribute);

		provider.setConfiguration(JsonSerialization.writeValueAsString(config));

		Map<String, Object> attributes = new HashMap<>();

		attributes.put(UserModel.USERNAME, "user");

		// fail on common contexts
		UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		try {
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}

		profile = provider.create(UserProfileContext.ACCOUNT, attributes);
		try {
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}

		profile = provider.create(UserProfileContext.REGISTRATION_PROFILE, attributes);
		try {
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}

		// no fail on User API
		profile = provider.create(UserProfileContext.USER_API, attributes);
		profile.validate();
	}

	@Test
	public void testRequiredByUserRole_ADMIN() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testRequiredByUserRole_ADMIN);
	}

	private static void testRequiredByUserRole_ADMIN(KeycloakSession session) throws IOException {
		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
		ComponentModel component = provider.getComponentModel();

		assertNotNull(component);

		UPConfig config = new UPConfig();
		UPAttribute attribute = new UPAttribute();

		attribute.setName(ATT_ADDRESS);

		UPAttributeRequired requirements = new UPAttributeRequired();

		List<String> roles = new ArrayList<>();
		roles.add(UPConfigUtils.ROLE_ADMIN);
		requirements.setRoles(roles);

		attribute.setRequired(requirements);

		config.addAttribute(attribute);

		provider.setConfiguration(JsonSerialization.writeValueAsString(config));

		Map<String, Object> attributes = new HashMap<>();

		attributes.put(UserModel.USERNAME, "user");

		// NO fail on common contexts
		UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		profile.validate();

		profile = provider.create(UserProfileContext.ACCOUNT, attributes);
		profile.validate();

		profile = provider.create(UserProfileContext.REGISTRATION_PROFILE, attributes);
		profile.validate();

		// fail on User API
		try {
			profile = provider.create(UserProfileContext.USER_API, attributes);
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}

	}

	@Test
	public void testRequiredByScope_clientDefaultScope() {
		getTestingClient().server().run((RunOnServer) UserProfileConfigTest::testRequiredByScope_clientDefaultScope);
	}

	private static void testRequiredByScope_clientDefaultScope(KeycloakSession session) throws IOException {
		configureSessionRealm(session);
		DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
		ComponentModel component = provider.getComponentModel();

		assertNotNull(component);

		UPConfig config = new UPConfig();
		UPAttribute attribute = new UPAttribute();

		attribute.setName(ATT_ADDRESS);

		UPAttributeRequired requirements = new UPAttributeRequired();

		List<String> scopes = new ArrayList<>();
		scopes.add("client-a");
		requirements.setScopes(scopes);

		attribute.setRequired(requirements);

		config.addAttribute(attribute);

		provider.setConfiguration(JsonSerialization.writeValueAsString(config));

		Map<String, Object> attributes = new HashMap<>();

		attributes.put(UserModel.USERNAME, "user");

		// client with default scopes for which is attribute NOT configured as required
		configureAuthenticationSession(session, "client-b", null);

		// no fail on User API nor Account console as they do not have scopes
		UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
		profile.validate();
		profile = provider.create(UserProfileContext.ACCOUNT, attributes);
		profile.validate();
		profile = provider.create(UserProfileContext.ACCOUNT_OLD, attributes);
		profile.validate();

		// no fail on auth flow scopes when scope is not required
		profile = provider.create(UserProfileContext.REGISTRATION_PROFILE, attributes);
		profile.validate();
		profile = provider.create(UserProfileContext.REGISTRATION_USER_CREATION, attributes);
		profile.validate();
		profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
		profile.validate();
		profile = provider.create(UserProfileContext.IDP_REVIEW, attributes);
		profile.validate();

		// client with default scope for which is attribute configured as required
		configureAuthenticationSession(session, "client-a", null);

		// no fail on User API nor Account console as they do not have scopes
		profile = provider.create(UserProfileContext.USER_API, attributes);
		profile.validate();
		profile = provider.create(UserProfileContext.ACCOUNT, attributes);
		profile.validate();
		profile = provider.create(UserProfileContext.ACCOUNT_OLD, attributes);
		profile.validate();

		// fail on auth flow scopes when scope is required
		try {
			profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}
		try {
			profile = provider.create(UserProfileContext.REGISTRATION_PROFILE, attributes);
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}
		try {
			profile = provider.create(UserProfileContext.REGISTRATION_USER_CREATION, attributes);
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}
		try {
			profile = provider.create(UserProfileContext.IDP_REVIEW, attributes);
			profile.validate();
			fail("Should fail validation");
		} catch (ValidationException ve) {
			assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
		}

	}

}
