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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPAttributeSelector;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.services.messages.Messages;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.userprofile.AttributeGroupMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileConstants;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.UserProfileUtil;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.userprofile.validator.MultiValueValidator;
import org.keycloak.userprofile.validator.PersonNameProhibitedCharactersValidator;
import org.keycloak.userprofile.validator.UsernameIDNHomographValidator;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.validators.EmailValidator;
import org.keycloak.validate.validators.LengthValidator;
import org.keycloak.validate.validators.UriValidator;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import static java.util.Optional.ofNullable;

import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;
import static org.keycloak.userprofile.config.UPConfigUtils.parseSystemDefaultConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserProfileTest extends AbstractUserProfileTest {

    protected static final String ATT_ADDRESS = "address";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        testRealm.setClientScopes(new ArrayList<>());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("customer").protocol("openid-connect").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("client-a").protocol("openid-connect").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("some-optional-scope").protocol("openid-connect").build());
        ClientRepresentation client = KeycloakModelUtils.createClient(testRealm, "client-a");
        client.setDefaultClientScopes(List.of("customer"));
        client.setOptionalClientScopes(Collections.singletonList("some-optional-scope"));
        KeycloakModelUtils.createClient(testRealm, "client-b");
    }

    @Test
    public void testIdempotentProfile() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testIdempotentProfile);
    }

    @Test
    public void testReadOnlyAllowed() throws Exception {
        // create a user with attribute foo value 123 allowed by the profile now but disallowed later
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("foo", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN))));
        config.getAttribute(UserModel.EMAIL).setPermissions(new UPAttributePermissions(Set.of(ROLE_USER), Set.of(ROLE_ADMIN)));
        RealmResource realmRes = testRealm();
        realmRes.users().userProfile().update(config);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("profiled-user-foo-ro");
        userRep.setFirstName("John");
        userRep.setLastName("Doe");
        userRep.setEmail(org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        userRep.setAttributes(Map.of("foo", Collections.singletonList("123")));
        Response response = realmRes.users().create(userRep);
        final String userId = ApiUtil.getCreatedId(response);
        userRep = realmRes.users().get(userId).toRepresentation();
        Assert.assertEquals(Collections.singletonList("123"), userRep.getAttributes().get("foo"));

        // it should work if foo is read-only in the context
        getTestingClient().server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, userId);
            UserProfileProvider provider = getUserProfileProvider(session);
            UPConfig upConfig = provider.getConfiguration();
            upConfig.getAttribute("foo")
                    .setValidations(Map.of("length", Map.of("min", "5", "max", "15")));
            provider.setConfiguration(upConfig);
            Map<String, List<String>> attributes = new HashMap<>(user.getAttributes());
            UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
            profile.validate();
        });

        // it should work if foo is read-only in the context
        getTestingClient().server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, userId);
            user.setEmail(null);
            UserProfileProvider provider = getUserProfileProvider(session);
            Map<String, Object> attributes = new HashMap<>(user.getAttributes());
            attributes.put("email", "");
            UserProfile profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);
            profile.validate();
        });

        // it should fail if foo can be modified
        getTestingClient().server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, userId);
            UserProfileProvider provider = getUserProfileProvider(session);
            UPConfig upConfig = provider.getConfiguration();
            UPAttribute changedFoo = upConfig.getAttribute("foo");
            changedFoo.setPermissions(new UPAttributePermissions(Set.of(), Set.of(ROLE_USER, ROLE_ADMIN)));
            changedFoo.setValidations(Map.of("length", Map.of("min", "5", "max", "15")));
            provider.setConfiguration(upConfig);

            Map<String, List<String>> attributes = new HashMap<>(user.getAttributes());
            UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
            try {
                profile.validate();
                Assert.fail("Should fail validation on foo minimum length");
            } catch (ValidationException ve) {
                assertTrue(ve.isAttributeOnError("foo"));
                assertTrue(ve.hasError(LengthValidator.MESSAGE_INVALID_LENGTH));
            }
        });
    }

    private static void testIdempotentProfile(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        attributes.put(UserModel.USERNAME, "profiled-user");

        // once created, profile attributes can not be changed
        assertTrue(profile.getAttributes().contains(UserModel.USERNAME));
        assertNull(profile.getAttributes().getFirst(UserModel.USERNAME));
    }

    @Test
    public void testCustomAttributeInAnyContext() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testCustomAttributeInAnyContext);
    }

    private static void testCustomAttributeInAnyContext(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("address", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER)), new UPAttributeRequired()));
        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        try {
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // address is mandatory
            assertTrue(ve.isAttributeOnError("address"));
        }

        containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, "address");

        // not writable in user api, no validation should happen
        profile = provider.create(UserProfileContext.USER_API, attributes);
        profile.validate();

        attributes.put("address", "myaddress");
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();
    }

    @Test
    public void testEmptyAttributeRemoved() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testEmptyAttributeRemoved);
    }

    private static void testEmptyAttributeRemoved(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        attributes.put("address", "foo");

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("address", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER))));
        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();

        // attribute explicitly set with an empty value so we assume it should be removed regardless the `removeAttributes` parameter being set to false
        attributes.put("address", "");
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        profile.update(false);

        assertNull(user.getFirstAttribute("address"));

        attributes.put("address", "bar");
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        profile.update();
        assertEquals("bar", user.getFirstAttribute("address"));

        // attribute not provided so we assume there is no intention to remove the attribute
        attributes.remove("address");
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        profile.update(false);

        assertNotNull(user.getFirstAttribute("address"));
    }

    @Test
    public void testResolveProfile() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testResolveProfile);
    }

    private static void testResolveProfile(KeycloakSession session) {
        configureAuthenticationSession(session);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("business.address", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER)), new UPAttributeRequired(Set.of(), Set.of("customer"))));
        provider.setConfiguration(config);

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
    public void testValidation() throws IOException {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::failValidationWhenEmptyAttributes);
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testAttributeValidation);
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testEmailAsUsernameValidation);
        getTestingClient().server(TEST_REALM_NAME).run((KeycloakSession session) -> testNonAsciiEmailValidator(session, false));
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(testRealm())
                .setSmtpServer(EmailSenderProvider.CONFIG_ALLOW_UTF8, Boolean.TRUE.toString()).update()) {
            getTestingClient().server(TEST_REALM_NAME).run((KeycloakSession session) -> testNonAsciiEmailValidator(session, true));
        }
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

        UPConfig config = provider.getConfiguration();

        UPAttribute email = config.getAttribute("email");

        email.setRequired(null);

        provider.setConfiguration(config);

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

    private static void testEmailAsUsernameValidation(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        provider.setConfiguration(null);
        UserProfile profile;
        RealmModel realm = session.getContext().getRealm();

        try {
            realm.setRegistrationEmailAsUsername(true);
            attributes.clear();
            attributes.put(UserModel.FIRST_NAME, "Joe");
            attributes.put(UserModel.LAST_NAME, "Doe");
            // valid email but invalid as username
            attributes.put(UserModel.EMAIL, "foo%bar@example.com");
            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            profile.validate();
        } catch (ValidationException ve) {
            Assert.fail("Should be OK email as username");
        } finally {
            realm.setRegistrationEmailAsUsername(false);
        }
    }

    private static void testNonAsciiEmailValidator(KeycloakSession session, boolean success) {
        Map<String, Object> attributes = new HashMap<>();
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        List<ValidationError> errors = new ArrayList<>();
        attributes.put(UserModel.USERNAME, "diego");
        attributes.put(UserModel.EMAIL, "diegø@foo.com");
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        if (success) {
            assertTrue(profile.getAttributes().validate(UserModel.EMAIL, errors::add));
        } else {
            assertFalse(profile.getAttributes().validate(UserModel.EMAIL, errors::add));
            assertTrue(containsErrorMessage(errors, EmailValidator.MESSAGE_NON_ASCII_LOCAL_PART_EMAIL));
        }
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

    private static void testValidateComplianceWithUserProfile(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().addUser(realm, "profiled-user");
        UserProfileProvider provider = getUserProfileProvider(session);

        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("address", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER)), new UPAttributeRequired()));
        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, user);

        try {
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // username is mandatory
            assertTrue(ve.isAttributeOnError("address"));
        }

        user.setSingleAttribute(UserModel.FIRST_NAME, "john");
        user.setSingleAttribute(UserModel.LAST_NAME, "doe");
        user.setSingleAttribute(UserModel.EMAIL, "jd@keycloak.org");
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
        user.setFirstName("John");
        user.setLastName("John");
        user.setEmail(org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("address", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER)), new UPAttributeRequired()));
        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, user);
        Attributes attributes = profile.getAttributes();

        assertThat(attributes.nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.LOCALE, "address"));

        try {
            profile.validate();
            Assert.fail("Should fail validation");
        } catch (ValidationException ve) {
            // username is mandatory
            assertTrue(ve.isAttributeOnError("address"));
        }

        assertNotNull(attributes.getFirst(UserModel.USERNAME));
        assertNotNull(attributes.getFirst(UserModel.EMAIL));
        assertNotNull(attributes.getFirst(UserModel.FIRST_NAME));
        assertNotNull(attributes.getFirst(UserModel.LAST_NAME));
        assertNull(attributes.getFirst("address"));

        user.setAttribute("address", Arrays.asList("fixed-address"));

        profile = provider.create(UserProfileContext.ACCOUNT, user);
        attributes = profile.getAttributes();

        profile.validate();

        assertNotNull(attributes.getFirst("address"));
    }

    @Test
    public void testGetProfileAttributeGroups() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testGetProfileAttributeGroups);
    }

    private static void testGetProfileAttributeGroups(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().addUser(realm, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        UPGroup companyAddress = new UPGroup("companyaddress");
        companyAddress.setDisplayHeader("header");
        companyAddress.setDisplayDescription("description");
        config.addGroup(companyAddress);
        config.addOrReplaceAttribute(new UPAttribute("address", companyAddress));
        UPGroup groupWithAnnotation = new UPGroup("groupwithanno");
        groupWithAnnotation.setAnnotations(Map.of("anno1", "value1", "anno2", "value2"));
        config.addGroup(groupWithAnnotation);
        config.addOrReplaceAttribute(new UPAttribute("second", groupWithAnnotation));
        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, user);
        Attributes attributes = profile.getAttributes();

        assertThat(attributes.nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.LOCALE, "address", "second"));


        AttributeGroupMetadata companyAddressGroup = attributes.getMetadata("address").getAttributeGroupMetadata();
        assertEquals("companyaddress", companyAddressGroup.getName());
        assertEquals("header", companyAddressGroup.getDisplayHeader());
        assertEquals("description", companyAddressGroup.getDisplayDescription());
        assertNull(companyAddressGroup.getAnnotations());

        AttributeGroupMetadata groupwithannoGroup = attributes.getMetadata("second").getAttributeGroupMetadata();
        assertEquals("groupwithanno", groupwithannoGroup.getName());
        assertNull(groupwithannoGroup.getDisplayHeader());
        assertNull(groupwithannoGroup.getDisplayDescription());
        Map<String, Object> annotations = groupwithannoGroup.getAnnotations();
        assertEquals(2, annotations.size());
        assertEquals("value1", annotations.get("anno1"));
        assertEquals("value2", annotations.get("anno2"));
    }

    @Test
    public void testCreateAndUpdateUser() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testCreateAndUpdateUser);
    }

    private static void testCreateAndUpdateUser(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);

        UPConfig config = provider.getConfiguration();
        config.addOrReplaceAttribute(new UPAttribute("address", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN, ROLE_USER))));
        config.addOrReplaceAttribute(new UPAttribute("business.address", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN, ROLE_USER))));
        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();
        String userName = org.keycloak.models.utils.KeycloakModelUtils.generateId();

        attributes.put(UserModel.USERNAME, userName);
        attributes.put(UserModel.EMAIL, "user@keycloak.org");
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
        Map<String, String> attributesUpdatedOldValues = new HashMap<>();
        attributesUpdatedOldValues.put(UserModel.FIRST_NAME, "Joe");
        attributesUpdatedOldValues.put(UserModel.LAST_NAME, "Doe");
        attributesUpdatedOldValues.put(UserModel.EMAIL, "user@keycloak.org");

        profile.update((attributeName, userModel, oldValue) -> {
            assertTrue(attributesUpdated.add(attributeName));
            assertEquals(attributesUpdatedOldValues.get(attributeName), getSingleValue(oldValue));
            assertEquals(attributes.get(attributeName), userModel.getFirstAttribute(attributeName));
            });

        assertThat(attributesUpdated, containsInAnyOrder(UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.EMAIL));

        configureAuthenticationSession(session);

        attributes.put("business.address", "fixed-business-address");
        profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);

        attributesUpdated.clear();
        profile.update((attributeName, userModel, oldValue) -> assertTrue(attributesUpdated.add(attributeName)));

        assertThat(attributesUpdated, containsInAnyOrder("business.address"));

        assertEquals("fixed-business-address", user.getFirstAttribute("business.address"));
    }

    private static String getSingleValue(List<String> vals) {
        if(vals==null || vals.isEmpty())
            return null;
        return vals.get(0);
    }

    @Test
    public void testReadonlyUpdates() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testReadonlyUpdates);
    }

    private static void testReadonlyUpdates(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        attributes.put("address", Arrays.asList("fixed-address"));
        attributes.put("department", Arrays.asList("sales"));

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("department", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN))));
        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        UserModel user = profile.create();

        assertThat(profile.getAttributes().nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.LOCALE, "department"));

        assertNull(user.getFirstAttribute("department"));

        profile = provider.create(UserProfileContext.USER_API, attributes, user);

        Set<String> attributesUpdated = new HashSet<>();

        profile.update((attributeName, userModel, oldValue) -> assertTrue(attributesUpdated.add(attributeName)));

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
    public void testReadonlyEmailCannotBeUpdated() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testReadonlyEmailCannotBeUpdated);
    }

    private static void testReadonlyEmailCannotBeUpdated(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, "readonly@foo.bar");

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("email", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN))));

        // configure email r/o for user
        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        UserModel user = profile.create();

        assertThat(profile.getAttributes().nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.LOCALE));

        profile = provider.create(UserProfileContext.USER_API, attributes, user);

        Set<String> attributesUpdated = new HashSet<>();

        profile.update((attributeName, userModel, oldValue) -> assertTrue(attributesUpdated.add(attributeName)));

        attributes.put(UserModel.EMAIL, "cannot-change@foo.bar");

        profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);
        try {
            profile.update();
            fail("Should fail since email is read only");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError("email"));
        }
        assertEquals("E-Mail address shouldn't be changed", "readonly@foo.bar", user.getEmail());

        attributes.put(UserModel.EMAIL, "admin-can-change@foo.bar");
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update();
        assertEquals("admin-can-change@foo.bar", user.getEmail());
    }

    @Test
    public void testUpdateEmail() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testUpdateEmail);
    }

    private static void testUpdateEmail(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        RequiredActionProviderModel actionConfig = realm.getRequiredActionProviderByAlias(RequiredAction.UPDATE_EMAIL.name());

        actionConfig.setEnabled(true);

        realm.updateRequiredActionProvider(actionConfig);

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();

        config.getAttribute("email").getPermissions().setEdit(Set.of(ROLE_USER, ROLE_ADMIN));

        // configure email r/w for user
        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, "myemail@foo.bar");

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();
        assertEquals(attributes.get(UserModel.EMAIL), user.getEmail());
        assertNull(user.getFirstAttribute(UserModel.EMAIL_PENDING));

        assertThat(profile.getAttributes().nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.LOCALE, TermsAndConditions.USER_ATTRIBUTE, UserModel.EMAIL_PENDING));

        attributes.put("email", "changed@foo.bar");

        profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);
        try {
            profile.update();
            fail("Should fail");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(UserModel.EMAIL));
        }

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        try {
            profile.update();
            fail("Should fail");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(UserModel.EMAIL));
        }

        attributes.remove(UserModel.EMAIL);
        attributes.put(UserModel.EMAIL_PENDING, "pending@foo.bar");

        profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);
        profile.update();
        assertNull(user.getFirstAttribute(UserModel.EMAIL_PENDING));

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        profile.update();
        assertNull(user.getFirstAttribute(UserModel.EMAIL_PENDING));

        config.getAttribute(UserModel.EMAIL).getPermissions().setEdit(Set.of(ROLE_ADMIN));
        provider.setConfiguration(config);
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        profile.update();
        assertEquals("myemail@foo.bar", user.getFirstAttribute(UserModel.EMAIL));
        assertNull(user.getFirstAttribute(UserModel.EMAIL_PENDING));
        assertFalse(profile.getAttributes().getWritable().containsKey(UserModel.EMAIL));
        user.setEmail(null);
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        profile.update();
        assertNull(user.getFirstAttribute(UserModel.EMAIL));
        assertFalse(profile.getAttributes().getWritable().containsKey(UserModel.EMAIL));
        config.getAttribute(UserModel.EMAIL).getPermissions().setEdit(Set.of(ROLE_USER, ROLE_ADMIN));
        provider.setConfiguration(config);
        user.setEmail("myemail@foo.bar");

        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update();
        assertNotNull(user.getFirstAttribute(UserModel.EMAIL_PENDING));

        attributes.put(UserModel.EMAIL_PENDING, "");

        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update();
        assertNull(user.getFirstAttribute(UserModel.EMAIL_PENDING));
    }

    @Test
    public void testDoNotUpdateUndefinedAttributes() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testDoNotUpdateUndefinedAttributes);
    }

    private static void testDoNotUpdateUndefinedAttributes(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        attributes.put("address", Arrays.asList("fixed-address"));
        attributes.put("department", Arrays.asList("sales"));
        attributes.put("phone", Arrays.asList("fixed-phone"));

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("department", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN))));
        config.addOrReplaceAttribute(new UPAttribute("phone", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN))));
        config.addOrReplaceAttribute(new UPAttribute("address", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN))));
        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        UserModel user = profile.create();
        assertThat(profile.getAttributes().nameSet(),
                containsInAnyOrder(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.LOCALE, "address", "department", "phone"));

        attributes.put("address", Arrays.asList("change-address"));
        attributes.put("department", Arrays.asList("changed-sales"));
        attributes.put("phone", Arrays.asList("changed-phone"));
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        Set<String> attributesUpdated = new HashSet<>();
        profile.update((attributeName, userModel, oldValue) -> assertTrue(attributesUpdated.add(attributeName)));
        assertThat(attributesUpdated, containsInAnyOrder("department", "address", "phone"));

        config.removeAttribute("address");
        provider.setConfiguration(config);
        attributesUpdated.clear();
        attributes.remove("address");
        attributes.put("department", "foo");
        attributes.put("phone", "foo");
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update((attributeName, userModel, oldValue) -> assertTrue(attributesUpdated.add(attributeName)));
        assertThat(attributesUpdated, containsInAnyOrder("department", "phone"));
        assertTrue(user.getAttributes().containsKey("address"));

        config.addOrReplaceAttribute(new UPAttribute("address", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN))));
        provider.setConfiguration(config);
        attributes.put("department", "foo");
        attributes.put("phone", "foo");
        attributes.put("address", "bar");
        attributesUpdated.clear();
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update((attributeName, userModel, oldValue) -> assertTrue(attributesUpdated.add(attributeName)));
        assertThat(attributesUpdated, containsInAnyOrder("address"));
        assertEquals("bar", user.getFirstAttribute("address"));
        assertEquals("foo", user.getFirstAttribute("phone"));
        assertEquals("foo", user.getFirstAttribute("department"));

        attributes.remove("address");
        attributesUpdated.clear();
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update((attributeName, userModel, oldValue) -> assertTrue(attributesUpdated.add(attributeName)));
        assertThat(attributesUpdated, containsInAnyOrder("address"));
        assertFalse(user.getAttributes().containsKey("address"));
        assertTrue(user.getAttributes().containsKey("phone"));
        assertTrue(user.getAttributes().containsKey("department"));

        String prefixedAttributeName = Constants.USER_ATTRIBUTES_PREFIX.concat("prefixed");
        attributes.put(prefixedAttributeName, "foo");
        attributesUpdated.clear();
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update((attributeName, userModel, oldValue) -> assertTrue(attributesUpdated.add(attributeName)));
        assertTrue(attributesUpdated.isEmpty());
        assertFalse(user.getAttributes().containsKey("prefixedAttributeName"));
    }

    @Test
    public void testComponentModelId() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testComponentModelId);
    }

    private static void testComponentModelId(KeycloakSession session) {
        setDefaultConfiguration(session);
        Optional<ComponentModel> component = getComponentModel(session);
        assertTrue(component.isPresent());
        assertEquals("declarative-user-profile", component.get().getProviderId());
    }

    @Test
    public void testInvalidConfiguration() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testInvalidConfiguration);
    }

    private static void testInvalidConfiguration(KeycloakSession session) {
        try {
            setConfiguration(session, "{\"validateConfigAttribute\": true}");
            fail("Should fail validation");
        } catch (RuntimeException ve) {
            // OK
        }

    }

    @Test
    public void testResetConfiguration() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testResetConfiguration);
    }

    private static void testResetConfiguration(KeycloakSession session) {
        setConfiguration(session, null);
        assertFalse(getComponentModel(session).isPresent());
    }

    @Test
    public void testDefaultConfig() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testDefaultConfig);
    }

    private static void testDefaultConfig(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);

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

    private static void testCustomValidationForUsername(KeycloakSession session) {
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        UPAttribute attribute = new UPAttribute(UserModel.USERNAME);

        Map<String, Object> validatorConfig = new HashMap<>();

        validatorConfig.put("min", 4);

        attribute.addValidation(LengthValidator.ID, validatorConfig);

        config.addOrReplaceAttribute(attribute);

        UserProfileProvider provider = getUserProfileProvider(session);
        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "us");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        try {
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(UserModel.USERNAME));
            assertTrue(ve.hasError(LengthValidator.MESSAGE_INVALID_LENGTH_TOO_SHORT));
        }

        attributes.put(UserModel.USERNAME, "user");

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        profile.validate();

        provider.setConfiguration(null);

        attributes.put(UserModel.USERNAME, ROLE_USER);
        attributes.put(UserModel.EMAIL, "user@keycloak.org");
        attributes.put(UserModel.FIRST_NAME, "Joe");
        attributes.put(UserModel.LAST_NAME, "Doe");

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        profile.validate();
    }

    @Test
    public void testRemoveDefaultValidationFromUsername() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testRemoveDefaultValidationFromUsername);
    }

    private static void testRemoveDefaultValidationFromUsername(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);

        // reset configuration to default
        provider.setConfiguration(null);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "你好世界");
        attributes.put(UserModel.EMAIL, "test@keycloak.org");
        attributes.put(UserModel.FIRST_NAME, "Foo");
        attributes.put(UserModel.LAST_NAME, "Bar");

        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        try {
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.hasError(UsernameIDNHomographValidator.MESSAGE_NO_MATCH));
        }

        UPConfig config = provider.getConfiguration();

        for (UPAttribute attribute : config.getAttributes()) {
            if (UserModel.USERNAME.equals(attribute.getName())) {
                attribute.getValidations().remove(UsernameIDNHomographValidator.ID);
                break;
            }
        }

        provider.setConfiguration(config);

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

        profile.validate();
    }

    @Test
    public void testHomographValidator() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testHomographValidator);
    }

    private static void testHomographValidator(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();

        UPAttribute attribute = config.getAttribute(UserModel.LAST_NAME);
        attribute.addValidation(UsernameIDNHomographValidator.ID, Map.of(UsernameIDNHomographValidator.CFG_ERROR_MESSAGE, "error-something"));
        provider.setConfiguration(config);
        try {
            Map<String, Object> attributes = new HashMap<>();

            attributes.put(UserModel.USERNAME, "abc");
            attributes.put(UserModel.EMAIL, "test@keycloak.org");
            attributes.put(UserModel.FIRST_NAME, "Foo");
            attributes.put(UserModel.LAST_NAME, "你好世界");

            UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

            try {
                profile.validate();
                fail("Should fail validation");
            } catch (ValidationException ve) {
                assertTrue(ve.hasError("error-something"));
            }
        } finally {
            attribute.getValidations().remove(UsernameIDNHomographValidator.ID);
            provider.setConfiguration(config);
        }
    }

    @Test
    public void testOptionalAttributes() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testOptionalAttributes);
    }

    private static void testOptionalAttributes(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        UPAttribute attribute = new UPAttribute();
        attribute.setName(UserModel.FIRST_NAME);
        Map<String, Object> validatorConfig = new HashMap<>();
        validatorConfig.put(LengthValidator.KEY_MAX, 4);
        attribute.addValidation(LengthValidator.ID, validatorConfig);
        config.addOrReplaceAttribute(attribute);

        attribute = new UPAttribute();
        attribute.setName(UserModel.LAST_NAME);
        attribute.addValidation(LengthValidator.ID, validatorConfig);
        config.addOrReplaceAttribute(attribute);

        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

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
    @ModelTest(realmName = "test")
    public void testPersonNameProhibitedCharsValidator(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();

        UPAttribute lastNameAttr = config.getAttribute(UserModel.LAST_NAME);
        Map<String, Object> origValidatorCfg = lastNameAttr.getValidations().get(PersonNameProhibitedCharactersValidator.ID);
        try {
            // Should fail with the default error message
            Map<String, Object> attributes = new HashMap<>();

            attributes.put(UserModel.USERNAME, "abc");
            attributes.put(UserModel.EMAIL, "test@keycloak.org");
            attributes.put(UserModel.FIRST_NAME, "Foo");
            attributes.put(UserModel.LAST_NAME, "Jo&hn");

            UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);

            try {
                profile.validate();
                fail("Should fail validation");
            } catch (ValidationException ve) {
                assertTrue(ve.hasError(PersonNameProhibitedCharactersValidator.MESSAGE_NO_MATCH));
            }

            // custom error message should be used
            lastNameAttr.getValidations().put(PersonNameProhibitedCharactersValidator.ID, Map.of(PersonNameProhibitedCharactersValidator.CFG_ERROR_MESSAGE, "error-something"));
            provider.setConfiguration(config);

            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            try {
                profile.validate();
                fail("Should fail validation");
            } catch (ValidationException ve) {
                assertTrue(ve.hasError("error-something"));
            }

            // error-message config parameters used with empty value. Default error message should be used
            lastNameAttr = config.getAttribute(UserModel.LAST_NAME);
            lastNameAttr.getValidations().put(PersonNameProhibitedCharactersValidator.ID, Map.of(PersonNameProhibitedCharactersValidator.CFG_ERROR_MESSAGE, ""));
            provider.setConfiguration(config);

            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            try {
                profile.validate();
                fail("Should fail validation");
            } catch (ValidationException ve) {
                assertTrue(ve.hasError(PersonNameProhibitedCharactersValidator.MESSAGE_NO_MATCH));
            }
        } finally {
            // Rollback
            lastNameAttr.getValidations().put(PersonNameProhibitedCharactersValidator.ID, origValidatorCfg);
            provider.setConfiguration(config);
        }
    }

    @Test
    @ModelTest(realmName = "test")
    public void testUriValidator(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();

        UPAttribute attribute = new UPAttribute("picture-url");
        attribute.addValidation(UriValidator.ID, new HashMap<>());
        config.addOrReplaceAttribute(attribute);
        provider.setConfiguration(config);

        try {
            // Should fail with the default error message
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(UserModel.USERNAME, "abc");
            attributes.put("picture-url", "some-invalid-url");

            UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
            try {
                profile.validate();
                fail("Should fail validation");
            } catch (ValidationException ve) {
                assertTrue(ve.hasError(UriValidator.MESSAGE_INVALID_URI));
            }

            // URL with fragment should be OK by default
            attributes.put("picture-url", "https://somehost/somepath?param=foo#frg=bar");
            profile = provider.create(UserProfileContext.USER_API, attributes);
            profile.validate();

            // Not allow fragment
            attribute.addValidation(UriValidator.ID, Map.of(UriValidator.KEY_ALLOW_FRAGMENT, false));
            config.addOrReplaceAttribute(attribute);
            provider.setConfiguration(config);

            attributes.put("picture-url", "https://somehost/somepath?param=foo#frg=bar");
            profile = provider.create(UserProfileContext.USER_API, attributes);
            try {
                profile.validate();
                fail("Should fail validation");
            } catch (ValidationException ve) {
                assertTrue(ve.hasError(UriValidator.MESSAGE_INVALID_FRAGMENT));
            }

            // not allow file URL by default
            attributes.put("picture-url", "file:///somefile.txt");
            profile = provider.create(UserProfileContext.USER_API, attributes);
            try {
                profile.validate();
                fail("Should fail validation");
            } catch (ValidationException ve) {
                assertTrue(ve.hasError(UriValidator.MESSAGE_INVALID_SCHEME));
            }

            // Allow file scheme and check it works
            attribute.addValidation(UriValidator.ID, Map.of(UriValidator.KEY_ALLOWED_SCHEMES, Arrays.asList("https", "http", "file")));
            config.addOrReplaceAttribute(attribute);
            provider.setConfiguration(config);

            attributes.put("picture-url", "file:///somefile.txt");
            profile = provider.create(UserProfileContext.USER_API, attributes);
            profile.validate();
        } finally {
            config.removeAttribute("picture-url");
            provider.setConfiguration(config);
        }
    }

    @Test
    public void testCustomAttributeRequired() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testCustomAttributeRequired);
    }

    private static void testCustomAttributeRequired(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
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

        config.addOrReplaceAttribute(attribute);

        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

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
    public void testNullAttributesInConfig() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testNullAttributesInConfig);
    }

    private static void testNullAttributesInConfig(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.setAttributes(null);
        config.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);

        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);

        profile.validate();

        config.setAttributes(Collections.emptyList());
        try {
            provider.setConfiguration(config);
            Assert.fail("Expected to fail as we are trying to remove required attributes email and username");
        } catch (ComponentValidationException cve) {
            //ignore
        }
    }

    @Test
    public void testCustomAttributeOptional() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testCustomAttributeOptional);
    }

    private static void testCustomAttributeOptional(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        UPAttribute attribute = new UPAttribute();

        attribute.setName(ATT_ADDRESS);

        Map<String, Object> validatorConfig = new HashMap<>();
        validatorConfig.put(LengthValidator.KEY_MIN, 4);
        attribute.addValidation(LengthValidator.ID, validatorConfig);

        config.addOrReplaceAttribute(attribute);

        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

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

    private static void testRequiredIfUser(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute(ATT_ADDRESS, new UPAttributePermissions(Set.of(), Set.of(ROLE_USER)), new UPAttributeRequired(Set.of(ROLE_USER), Set.of())));
        provider.setConfiguration(config);

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

        profile = provider.create(UserProfileContext.REGISTRATION, attributes);
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

    private static void testRequiredIfAdmin(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute(ATT_ADDRESS, new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN)), new UPAttributeRequired(Set.of(ROLE_ADMIN), Set.of())));
        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

        // NO fail on common contexts
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();

        profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        profile.validate();

        profile = provider.create(UserProfileContext.REGISTRATION, attributes);
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
    @ModelTest(realmName=TEST_REALM_NAME)
    public void testEmailRequired(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(UserModel.USERNAME, "james");
        attributes.put(UserModel.FIRST_NAME, "James");
        attributes.put(UserModel.LAST_NAME, "Doe");
        UserProfile profile;

        // Email required for users by default, but not for admins
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        provider.setConfiguration(config);
        UPAttribute emailOrigConfig = config.getAttribute(UserModel.EMAIL);
        Assert.assertEquals(emailOrigConfig.getRequired().getRoles(), Set.of(ROLE_USER)); // Should be required only for users by default

        try {
            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            profile.validate();
            Assert.fail("Should not be here as email is required for users");
        } catch (ValidationException ve) {
            // expected
        }
        try {
            profile = provider.create(UserProfileContext.USER_API, attributes);
            profile.validate();
        } catch (ValidationException ve) {
            Assert.fail("Should not be here as email is NOT required for administrators");
        }

        // Test email required in config, registrationEmailAsUsername = false : Email should be required
        config.addOrReplaceAttribute(new UPAttribute(UserModel.EMAIL, new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN, ROLE_USER)), new UPAttributeRequired(Set.of(ROLE_ADMIN, ROLE_USER), Set.of())));
        provider.setConfiguration(config);

        try {
            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            profile.validate();
            Assert.fail("Should not be here as email is required for users");
        } catch (ValidationException ve) {
            // expected
        }
        try {
            profile = provider.create(UserProfileContext.USER_API, attributes);
            profile.validate();
            Assert.fail("Should not be here as email is required for administrators");
        } catch (ValidationException ve) {
            // expected
        }

        // Test email required in config, registrationEmailAsUsername = true : Email should be required
        try {
            realm.setRegistrationEmailAsUsername(true);
            try {
                profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
                profile.validate();
                Assert.fail("Should not be here as email is required for users");
            } catch (ValidationException ve) {
                // expected
            }
            try {
                profile = provider.create(UserProfileContext.USER_API, attributes);
                profile.validate();
                Assert.fail("Should not be here as email is required for administrators");
            } catch (ValidationException ve) {
                // expected
            }
        } finally {
            realm.setRegistrationEmailAsUsername(false);
        }

        // Test email NOT required in config, registrationEmailAsUsername = true : Email should be required
        config.addOrReplaceAttribute(new UPAttribute(UserModel.EMAIL, new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN, ROLE_USER)), null));
        provider.setConfiguration(config);
        try {
            realm.setRegistrationEmailAsUsername(true);
            try {
                profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
                profile.validate();
                Assert.fail("Should not be here as email is required for users");
            } catch (ValidationException ve) {
                // expected
            }
            try {
                profile = provider.create(UserProfileContext.USER_API, attributes);
                profile.validate();
                Assert.fail("Should not be here as email is required for administrators");
            } catch (ValidationException ve) {
                // expected
            }
        } finally {
            realm.setRegistrationEmailAsUsername(false);
        }

        // Test email NOT required in config, registrationEmailAsUsername = false : Email should NOT be required
        try {
            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            profile.validate();
        } catch (ValidationException ve) {
            Assert.fail("Should not be here as email is required for users");
        }
        try {
            profile = provider.create(UserProfileContext.USER_API, attributes);
            profile.validate();
        } catch (ValidationException ve) {
            Assert.fail("Should not be here as email is required for administrators");
        }
    }

    @Test
    public void testNoValidationsIfUserReadOnly() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testNoValidationsIfUserReadOnly);
    }

    private static void testNoValidationsIfUserReadOnly(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute(ATT_ADDRESS, new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN)), new UPAttributeRequired()));
        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.FIRST_NAME, "user");
        attributes.put(UserModel.LAST_NAME, "user");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");

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

    private static void testNoValidationsIfAdminReadOnly(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute(ATT_ADDRESS, new UPAttributePermissions(Set.of(), Set.of(ROLE_USER)), new UPAttributeRequired()));
        provider.setConfiguration(config);

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
    public void testIgnoreReadOnlyAttribute() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testIgnoreReadOnlyAttribute);
    }

    private static void testIgnoreReadOnlyAttribute(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute(ATT_ADDRESS, new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER)), new UPAttributeRequired(Set.of(ROLE_USER), Set.of())));
        config.addOrReplaceAttribute(new UPAttribute(UserModel.FIRST_NAME, new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_USER)), new UPAttributeRequired(Set.of(ROLE_USER), Set.of())));
        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());

        // Fails on USER context
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        try {
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
        }

        // attribute ignored for admin when not provided and creating user
        profile = provider.create(UserProfileContext.USER_API, attributes);
        profile.validate();

        // attribute ignored for admin when empty and creating user
        attributes.put(ATT_ADDRESS, List.of(""));
        attributes.put(UserModel.FIRST_NAME, List.of(""));
        profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();

        // attribute ignored for admin when empty and updating user
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.validate();

        // attribute not ignored for admin when empty and updating user
        user.setFirstName("alice");
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        try {
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(UserModel.FIRST_NAME));
        }
    }

    @Test
    public void testReadOnlyInternalAttributeValidation() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testReadOnlyInternalAttributeValidation);
    }

    private static void testReadOnlyInternalAttributeValidation(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        UserModel maria = session.users().addUser(realm, "maria");

        maria.setAttribute(LDAPConstants.LDAP_ID, List.of("1"));

        UserProfileProvider provider = getUserProfileProvider(session);
        Map<String, List<String>> attributes = new HashMap<>();

        attributes.put(LDAPConstants.LDAP_ID, List.of("2"));

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes, maria);

        try {
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(LDAPConstants.LDAP_ID));
        }
    }

    @Test
    @ModelTest
    public void testRequiredByClientScope(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        session.getContext().setRealm(realm);
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute(ATT_ADDRESS, new UPAttributePermissions(Set.of(), Set.of(ROLE_USER)), new UPAttributeRequired(Set.of(), Set.of("client-a"))));
        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, "user@email.test");

        // client with default scope for which is attribute configured as required
        configureAuthenticationSession(session, "client-a", null);

        // no fail on User API because they don't have access to scopes yet
        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        profile.validate();

        // fail on auth flow scopes when scope is required
        try {
            profile = provider.create(UserProfileContext.ACCOUNT, attributes);
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
        }

        try {
            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
        }
        try {
            profile = provider.create(UserProfileContext.REGISTRATION, attributes);
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
    @ModelTest
    public void testRequiredByOptionalClientScope(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        session.getContext().setRealm(realm);

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute(ATT_ADDRESS, new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN, ROLE_USER)), new UPAttributeRequired(Set.of(ROLE_ADMIN, ROLE_USER), Set.of("some-optional-scope"))));
        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, "user");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, "user@email.test");

        // client with default scopes. No address scope included
        configureAuthenticationSession(session, "client-a", null);

        // no fail on User API because they don't have access to scopes yet
        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        profile.validate();

        // no fail on auth flow scopes when scope is not required
        profile = provider.create(UserProfileContext.ACCOUNT, attributes);
        profile.validate();
        profile = provider.create(UserProfileContext.REGISTRATION, attributes);
        profile.validate();
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();
        profile = provider.create(UserProfileContext.IDP_REVIEW, attributes);
        profile.validate();

        // client with default scopes for which is attribute NOT configured as required
        configureAuthenticationSession(session, "client-a", Set.of("some-optional-scope"));

        // No fail on admin and account console as they do not have scopes
        profile = provider.create(UserProfileContext.USER_API, attributes);
        profile.validate();

        // fail on auth flow scopes when scope is required
        try {
            profile = provider.create(UserProfileContext.ACCOUNT, attributes);
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
        }
        try {
            profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
            profile.validate();
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(ATT_ADDRESS));
        }
        try {
            profile = provider.create(UserProfileContext.REGISTRATION, attributes);
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

    private static void testConfigurationInvalidScope(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute(ATT_ADDRESS, new UPAttributePermissions(Set.of(), Set.of(ROLE_USER)),
                new UPAttributeRequired(Set.of(), Set.of("invalid")), new UPAttributeSelector(Set.of("invalid"))));

        try {
            provider.setConfiguration(config);
            Assert.fail("Expected to fail due to invalid client scope");
        } catch (ComponentValidationException cve) {
            //ignore
        }
    }

    @Test
    public void testUsernameAndEmailPermissionNotSetIfEmpty() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testUsernameAndEmailPermissionNotSetIfEmpty);
    }

    private static void testUsernameAndEmailPermissionNotSetIfEmpty(KeycloakSession session){
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = provider.getConfiguration();

        for (UPAttribute attribute : config.getAttributes()) {
            if (attribute.getName().equals(UserModel.USERNAME) || attribute.getName().equals(UserModel.EMAIL)) {
                attribute.setPermissions(new UPAttributePermissions());
            }
        }

        provider.setConfiguration(config);

        RealmModel realm = session.getContext().getRealm();
        String username = "profiled-user-profile";
        UserModel user = session.users().addUser(realm, username);
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, user.getUsername());
        attributes.put(UserModel.EMAIL, "test@keycloak.com");

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes, user);

        profile.update();

        user = session.users().getUserById(realm, user.getId());

        assertEquals("test@keycloak.com", user.getEmail());
    }

    @Test
    public void testDoNotRemoveAttributes() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testDoNotRemoveAttributes);
    }

    private static void testDoNotRemoveAttributes(KeycloakSession session) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId());
        attributes.put(UserModel.EMAIL, Arrays.asList("test@test.com"));
        attributes.put("test-attribute", Arrays.asList("Test Value"));
        attributes.put("foo", Arrays.asList("foo"));

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.removeAttribute(UserModel.FIRST_NAME);
        config.removeAttribute(UserModel.LAST_NAME);
        config.addOrReplaceAttribute(new UPAttribute("test-attribute", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER, ROLE_ADMIN))));
        config.addOrReplaceAttribute(new UPAttribute("foo", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER, ROLE_ADMIN))));
        config.addOrReplaceAttribute(new UPAttribute("email", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER, ROLE_ADMIN))));

        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();

        attributes.clear();
        attributes.put(UserModel.EMAIL, Arrays.asList("new-email@test.com"));
        attributes.put("foo", "changed");
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        try {
            profile.update(false);
            fail("Should fail validation");
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(UserModel.USERNAME));
            assertTrue(ve.hasError(Messages.MISSING_USERNAME));
        }

        attributes.put(UserModel.USERNAME, Collections.singletonList(user.getUsername()));
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update(false);

        profile = provider.create(UserProfileContext.USER_API, user);
        Attributes userAttributes = profile.getAttributes();
        assertEquals("new-email@test.com", userAttributes.getFirst(UserModel.EMAIL));
        assertEquals("Test Value", userAttributes.getFirst("test-attribute"));
        assertEquals("changed", userAttributes.getFirst("foo"));

        attributes.remove("foo");
        attributes.put("test-attribute", userAttributes.getFirst("test-attribute"));
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update(true);
        profile = provider.create(UserProfileContext.USER_API, user);
        userAttributes = profile.getAttributes();
        // remove attribute if not set
        assertEquals("new-email@test.com", userAttributes.getFirst(UserModel.EMAIL));
        assertEquals("Test Value", userAttributes.getFirst("test-attribute"));
        assertNull(userAttributes.getFirst("foo"));

        config.addOrReplaceAttribute(new UPAttribute("test-attribute", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER))));
        provider.setConfiguration(config);
        attributes.remove("test-attribute");
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update(true);
        profile = provider.create(UserProfileContext.USER_API, user);
        userAttributes = profile.getAttributes();
        // do not remove test-attribute because admin does not have write permissions
        assertEquals("new-email@test.com", userAttributes.getFirst(UserModel.EMAIL));
        assertEquals("Test Value", userAttributes.getFirst("test-attribute"));

        config.addOrReplaceAttribute(new UPAttribute("test-attribute", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER, ROLE_ADMIN))));
        provider.setConfiguration(config);
        attributes.remove("test-attribute");
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update(true);
        profile = provider.create(UserProfileContext.USER_API, user);
        userAttributes = profile.getAttributes();
        // removes the test-attribute attribute because now admin has write permission
        assertEquals("new-email@test.com", userAttributes.getFirst(UserModel.EMAIL));
        assertNull(userAttributes.getFirst("test-attribute"));
    }

    @Test
    public void testRemoveEmptyRootAttribute() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testRemoveEmptyRootAttribute);
    }

    private static void testRemoveEmptyRootAttribute(KeycloakSession session) {
        Map<String, List<String>> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, List.of(org.keycloak.models.utils.KeycloakModelUtils.generateId()));
        attributes.put(UserModel.EMAIL, List.of(""));
        attributes.put(UserModel.FIRST_NAME, List.of(""));
        attributes.put("test-attribute", List.of(""));

        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("test-attribute", new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN, ROLE_USER))));
        config.addOrReplaceAttribute(new UPAttribute(UserModel.FIRST_NAME, new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN, ROLE_USER))));
        config.addOrReplaceAttribute(new UPAttribute(UserModel.LAST_NAME, new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN, ROLE_USER))));
        config.addOrReplaceAttribute(new UPAttribute(UserModel.EMAIL, new UPAttributePermissions(Set.of(), Set.of(ROLE_ADMIN, ROLE_USER))));
        provider.setConfiguration(config);

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();
        assertNull(user.getEmail());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());

        attributes.remove(UserModel.EMAIL);
        attributes.put(UserModel.FIRST_NAME, List.of("myfname"));
        profile = provider.create(UserProfileContext.USER_API, attributes);
        Attributes upAttributes = profile.getAttributes();
        assertRemoveEmptyRootAttribute(attributes, user, upAttributes);

        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update(false);
        upAttributes = profile.getAttributes();
        assertRemoveEmptyRootAttribute(attributes, user, upAttributes);
    }

    private static void assertRemoveEmptyRootAttribute(Map<String, List<String>> attributes, UserModel user, Attributes upAttributes) {
        assertNull(upAttributes.getFirst(UserModel.LAST_NAME));
        assertNull(user.getLastName());
        assertNull(upAttributes.getFirst(UserModel.EMAIL));
        assertNull(user.getEmail());
        assertEquals(upAttributes.getFirst(UserModel.FIRST_NAME), attributes.get(UserModel.FIRST_NAME).get(0));
    }

    @Test
    public void testRemoveOptionalAttributesFromDefaultConfigIfNotSet() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testRemoveOptionalAttributesFromDefaultConfigIfNotSet);
    }

    private static void testRemoveOptionalAttributesFromDefaultConfigIfNotSet(KeycloakSession session) {
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("foo"));
        config.removeAttribute(UserModel.FIRST_NAME);
        config.removeAttribute(UserModel.LAST_NAME);

        UserProfileProvider provider = getUserProfileProvider(session);
        provider.setConfiguration(config);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        attributes.put("foo", "foo");

        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        UserModel user = profile.create();

        assertFalse(profile.getAttributes().contains(UserModel.FIRST_NAME));
        assertFalse(profile.getAttributes().contains(UserModel.LAST_NAME));

        UPAttribute firstName = new UPAttribute();
        firstName.setName(UserModel.FIRST_NAME);
        config.addOrReplaceAttribute(firstName);
        UPAttribute lastName = new UPAttribute();
        lastName.setName(UserModel.LAST_NAME);
        config.addOrReplaceAttribute(lastName);
        provider.setConfiguration(config);
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        assertTrue(profile.getAttributes().contains(UserModel.FIRST_NAME));
        assertTrue(profile.getAttributes().contains(UserModel.LAST_NAME));
    }

    @Test
    public void testUnmanagedPolicy() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testUnmanagedPolicy);
    }

    private static void testUnmanagedPolicy(KeycloakSession session) {
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        config.addOrReplaceAttribute(new UPAttribute("bar", new UPAttributePermissions(Set.of(), Set.of(ROLE_USER, ROLE_ADMIN))));
        UserProfileProvider provider = getUserProfileProvider(session);
        provider.setConfiguration(config);

        // can't create attribute if policy is disabled
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        attributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        attributes.put("foo", List.of("foo"));
        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();
        assertFalse(user.getAttributes().containsKey("foo"));

        // user already set with an unmanaged attribute, and it should be visible if policy is adminEdit
        user.setSingleAttribute("foo", "foo");
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        assertFalse(profile.getAttributes().contains("foo"));
        config.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ADMIN_EDIT);
        provider.setConfiguration(config);
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        assertTrue(profile.getAttributes().contains("foo"));
        assertFalse(profile.getAttributes().isReadOnly("foo"));

        // user already set with an unmanaged attribute, and it should be visible if policy is adminView but read-only
        config.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ADMIN_VIEW);
        provider.setConfiguration(config);
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        assertTrue(profile.getAttributes().contains("foo"));
        assertTrue(profile.getAttributes().isReadOnly("foo"));

        // user already set with an unmanaged attribute, but it is not available to user-facing contexts
        config.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ADMIN_VIEW);
        provider.setConfiguration(config);
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        assertFalse(profile.getAttributes().contains("foo"));

        // user already set with an unmanaged attribute, and it is available to all contexts
        config.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        provider.setConfiguration(config);
        profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes, user);
        assertTrue(profile.getAttributes().contains("foo"));
        assertFalse(profile.getAttributes().isReadOnly("foo"));
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        assertTrue(profile.getAttributes().contains("foo"));
        assertFalse(profile.getAttributes().isReadOnly("foo"));
    }

    @Test
    public void testOptionalRootAttributesAsUnmanagedAttribute() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testOptionalRootAttributesAsUnmanagedAttribute);
    }

    private static void testOptionalRootAttributesAsUnmanagedAttribute(KeycloakSession session) {
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig();
        UserProfileProvider provider = getUserProfileProvider(session);
        provider.setConfiguration(config);
        Map<String, String> rawAttributes = new HashMap<>();
        rawAttributes.put(UserModel.USERNAME, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        rawAttributes.put(UserModel.EMAIL, org.keycloak.models.utils.KeycloakModelUtils.generateId() + "@keycloak.org");
        rawAttributes.put(UserModel.FIRST_NAME, "firstName");
        rawAttributes.put(UserModel.LAST_NAME, "lastName");
        UserProfile profile = provider.create(UserProfileContext.USER_API, rawAttributes);
        UserModel user = profile.create();
        assertEquals(rawAttributes.get(UserModel.FIRST_NAME), user.getFirstName());
        assertEquals(rawAttributes.get(UserModel.LAST_NAME), user.getLastName());
        AbstractUserRepresentation rep = profile.toRepresentation();
        assertEquals(rawAttributes.get(UserModel.FIRST_NAME), rep.getFirstName());
        assertEquals(rawAttributes.get(UserModel.LAST_NAME), rep.getLastName());
        assertNull(rep.getAttributes());

        config.removeAttribute(UserModel.FIRST_NAME);
        config.removeAttribute(UserModel.LAST_NAME);
        provider.setConfiguration(config);
        profile = provider.create(UserProfileContext.USER_API, user);
        Attributes attributes = profile.getAttributes();
        assertNull(attributes.getFirst(UserModel.FIRST_NAME));
        assertNull(attributes.getFirst(UserModel.LAST_NAME));
        rep = profile.toRepresentation();
        assertNull(rep.getFirstName());
        assertNull(rep.getLastName());
        assertNull(rep.getAttributes());

        rawAttributes.put(UserModel.FIRST_NAME, "firstName");
        rawAttributes.put(UserModel.LAST_NAME, "lastName");
        config.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ADMIN_EDIT);
        provider.setConfiguration(config);
        profile = provider.create(UserProfileContext.USER_API, user);
        attributes = profile.getAttributes();
        assertEquals(rawAttributes.get(UserModel.FIRST_NAME), attributes.getFirst(UserModel.FIRST_NAME));
        assertEquals(rawAttributes.get(UserModel.LAST_NAME), attributes.getFirst(UserModel.LAST_NAME));
        rep = profile.toRepresentation();
        assertNull(rep.getFirstName());
        assertNull(rep.getLastName());
        assertNull(rep.getAttributes());

        rawAttributes.remove(UserModel.LAST_NAME);
        rawAttributes.put(UserModel.FIRST_NAME, "firstName");
        profile = provider.create(UserProfileContext.USER_API, rawAttributes, user);
        attributes = profile.getAttributes();
        assertEquals(rawAttributes.get(UserModel.FIRST_NAME), attributes.getFirst(UserModel.FIRST_NAME));
        assertNull(attributes.getFirst(UserModel.LAST_NAME));
        rep = profile.toRepresentation();
        assertNull(rep.getFirstName());
        assertNull(rep.getLastName());
        assertNull(rep.getAttributes());

        rawAttributes.put(UserModel.LAST_NAME, "lastNameChanged");
        rawAttributes.put(UserModel.FIRST_NAME, "firstNameChanged");
        profile = provider.create(UserProfileContext.USER_API, rawAttributes, user);
        attributes = profile.getAttributes();
        assertEquals(rawAttributes.get(UserModel.FIRST_NAME), attributes.getFirst(UserModel.FIRST_NAME));
        assertEquals(rawAttributes.get(UserModel.LAST_NAME), attributes.getFirst(UserModel.LAST_NAME));
        rep = profile.toRepresentation();
        assertNull(rep.getFirstName());
        assertNull(rep.getLastName());
        assertNull(rep.getAttributes());
    }

    @Test
    public void testAttributeNormalization() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testAttributeNormalization);
    }

    private static void testAttributeNormalization(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        Map<String, String> attributes = new HashMap<>();
        attributes.put(UserModel.USERNAME, "TesT");
        attributes.put(UserModel.EMAIL, "TesT@TesT.org");
        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        Attributes profileAttributes = profile.getAttributes();
        assertEquals(attributes.get(UserModel.USERNAME).toLowerCase(), profileAttributes.getFirst(UserModel.USERNAME));
        assertEquals(attributes.get(UserModel.EMAIL).toLowerCase(), profileAttributes.getFirst(UserModel.EMAIL));
    }

    @Test
    public void testEmailAttributeInUpdateEmailContext() {
        ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, true);
        try {
            getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testEmailAttributeInUpdateEmailContext);
        } finally {
            ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, false);
        }
    }

    private static void testEmailAttributeInUpdateEmailContext(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        String userName = org.keycloak.models.utils.KeycloakModelUtils.generateId();
        Map<String, String> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, userName);
        attributes.put(UserModel.EMAIL, userName + "@keycloak.org");
        attributes.put(UserModel.FIRST_NAME, "Joe");
        attributes.put(UserModel.LAST_NAME, "Doe");

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();

        profile = provider.create(UserProfileContext.UPDATE_EMAIL, user);
        containsInAnyOrder(profile.getAttributes().nameSet(), UserModel.EMAIL);

        UPConfig upConfig = provider.getConfiguration();
        upConfig.addOrReplaceAttribute(new UPAttribute("foo", new UPAttributePermissions(Set.of(), Set.of(UserProfileConstants.ROLE_USER)), new UPAttributeRequired(Set.of(UserProfileConstants.ROLE_USER), Set.of())));
        provider.setConfiguration(upConfig);
        profile = provider.create(UserProfileContext.UPDATE_EMAIL, attributes, user);
        profile.update();

        upConfig = provider.getConfiguration();
        upConfig.getAttribute(UserModel.EMAIL).getValidations().put(LengthValidator.ID, Map.of("min", "1", "max", "2"));
        provider.setConfiguration(upConfig);
        profile = provider.create(UserProfileContext.UPDATE_EMAIL, attributes, user);
        try {
            profile.update();
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(UserModel.EMAIL));
            assertTrue(ve.hasError(LengthValidator.MESSAGE_INVALID_LENGTH));
        }

        RealmModel realm = session.getContext().getRealm();

        try {
            upConfig = UPConfigUtils.parseSystemDefaultConfig();
            upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
            provider.setConfiguration(upConfig);
            realm.setRegistrationEmailAsUsername(true);
            attributes.put(UserModel.EMAIL, "new@email.com");
            profile = provider.create(UserProfileContext.UPDATE_EMAIL, attributes, user);
            profile.update();
            assertEquals(attributes.get(UserModel.EMAIL), profile.getAttributes().getFirst(UserModel.EMAIL));
            assertEquals(attributes.get(UserModel.EMAIL), profile.getAttributes().getFirst(UserModel.USERNAME));
        } finally {
            realm.setRegistrationEmailAsUsername(false);
        }

        try {
            realm.setEditUsernameAllowed(false);
            attributes.put(UserModel.EMAIL, "other@email.com");
            profile = provider.create(UserProfileContext.UPDATE_EMAIL, attributes, user);
            profile.update();
            assertEquals(attributes.get(UserModel.EMAIL), profile.getAttributes().getFirst(UserModel.EMAIL));
            assertEquals("new@email.com", profile.getAttributes().getFirst(UserModel.USERNAME));
        } catch (ValidationException ve) {
            assertTrue(ve.isAttributeOnError(UserModel.USERNAME));
            assertTrue(ve.hasError(Messages.READ_ONLY_USERNAME));
        } finally {
            realm.setEditUsernameAllowed(true);
        }

        try {
            upConfig = UPConfigUtils.parseSystemDefaultConfig();
            upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
            provider.setConfiguration(upConfig);
            realm.setEditUsernameAllowed(false);
            realm.setRegistrationEmailAsUsername(true);
            attributes.put(UserModel.EMAIL, "other@email.com");
            profile = provider.create(UserProfileContext.UPDATE_EMAIL, attributes, user);
            profile.update();
            assertEquals(attributes.get(UserModel.EMAIL), profile.getAttributes().getFirst(UserModel.EMAIL));
            assertEquals(attributes.get(UserModel.EMAIL), profile.getAttributes().getFirst(UserModel.USERNAME));
        } finally {
            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(false);
        }
    }

    @Test
    public void testEmailAnnotationsInAccountContext() {
        ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, true);
        try {
            getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testEmailAnnotationsInAccountContext);
        } finally {
            ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, false);
        }
    }

    private static void testEmailAnnotationsInAccountContext(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        String userName = org.keycloak.models.utils.KeycloakModelUtils.generateId();
        Map<String, String> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, userName);
        String originalEmail = userName + "@keycloak.org";
        attributes.put(UserModel.EMAIL, originalEmail);
        attributes.put(UserModel.FIRST_NAME, "Joe");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put("address", "some address");

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();
        RealmModel realm = session.getContext().getRealm();

        try {
            realm.setEditUsernameAllowed(false);
            realm.setRegistrationEmailAsUsername(true);
            profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);
            assertFalse(ofNullable(profile.getAttributes().getAnnotations(UserModel.EMAIL)).orElse(Map.of()).containsKey("kc.required.action.supported"));
        } finally {
            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(false);
        }

        try {
            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(true);
            profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);
            assertThat(ofNullable(profile.getAttributes().getAnnotations(UserModel.EMAIL)).orElse(Map.of()).get("kc.required.action.supported"), is(true));
        } finally {
            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(false);
        }

        try {
            realm.setEditUsernameAllowed(false);
            realm.setRegistrationEmailAsUsername(false);
            UPConfig upConfig = provider.getConfiguration();
            UPAttribute attribute = upConfig.getAttribute(UserModel.EMAIL);
            attribute.setPermissions(new UPAttributePermissions(Set.of(ROLE_USER), Set.of(ROLE_ADMIN)));
            provider.setConfiguration(upConfig);
            profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);
            assertFalse(ofNullable(profile.getAttributes().getAnnotations(UserModel.EMAIL)).orElse(Map.of()).containsKey("kc.required.action.supported"));
        } finally {
            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(false);
        }

        try {
            realm.setEditUsernameAllowed(false);
            realm.setRegistrationEmailAsUsername(false);
            UPConfig upConfig = provider.getConfiguration();
            UPAttribute attribute = upConfig.getAttribute(UserModel.EMAIL);
            attribute.setPermissions(new UPAttributePermissions(Set.of(ROLE_USER), Set.of(ROLE_ADMIN, ROLE_USER)));
            provider.setConfiguration(upConfig);
            profile = provider.create(UserProfileContext.ACCOUNT, attributes, user);
            assertThat(ofNullable(profile.getAttributes().getAnnotations(UserModel.EMAIL)).orElse(Map.of()).get("kc.required.action.supported"), is(true));
        } finally {
            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(false);
        }
    }

    @Test
    public void testEmailFieldHiddenWhenEmptyAndReadOnlyWithUpdateEmailEnabled() {
        ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, true);
        try {
            getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testEmailFieldHiddenWhenEmptyAndReadOnlyWithUpdateEmailEnabled);
        } finally {
            ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, false);
        }
    }

    private static void testEmailFieldHiddenWhenEmptyAndReadOnlyWithUpdateEmailEnabled(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        String userName = org.keycloak.models.utils.KeycloakModelUtils.generateId();
        Map<String, String> attributes = new HashMap<>();
        
        // Enable UPDATE_EMAIL feature
        RealmModel realm = session.getContext().getRealm();
        realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.UPDATE_EMAIL.name()).setEnabled(true);
        
        // Create user without email
        attributes.put(UserModel.USERNAME, userName);
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        // Deliberately not setting email to test empty email scenario
        
        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();
        
        // Configure email as read-only for users (only admins can edit)
        UPConfig upConfig = provider.getConfiguration();
        UPAttribute emailAttr = upConfig.getAttribute(UserModel.EMAIL);
        if (emailAttr == null) {
            emailAttr = new UPAttribute(UserModel.EMAIL);
            upConfig.addOrReplaceAttribute(emailAttr);
        }
        emailAttr.setPermissions(new UPAttributePermissions(Set.of(), Set.of("admin")));
        provider.setConfiguration(upConfig);

        profile = provider.create(UserProfileContext.UPDATE_PROFILE, user);
        Map<String, List<String>> readableAttributes = profile.getAttributes().getReadable();
        
        // Email should NOT be visible in UPDATE_PROFILE context when empty and read-only
        assertFalse("Email field should be hidden when empty, read-only, and UPDATE_EMAIL is enabled",
                readableAttributes.containsKey(UserModel.EMAIL));
    }

    @Test
    public void testMultivalued() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testMultivalued);
    }

    @Test
    public void testDefaultValue() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testInvalidConfigDefaultValue);
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testDefaultValue);
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testNoDefaultValueForRootAttributes);
    }

    private static void testInvalidConfigDefaultValue(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig upConfig = UPConfigUtils.parseSystemDefaultConfig();
        provider.setConfiguration(upConfig);

        UPAttribute foo = new UPAttribute("foo", new UPAttributePermissions(Set.of(), Set.of(UserProfileConstants.ROLE_ADMIN)));
        foo.setDefaultValue("def");
        foo.setValidations(Map.of("length", Map.of("min", "5", "max", "15")));
        upConfig.addOrReplaceAttribute(foo);

        try {
            provider.setConfiguration(upConfig);
            fail("Should fail because default value is not reach min length");
        } catch (ComponentValidationException cve) {
            //ignore
        }
    }

    private static void testDefaultValue(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig upConfig = UPConfigUtils.parseSystemDefaultConfig();
        UPAttribute foo = new UPAttribute("foo", new UPAttributePermissions(Set.of(), Set.of(UserProfileConstants.ROLE_ADMIN)));
        foo.setDefaultValue("def");
        upConfig.addOrReplaceAttribute(foo);
        provider.setConfiguration(upConfig);

        String userName = org.keycloak.models.utils.KeycloakModelUtils.generateId();
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(UserModel.USERNAME, List.of(userName));
        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();
        List<String> actualValue = user.getAttributes().get("foo");
        List<String> expectedValue = List.of("def");
        assertThat(actualValue, Matchers.equalTo(expectedValue));
    }

    private static void testNoDefaultValueForRootAttributes(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig upConfig = UPConfigUtils.parseSystemDefaultConfig();
        upConfig.getAttribute(UserModel.USERNAME).setDefaultValue("def");
        upConfig.getAttribute(UserModel.EMAIL).setDefaultValue("def");
        upConfig.getAttribute(UserModel.FIRST_NAME).setDefaultValue("def");
        upConfig.getAttribute(UserModel.LAST_NAME).setDefaultValue("def");

        try {
            provider.setConfiguration(upConfig);
            fail("Should fail validation for default value");
        } catch (ComponentValidationException cve) {
            String message = cve.getMessage();
            for (String attributeName : List.of(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME)) {
                if (UserProfileUtil.isRootAttribute(attributeName)) {
                    assertThat(message, Matchers.containsString("Default value not supported for attribute '" + attributeName + "'"));
                }
            }
        }
    }

    private static void testMultivalued(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        UPConfig upConfig = UPConfigUtils.parseSystemDefaultConfig();
        provider.setConfiguration(upConfig);
        String userName = org.keycloak.models.utils.KeycloakModelUtils.generateId();
        Map<String, List<String>> attributes = new HashMap<>();

        attributes.put(UserModel.USERNAME, List.of(userName));
        attributes.put(UserModel.EMAIL, List.of(userName + "@keycloak.org"));
        attributes.put(UserModel.FIRST_NAME, List.of("Joe"));
        attributes.put(UserModel.LAST_NAME, List.of("Doe"));

        UserProfile profile = provider.create(UserProfileContext.USER_API, attributes);
        UserModel user = profile.create();
        profile = provider.create(UserProfileContext.USER_API, user);

        containsInAnyOrder(profile.getAttributes().nameSet(), UserModel.EMAIL);

        UPAttribute foo = new UPAttribute("foo", new UPAttributePermissions(Set.of(), Set.of(UserProfileConstants.ROLE_ADMIN)));
        upConfig.addOrReplaceAttribute(foo);
        provider.setConfiguration(upConfig);
        List<String> expectedValues = List.of("a", "b");
        attributes.put("foo", expectedValues);
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        try {
            profile.update();
            fail("Should fail because foo attribute is single-valued by default");
        } catch (ValidationException ve) {
            assertTrue(ve.hasError(MultiValueValidator.MESSAGE_INVALID_SIZE));
        }

        foo.setMultivalued(true);
        upConfig.addOrReplaceAttribute(foo);
        provider.setConfiguration(upConfig);
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update();
        List<String> actualValues = user.getAttributes().get("foo");
        assertThat(actualValues, Matchers.containsInAnyOrder(expectedValues.toArray()));

        attributes.put("foo", List.of("a", "b", "c"));
        foo.addValidation(MultiValueValidator.ID, Map.of(MultiValueValidator.KEY_MAX, 2));
        provider.setConfiguration(upConfig);
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        try {
            profile.update();
            fail("Should fail because foo attribute expects 2 values");
        } catch (ValidationException ve) {
            assertTrue(ve.hasError(MultiValueValidator.MESSAGE_INVALID_SIZE));
        }

        attributes.put("foo", List.of("a"));
        foo.addValidation(MultiValueValidator.ID, Map.of(MultiValueValidator.KEY_MIN, 2, MultiValueValidator.KEY_MAX, 2));
        provider.setConfiguration(upConfig);
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        try {
            profile.update();
            fail("Should fail because foo attribute expects at least 2 values");
        } catch (ValidationException ve) {
            assertTrue(ve.hasError(MultiValueValidator.MESSAGE_INVALID_SIZE));
        }

        attributes.put("foo", List.of("a", "b"));
        foo.addValidation(MultiValueValidator.ID, Map.of(MultiValueValidator.KEY_MIN, 2, MultiValueValidator.KEY_MAX, 2));
        provider.setConfiguration(upConfig);
        profile = provider.create(UserProfileContext.USER_API, attributes, user);
        profile.update();
    }

    @Test
    public void testDefaultConfigWhenComponentConfigIsNotSet() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UserProfileTest::testDefaultConfigWhenComponentConfigIsNotSet);
    }

    private static void testDefaultConfigWhenComponentConfigIsNotSet(KeycloakSession session) {
        UserProfileProvider provider = getUserProfileProvider(session);
        provider.setConfiguration(parseSystemDefaultConfig());
        RealmModel realm = session.getContext().getRealm();
        ComponentModel component = realm.getComponentsStream(realm.getId(), UserProfileProvider.class.getName()).findAny().get();
        component.setConfig(new MultivaluedHashMap<>());
        realm.updateComponent(component);
        provider.create(UserProfileContext.USER_API, Map.of());
    }
}
