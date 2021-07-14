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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.config.UPAttribute;
import org.keycloak.userprofile.config.UPAttributePermissions;
import org.keycloak.userprofile.config.UPAttributeRequired;
import org.keycloak.userprofile.config.UPAttributeSelector;
import org.keycloak.userprofile.config.UPConfig;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.validators.EmailValidator;
import org.keycloak.validate.validators.LengthValidator;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class UserProfileTest extends AbstractUserProfileTest {

    protected static final String ATT_ADDRESS = "address";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        testRealm.setClientScopes(new ArrayList<>());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("customer").protocol("openid-connect").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("client-a").protocol("openid-connect").build());
        ClientRepresentation client = KeycloakModelUtils.createClient(testRealm, "client-a");
        client.setDefaultClientScopes(Collections.singletonList("customer"));
        KeycloakModelUtils.createClient(testRealm, "client-b");
    }

    @Test
    public void testIdempotentProfile() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testIdempotentProfile);
    }

    private static void testIdempotentProfile(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        attributes.put(UserModel.USERNAME, "profiled-user");

        // once created, profile attributes can not be changed
        assertTrue(profile.getAttributes().contains(UserModel.USERNAME));
        assertNull(profile.getAttributes().getFirstValue(UserModel.USERNAME));
    }

    @Test
    public void testCustomAttributeInAnyContext() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testCustomAttributeInAnyContext);
    }

    private static void testCustomAttributeInAnyContext(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "profiled-user");

        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration("{\"attributes\": [{\"name\": \"address\", \"required\": {}, \"permissions\": {\"edit\": [\"user\"]}}]}");

        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        try {
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // address is mandatory
            assertTrue(ve.isAttributeOnError("address"));
        }

        assertThat(profile.getAttributes().nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, "address"));

        attributes.put("address", "myaddress");

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        profile.validate();
    }

    @Test
    public void testResolveProfile() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testResolveProfile);
    }

    private static void testResolveProfile(KeycloakSession session) {
        configureAuthenticationSession(session);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "profiled-user");

        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration("{\"attributes\": [{\"name\": \"business.address\", \"required\": {\"scopes\": [\"customer\"]}, \"permissions\": {\"edit\": [\"user\"]}}]}");

        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        profile.getAttributes();

        try {
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // address is mandatory
            assertTrue(ve.isAttributeOnError("business.address"));
        }

        attributes.put("business.address", "valid-address");
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();

        profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        profile.validate();
    }

    @Test
    public void testValidation() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::failValidationWhenEmptyAttributes);
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testAttributeValidation);
    }

    private static void failValidationWhenEmptyAttributes(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        provider.setConfiguration(null);
        UserProfile profile;

        try {
            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // username is mandatory
            assertTrue(ve.isAttributeOnError(UserModel.USERNAME));
        }

        RealmModel realm = session.getContext().getRealm();

        try {
            attributes.clear();
            attributes.put(UserModel.EMAIL, "profile-user@keycloak.org");
            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // username is mandatory
            assertTrue(ve.isAttributeOnError(UserModel.USERNAME));
        }

        try {
            realm.setRegistrationEmailAsUsername(true);
            attributes.clear();
            attributes.put(UserModel.FIRST_NAME, "Joe");
            attributes.put(UserModel.LAST_NAME, "Doe");
            attributes.put(UserModel.EMAIL, "profile-user@keycloak.org");
            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            profile.validate();
        } catch (ValidationException ve) {
            Assert.fail("Should be OK email as username");
        } finally {
            // we should probably avoid this kind of logic and make the test reset the realm to original state
            realm.setRegistrationEmailAsUsername(false);
        }

        attributes.clear();
        attributes.put(UserModel.USERNAME, "profile-user");
        attributes.put(UserModel.FIRST_NAME, "Joe");
        attributes.put(UserModel.LAST_NAME, "Doe");
        provider.create(UserProfileContext.UPDATE_PROFILE, attributes).validate();
    }

    private static void testAttributeValidation(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);

        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        List<ValidationError> errors = new ArrayList<>();

        assertFalse(profile.getAttributes().validate(UserModel.USERNAME, (Consumer<ValidationError>) errors::add));
        assertTrue(containsErrorMessage(errors, Messages.MISSING_USERNAME));

        errors.clear();
        attributes.clear();
        attributes.put(UserModel.EMAIL, "invalid");
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        assertFalse(profile.getAttributes().validate(UserModel.EMAIL, (Consumer<ValidationError>) errors::add));
        assertTrue(containsErrorMessage(errors, EmailValidator.MESSAGE_INVALID_EMAIL));
    }
    
    private static boolean containsErrorMessage(List<ValidationError> errors, String message){
    	for(ValidationError err : errors) {
    		if(err.getMessage().equals(message)) {
    			return true;
    		}
    	}
    	return false;
    }
    

    @Test
    public void testValidateComplianceWithUserProfile() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testValidateComplianceWithUserProfile);
    }

    private static void testValidateComplianceWithUserProfile(KeycloakSession session) throws IOException {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().addUser(realm, "profiled-user");
        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        UPConfig config = new UPConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName("address");

        UPAttributeRequired requirements = new UPAttributeRequired();

        attribute.setRequired(requirements);

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(Collections.singleton(ROLE_USER));
        attribute.setPermissions(permissions);

        config.addAttribute(attribute);

        provider.setConfiguration(JsonSerialization.writeValueAsString(config));

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, user);

        try {
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // username is mandatory
            assertTrue(ve.isAttributeOnError("address"));
        }

        user.setAttribute("address", Arrays.asList("fixed-address"));

        profile = provider.create(UserProfileContext.ACCOUNT, user);

        profile.validate();
    }

    @Test
    public void testGetProfileAttributes() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testGetProfileAttributes);
    }

    private static void testGetProfileAttributes(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().addUser(realm, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration("{\"attributes\": [{\"name\": \"address\", \"required\": {}, \"permissions\": {\"edit\": [\"user\"]}}]}");

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, user);
        Attributes attributes = profile.getAttributes();

        assertThat(attributes.nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, "address"));

        try {
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // username is mandatory
            assertTrue(ve.isAttributeOnError("address"));
        }

        assertNotNull(attributes.getFirstValue(UserModel.USERNAME));
        assertNull(attributes.getFirstValue(UserModel.EMAIL));
        assertNull(attributes.getFirstValue(UserModel.FIRST_NAME));
        assertNull(attributes.getFirstValue(UserModel.LAST_NAME));
        assertNull(attributes.getFirstValue("address"));

        user.setAttribute("address", Arrays.asList("fixed-address"));

        profile = provider.create(UserProfileContext.ACCOUNT, user);
        attributes = profile.getAttributes();

        profile.validate();

        assertNotNull(attributes.getFirstValue("address"));
    }

    @Test
    public void testCreateAndUpdateUser() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testCreateAndUpdateUser);
    }

    private static void testCreateAndUpdateUser(KeycloakSession session) throws IOException {
        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        UPConfig config = JsonSerialization.readValue(provider.getConfiguration(), UPConfig.class);
        UPAttribute attribute = new UPAttribute();
        attribute.setName("address");
        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(new HashSet<>(Arrays.asList("admin", "user")));
        attribute.setPermissions(permissions);
        config.addAttribute(attribute);

        attribute = new UPAttribute();
        attribute.setName("business.address");
        permissions = new UPAttributePermissions();
        permissions.setEdit(new HashSet<>(Arrays.asList("admin", "user")));
        attribute.setPermissions(permissions);
        config.addAttribute(attribute);

        provider.setConfiguration(JsonSerialization.writeValueAsString(config));

        Map<String, Object> attributes = new HashMap<>();
        String userName = org.keycloak.models.utils.KeycloakModelUtils.generateId();

        attributes.put(UserModel.USERNAME, userName);
        attributes.put(UserModel.FIRST_NAME, "Joe");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put("address", "fixed-address");

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        UserModel user = profile.create();

        assertEquals(userName, user.getUsername());
        assertEquals("fixed-address", user.getFirstAttribute("address"));

        attributes.put(UserModel.FIRST_NAME, "Alice");
        attributes.put(UserModel.LAST_NAME, "In Chains");
        attributes.put(UserModel.EMAIL, "alice@keycloak.org");

        profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);
        Set<String> attributesUpdated = new HashSet<>();

        profile.update((attributeName, userModel) -> assertTrue(attributesUpdated.add(attributeName)));

        assertThat(attributesUpdated, containsInAnyOrder(UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.EMAIL));

        configureAuthenticationSession(session);

        attributes.put("business.address", "fixed-business-address");
        profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);

        attributesUpdated.clear();
        profile.update((attributeName, userModel) -> assertTrue(attributesUpdated.add(attributeName)));

        assertThat(attributesUpdated, containsInAnyOrder("business.address"));

        assertEquals("fixed-business-address", user.getFirstAttribute("business.address"));
    }

    @Test
    public void testReadonlyUpdates() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testReadonlyUpdates);
    }

    private static void testReadonlyUpdates(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put("address", Arrays.asList("fixed-address"));
        attributes.put("department", Arrays.asList("sales"));

        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration("{\"attributes\": [{\"name\": \"department\", \"permissions\": {\"edit\": [\"admin\"]}}]}");

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        UserModel user = profile.create();

        assertThat(profile.getAttributes().nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, "address", "department"));

        assertNull(user.getFirstAttribute("department"));

        profile = provider.create(UserProfileContext.USER_API, attributes, user);

        Set<String> attributesUpdated = new HashSet<>();

        profile.update((attributeName, userModel) -> assertTrue(attributesUpdated.add(attributeName)));

        assertThat(attributesUpdated, containsInAnyOrder("department"));

        assertEquals("sales", user.getFirstAttribute("department"));

        attributes.put("department", "cannot-change");

        profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);

        try {
            profile.update();
            fail("Should fail due to read only attribute");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError("department"));
        }

        assertEquals("sales", user.getFirstAttribute("department"));

        assertTrue(profile.getAttributes().isReadOnly("department"));
    }

    @Test
    public void testDoNotUpdateUndefinedAttributes() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testDoNotUpdateUndefinedAttributes);
    }

    private static void testDoNotUpdateUndefinedAttributes(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put("address", Arrays.asList("fixed-address"));
        attributes.put("department", Arrays.asList("sales"));
        attributes.put("phone", Arrays.asList("fixed-phone"));

        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration("{\"attributes\": [{\"name\": \"department\", \"permissions\": {\"edit\": [\"admin\"]}},"
                + "{\"name\": \"phone\", \"permissions\": {\"edit\": [\"admin\"]}},"
                + "{\"name\": \"address\", \"permissions\": {\"edit\": [\"admin\"]}}]}");

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        UserModel user = profile.create();

        assertThat(profile.getAttributes().nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, "address", "department", "phone"));

        profile = provider.create(UserProfileContext.USER_API, attributes, user);

        Set<String> attributesUpdated = new HashSet<>();

        profile.update((attributeName, userModel) -> assertTrue(attributesUpdated.add(attributeName)));
        assertThat(attributesUpdated, containsInAnyOrder("department", "address", "phone"));

        provider.setConfiguration("{\"attributes\": [{\"name\": \"department\", \"permissions\": {\"edit\": [\"admin\"]}},"
                + "{\"name\": \"phone\", \"permissions\": {\"edit\": [\"admin\"]}}]}");
        attributesUpdated.clear();
        attributes.remove("address");
        attributes.put("department", "foo");
        attributes.put("phone", "foo");
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update((attributeName, userModel) -> assertTrue(attributesUpdated.add(attributeName)));
        assertThat(attributesUpdated, containsInAnyOrder("department", "phone"));
        assertTrue(user.getAttributes().containsKey("address"));

        provider.setConfiguration("{\"attributes\": [{\"name\": \"department\", \"permissions\": {\"edit\": [\"admin\"]}},"
                + "{\"name\": \"phone\", \"permissions\": {\"edit\": [\"admin\"]}},"
                + "{\"name\": \"address\", \"permissions\": {\"edit\": [\"admin\"]}}]}");
        attributes.put("department", "foo");
        attributes.put("phone", "foo");
        attributes.put("address", "bar");
        attributesUpdated.clear();
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update((attributeName, userModel) -> assertTrue(attributesUpdated.add(attributeName)));
        assertThat(attributesUpdated, containsInAnyOrder("address"));
        assertEquals("bar", user.getFirstAttribute("address"));
        assertEquals("foo", user.getFirstAttribute("phone"));
        assertEquals("foo", user.getFirstAttribute("department"));

        attributes.remove("address");
        attributesUpdated.clear();
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update((attributeName, userModel) -> assertTrue(attributesUpdated.add(attributeName)));
        assertThat(attributesUpdated, containsInAnyOrder("address"));
        assertFalse(user.getAttributes().containsKey("address"));
        assertTrue(user.getAttributes().containsKey("phone"));
        assertTrue(user.getAttributes().containsKey("department"));

        String prefixedAttributeName = Constants.USER_ATTRIBUTES_PREFIX.concat("prefixed");
        attributes.put(prefixedAttributeName, "foo");
        attributesUpdated.clear();
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update((attributeName, userModel) -> assertTrue(attributesUpdated.add(attributeName)));
        assertTrue(attributesUpdated.isEmpty());
        assertFalse(user.getAttributes().containsKey("prefixedAttributeName"));
    }

    @Test
    public void testInvalidConfiguration() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testInvalidConfiguration);
    }

    private static void testInvalidConfiguration(KeycloakSession session) {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);

        try {
            provider.setConfiguration("{\"validateConfigAttribute\": true}");
            fail("Should fail validation");
        } catch (ComponentValidationException ve) {
            // OK
        }

    }

    @Test
    public void testConfigurationChunks() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testConfigurationChunks);
    }

    private static void testConfigurationChunks(KeycloakSession session) throws IOException {
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

        component = provider.getComponentModel();

        // assert config is persisted in 2 pieces
        Assert.assertEquals("2", component.get(DeclarativeUserProfileProvider.UP_PIECES_COUNT_COMPONENT_CONFIG_KEY));
        // assert config is returned correctly
        Assert.assertEquals(newConfig, provider.getConfiguration());
    }

    @Test
    public void testResetConfiguration() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testResetConfiguration);
    }

    private static void testResetConfiguration(KeycloakSession session) throws IOException {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration(null);

        Assert.assertNull(provider.getComponentModel().get(DeclarativeUserProfileProvider.UP_PIECES_COUNT_COMPONENT_CONFIG_KEY));

        ComponentModel component = provider.getComponentModel();

        assertNotNull(component);

        Assert.assertTrue(component.getConfig().isEmpty());
    }

    @Test
    public void testDefaultConfig() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testDefaultConfig);
    }

    private static void testDefaultConfig(KeycloakSession session) {
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
    public void testCustomValidationForUsername() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testCustomValidationForUsername);
    }

    private static void testCustomValidationForUsername(KeycloakSession session) throws IOException {
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
        attributes.put(UserModel.FIRST_NAME, "Joe");
        attributes.put(UserModel.LAST_NAME, "Doe");

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        profile.validate();
    }

    @Test
    public void testOptionalAttributes() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testOptionalAttributes);
    }

    private static void testOptionalAttributes(KeycloakSession session) throws IOException {
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
    public void testCustomAttributeRequired() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testCustomAttributeRequired);
    }

    private static void testCustomAttributeRequired(KeycloakSession session) throws IOException {
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

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(Collections.singleton(ROLE_USER));
        attribute.setPermissions(permissions);

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
        attributes.put(UserModel.FIRST_NAME, "Joe");
        attributes.put(UserModel.LAST_NAME, "Doe");

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();
    }

    @Test
    public void testCustomAttributeOptional() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testCustomAttributeOptional);
    }

    private static void testCustomAttributeOptional(KeycloakSession session) throws IOException {
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
    public void testRequiredIfUser() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testRequiredIfUser);
    }

    private static void testRequiredIfUser(KeycloakSession session) throws IOException {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
        ComponentModel component = provider.getComponentModel();

        assertNotNull(component);

        UPConfig config = new UPConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName(ATT_ADDRESS);

        UPAttributeRequired requirements = new UPAttributeRequired();

        requirements.setRoles(Collections.singleton(ROLE_USER));

        attribute.setRequired(requirements);

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(Collections.singleton(ROLE_USER));
        attribute.setPermissions(permissions);

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

        attributes.put(UserModel.FIRST_NAME, "Joe");
        attributes.put(UserModel.LAST_NAME, "Doe");

        // no fail on User API
        profile = provider.create(UserProfileContext.USER_API, attributes);
        profile.validate();
    }

    @Test
    public void testRequiredIfAdmin() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testRequiredIfAdmin);
    }

    private static void testRequiredIfAdmin(KeycloakSession session) throws IOException {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
        ComponentModel component = provider.getComponentModel();

        assertNotNull(component);

        UPConfig config = new UPConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName(ATT_ADDRESS);

        UPAttributeRequired requirements = new UPAttributeRequired();

        requirements.setRoles(Collections.singleton(ROLE_ADMIN));

        attribute.setRequired(requirements);

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(Collections.singleton(UPConfigUtils.ROLE_ADMIN));
        attribute.setPermissions(permissions);

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
    public void testNoValidationsIfUserReadOnly() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testNoValidationsIfUserReadOnly);
    }

    private static void testNoValidationsIfUserReadOnly(KeycloakSession session) throws IOException {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
        ComponentModel component = provider.getComponentModel();

        assertNotNull(component);

        UPConfig config = new UPConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName(ATT_ADDRESS);

        UPAttributeRequired requirements = new UPAttributeRequired();
        attribute.setRequired(requirements);

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(Collections.singleton(UPConfigUtils.ROLE_ADMIN));
        attribute.setPermissions(permissions);

        config.addAttribute(attribute);

        provider.setConfiguration(JsonSerialization.writeValueAsString(config));

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.FIRST_NAME, "user");
        attributes.put(UserModel.LAST_NAME, "user");

        // NO fail on USER contexts
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();

        // Fails on ADMIN context - User REST API
        try {
            profile = provider.create(UserProfileContext.USER_API, attributes);
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
        }

    }

    @Test
    public void testNoValidationsIfAdminReadOnly() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testNoValidationsIfAdminReadOnly);
    }

    private static void testNoValidationsIfAdminReadOnly(KeycloakSession session) throws IOException {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
        ComponentModel component = provider.getComponentModel();

        assertNotNull(component);

        UPConfig config = new UPConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName(ATT_ADDRESS);

        UPAttributeRequired requirements = new UPAttributeRequired();
        attribute.setRequired(requirements);

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(Collections.singleton(UPConfigUtils.ROLE_USER));
        attribute.setPermissions(permissions);

        config.addAttribute(attribute);

        provider.setConfiguration(JsonSerialization.writeValueAsString(config));

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");

        // Fails on USER context
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        try {
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
        }

        // NO fail on ADMIN context - User REST API
        profile = provider.create(UserProfileContext.USER_API, attributes);
        profile.validate();
    }

    @Test
    public void testRequiredByClientScope() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testRequiredByClientScope);
    }

    private static void testRequiredByClientScope(KeycloakSession session) throws IOException {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
        ComponentModel component = provider.getComponentModel();

        assertNotNull(component);

        UPConfig config = new UPConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName(ATT_ADDRESS);

        UPAttributeRequired requirements = new UPAttributeRequired();

        requirements.setScopes(Collections.singleton("client-a"));

        attribute.setRequired(requirements);

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(Collections.singleton("user"));
        attribute.setPermissions(permissions);

        config.addAttribute(attribute);

        provider.setConfiguration(JsonSerialization.writeValueAsString(config));

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.EMAIL, "user@email.test");

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
            profile = provider.create(UserProfileContext.IDP_REVIEW, attributes);
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
        }

    }

    @Test
    public void testConfigurationInvalidScope() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testConfigurationInvalidScope);
    }

    private static void testConfigurationInvalidScope(KeycloakSession session) throws IOException {
        RealmModel realm = session.getContext().getRealm();
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
        ComponentModel component = provider.getComponentModel();

        assertNotNull(component);

        UPConfig config = new UPConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName(ATT_ADDRESS);

        UPAttributeRequired requirements = new UPAttributeRequired();

        requirements.setScopes(Collections.singleton("invalid"));

        attribute.setRequired(requirements);

        attribute.setSelector(new UPAttributeSelector());
        attribute.getSelector().setScopes(Collections.singleton("invalid"));

        config.addAttribute(attribute);

        try {
            provider.setConfiguration(JsonSerialization.writeValueAsString(config));
            Assert.fail("Expected to fail due to invalid client scope");
        } catch (ComponentValidationException cve) {
            //ignore
        }
    }
}
