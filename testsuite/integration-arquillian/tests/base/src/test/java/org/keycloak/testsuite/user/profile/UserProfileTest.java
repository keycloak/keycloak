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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.user.profile.config.UPAttribute;
import org.keycloak.testsuite.user.profile.config.UPAttributeRequired;
import org.keycloak.testsuite.user.profile.config.UPConfig;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.util.JsonSerialization;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.validators.EmailValidator;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserProfileTest extends AbstractUserProfileTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setClientScopes(Collections.singletonList(ClientScopeBuilder.create().name("customer").protocol("openid-connect").build()));
        ClientRepresentation client = KeycloakModelUtils.createClient(testRealm, "client-a");
        client.setDefaultClientScopes(Collections.singletonList("customer"));
    }

    @After
    public void onAfter() {
        getTestingClient().server().run((RunOnServer) UserProfileTest::resetConfiguration);
    }

    private static void resetConfiguration(KeycloakSession session) {
        configureSessionRealm(session);
        getDynamicUserProfileProvider(session).setConfiguration(null);
    }

    @Test
    public void testIdempotentProfile() {
        getTestingClient().server().run((RunOnServer) UserProfileTest::testIdempotentProfile);
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
        getTestingClient().server().run((RunOnServer) UserProfileTest::testCustomAttributeInAnyContext);
    }

    private static void testCustomAttributeInAnyContext(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "profiled-user");

        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration("{\"attributes\": [{\"name\": \"address\", \"required\": {}}]}");

        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        try {
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // address is mandatory
            assertTrue(ve.isAttributeOnError("address"));
        }

        assertThat(profile.getAttributes().nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, "address"));

        attributes.put("address", "myaddress");

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        profile.validate();
    }

    @Test
    public void testResolveProfile() {
        getTestingClient().server().run((RunOnServer) UserProfileTest::testResolveProfile);
    }

    private static void testResolveProfile(KeycloakSession session) {
        configureAuthenticationSession(session);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "profiled-user");

        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration("{\"attributes\": [{\"name\": \"business.address\", \"required\": {\"scopes\": [\"customer\"]}}]}");

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
        getTestingClient().server().run((RunOnServer) UserProfileTest::failValidationWhenEmptyAttributes);
        getTestingClient().server().run((RunOnServer) UserProfileTest::testAttributeValidation);
    }

    private static void failValidationWhenEmptyAttributes(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
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
        getTestingClient().server().run((RunOnServer) UserProfileTest::testValidateComplianceWithUserProfile);
    }

    private static void testValidateComplianceWithUserProfile(KeycloakSession session) throws IOException {
        RealmModel realm = configureSessionRealm(session);
        UserModel user = session.users().addUser(realm, "profiled-user");
        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        UPConfig config = new UPConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName("address");

        UPAttributeRequired requirements = new UPAttributeRequired();

        attribute.setRequired(requirements);

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
        getTestingClient().server().run((RunOnServer) UserProfileTest::testGetProfileAttributes);
    }

    private static void testGetProfileAttributes(KeycloakSession session) {
        RealmModel realm = configureSessionRealm(session);
        UserModel user = session.users().addUser(realm, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration("{\"attributes\": [{\"name\": \"address\", \"required\": {}}]}");

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
        getTestingClient().server().run((RunOnServer) UserProfileTest::testCreateAndUpdateUser);
    }

    private static void testCreateAndUpdateUser(KeycloakSession session) {
        UserProfileProvider provider = getDynamicUserProfileProvider(session);
        Map<String, Object> attributes = new HashMap<>();
        String userName = org.keycloak.models.utils.KeycloakModelUtils.generateId();

        attributes.put(UserModel.USERNAME, userName);
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
        getTestingClient().server().run((RunOnServer) UserProfileTest::testReadonlyUpdates);
    }

    private static void testReadonlyUpdates(KeycloakSession session) {
        configureSessionRealm(session);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put("address", Arrays.asList("fixed-address"));
        attributes.put("department", Arrays.asList("sales"));

        UserProfileProvider provider = getDynamicUserProfileProvider(session);

        provider.setConfiguration("{\"attributes\": [{\"name\": \"department\", \"permissions\": {\"edit\": [\"admin\"]}}]}");

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        UserModel user = profile.create();

        assertThat(profile.getAttributes().nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, "address", "department"));

        assertNull(user.getFirstAttribute("department"));

        profile = provider.create(UserProfileContext.USER_API, attributes, user);

        Set<String> attributesUpdated = new HashSet<>();

        profile.update((attributeName, userModel) -> assertTrue(attributesUpdated.add(attributeName)));

        assertThat(attributesUpdated, containsInAnyOrder("department"));

        assertEquals("sales", user.getFirstAttribute("department"));

        attributes.put("department", "cannot-change");

        profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);

        profile.update();

        assertEquals("sales", user.getFirstAttribute("department"));

        assertTrue(profile.getAttributes().isReadOnly("department"));
    }
}
