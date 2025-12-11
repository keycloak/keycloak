/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.account;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.representations.idm.UserProfileMetadata;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.keycloak.userprofile.UserProfileContext;

import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.account.AccountRestServiceTest.assertUserProfileAttributeMetadata;
import static org.keycloak.testsuite.account.AccountRestServiceTest.getUserProfileAttributeMetadata;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.PERMISSIONS_ADMIN_EDITABLE;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.PERMISSIONS_ADMIN_ONLY;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.PERMISSIONS_ALL;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test account rest service with custom user profile configurations
 *
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class AccountRestServiceWithUserProfileTest extends AbstractRestServiceTest {
    
    @Override
    @Before
    public void before() {
        super.before();
        setUserProfileConfiguration(null);
    }

    private final static String UP_CONFIG_FOR_METADATA = "{\"attributes\": ["
            + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {\"scopes\":[\"profile\"]}, \"displayName\": \"${profile.firstName}\", \"validations\": {\"length\": { \"max\": 255 }}},"
            + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}, \"displayName\": \"Last name\", \"annotations\": {\"formHintKey\" : \"userEmailFormFieldHint\", \"anotherKey\" : 10, \"yetAnotherKey\" : \"some value\"}},"
            + "{\"name\": \"attr_with_scope_selector\"," + PERMISSIONS_ALL + ", \"selector\": {\"scopes\": [\"profile\"]}},"
            + "{\"name\": \"attr_required\"," + PERMISSIONS_ALL + ", \"required\": {}},"
            + "{\"name\": \"attr_required_by_role\"," + PERMISSIONS_ALL + ", \"required\": {\"roles\" : [\"user\"]}},"
            + "{\"name\": \"attr_required_by_scope\"," + PERMISSIONS_ALL + ", \"required\": {\"scopes\": [\"profile\"]}},"
            + "{\"name\": \"attr_not_required_due_to_role\"," + PERMISSIONS_ALL + ", \"required\": {\"roles\" : [\"admin\"]}},"
            + "{\"name\": \"attr_readonly\"," + PERMISSIONS_ADMIN_EDITABLE + "},"
            + "{\"name\": \"attr_no_permission\"," + PERMISSIONS_ADMIN_ONLY + "}"
            + "]}";

    private final static String UP_CONFIG_NO_ACCESS_TO_NAME_FIELDS = "{\"attributes\": ["
            + "{\"name\": \"firstName\"," + PERMISSIONS_ADMIN_ONLY + ", \"required\": {}, \"displayName\": \"${profile.firstName}\", \"validations\": {\"length\": { \"max\": 255 }}},"
            + "{\"name\": \"lastName\"," + PERMISSIONS_ADMIN_ONLY + ", \"required\": {}, \"displayName\": \"Last name\", \"annotations\": {\"formHintKey\" : \"userEmailFormFieldHint\", \"anotherKey\" : 10, \"yetAnotherKey\" : \"some value\"}},"
            + "{\"name\": \"attr_readonly\"," + PERMISSIONS_ADMIN_EDITABLE + "},"
            + "{\"name\": \"attr_no_permission\"," + PERMISSIONS_ADMIN_ONLY + "}"
            + "]}";

    private final static String UP_CONFIG_RO_ACCESS_TO_NAME_FIELDS = "{\"attributes\": ["
            + "{\"name\": \"firstName\"," + PERMISSIONS_ADMIN_EDITABLE + ", \"required\": {}, \"displayName\": \"${profile.firstName}\", \"validations\": {\"length\": { \"max\": 255 }}},"
            + "{\"name\": \"lastName\"," + PERMISSIONS_ADMIN_EDITABLE + ", \"required\": {}, \"displayName\": \"Last name\", \"annotations\": {\"formHintKey\" : \"userEmailFormFieldHint\", \"anotherKey\" : 10, \"yetAnotherKey\" : \"some value\"}},"
            + "{\"name\": \"attr_readonly\"," + PERMISSIONS_ADMIN_EDITABLE + "},"
            + "{\"name\": \"attr_no_permission\"," + PERMISSIONS_ADMIN_ONLY + "}"
            + "]}";

    private final static String UP_CONFIG_RO_USERNAME_AND_EMAIL = "{\"attributes\": ["
            + "{\"name\": \"email\"," + PERMISSIONS_ADMIN_EDITABLE + ", \"required\": {}, \"displayName\": \"${email}\", \"annotations\": {\"formHintKey\" : \"userEmailFormFieldHint\", \"anotherKey\" : 10, \"yetAnotherKey\" : \"some value\"}},"
            + "{\"name\": \"attr_readonly\"," + PERMISSIONS_ADMIN_EDITABLE + "},"
            + "{\"name\": \"attr_no_permission\"," + PERMISSIONS_ADMIN_ONLY + "}"
            + "]}";


    @Test
    public void testEditUsernameAllowed() throws IOException {
        setUserProfileConfiguration(UP_CONFIG_FOR_METADATA);
        
        UserRepresentation user = getUser();
        assertNotNull(user.getUserProfileMetadata());
        
        assertUserProfileAttributeMetadata(user, "username", "${username}", true, false);
        assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);
        
        UserProfileAttributeMetadata uam = assertUserProfileAttributeMetadata(user, "firstName", "${profile.firstName}", true, false);
        assertNull(uam.getAnnotations());
        Map<String, Object> vc = assertValidatorExists(uam, "length");
        assertEquals(255, vc.get("max"));
        
        uam = assertUserProfileAttributeMetadata(user, "lastName", "Last name", true, false);
        assertNotNull(uam.getAnnotations());
        assertEquals(3, uam.getAnnotations().size());
        assertAnnotationValue(uam, "formHintKey", "userEmailFormFieldHint");
        assertAnnotationValue(uam, "anotherKey", 10);
        
        assertUserProfileAttributeMetadata(user, "attr_with_scope_selector", "attr_with_scope_selector", false, false);
        
        assertUserProfileAttributeMetadata(user, "attr_required", "attr_required", true, false);
        assertUserProfileAttributeMetadata(user, "attr_required_by_role", "attr_required_by_role", true, false);
        
        assertUserProfileAttributeMetadata(user, "attr_required_by_scope", "attr_required_by_scope", true, false);
        
        assertUserProfileAttributeMetadata(user, "attr_not_required_due_to_role", "attr_not_required_due_to_role", false, false);
        assertUserProfileAttributeMetadata(user, "attr_readonly", "attr_readonly", false, true);
        
        assertNull(getUserProfileAttributeMetadata(user, "attr_no_permission"));
    }

    @Test
    public void testGetUserProfileMetadata_NoAccessToNameFields() throws IOException {

        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(false);
            adminClient.realm("test").update(realmRep);

            setUserProfileConfiguration(UP_CONFIG_NO_ACCESS_TO_NAME_FIELDS);

            UserRepresentation user = getUser();
            assertNotNull(user.getUserProfileMetadata());

            assertUserProfileAttributeMetadata(user, "username", "${username}", true, true);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);

            assertNull(getUserProfileAttributeMetadata(user, "firstName"));
            assertNull(getUserProfileAttributeMetadata(user, "lastName"));
            assertUserProfileAttributeMetadata(user, "attr_readonly", "attr_readonly", false, true);

            assertNull(getUserProfileAttributeMetadata(user, "attr_no_permission"));

        } finally {
            RealmRepresentation realmRep = testRealm().toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void testUpdateEmailLink() throws Exception {
        RealmResource realm = adminClient.realm("test");
        RealmRepresentation realmRep = realm.toRepresentation();
        ApiUtil.enableRequiredAction(realm, RequiredAction.UPDATE_EMAIL, true);

        try {
            realmRep.setEditUsernameAllowed(false);
            realm.update(realmRep);

            UserRepresentation user = getUser();
            assertNotNull(user.getUserProfileMetadata());
            assertThat(user.getUserProfileMetadata().getAttributeMetadata(UserModel.EMAIL).getAnnotations().get("kc.required.action.supported"), is(true));

            UPConfig upConfig = realm.users().userProfile().getConfiguration();
            UPAttribute attribute = upConfig.getAttribute(UserModel.EMAIL);
            attribute.setPermissions(new UPAttributePermissions(Set.of(ROLE_USER), Set.of(ROLE_ADMIN)));
            realm.users().userProfile().update(upConfig);
            user = getUser();
            assertNotNull(user.getUserProfileMetadata());
            assertThat(user.getUserProfileMetadata().getAttributeMetadata(UserModel.EMAIL).getAnnotations().get("kc.required.action.supported"), is(nullValue()));
        } finally {
            ApiUtil.enableRequiredAction(realm, RequiredAction.UPDATE_EMAIL, false);
            realmRep.setEditUsernameAllowed(true);
            realm.update(realmRep);
        }
    }

    @Test
    public void testGetUserProfileMetadata_RoAccessToNameFields() throws IOException {

        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(false);
            adminClient.realm("test").update(realmRep);

            setUserProfileConfiguration(UP_CONFIG_RO_ACCESS_TO_NAME_FIELDS);

            UserRepresentation user = getUser();
            assertNotNull(user.getUserProfileMetadata());

            assertUserProfileAttributeMetadata(user, "username", "${username}", true, true);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);

            assertUserProfileAttributeMetadata(user, "firstName", "${profile.firstName}", true, true);
            assertUserProfileAttributeMetadata(user, "lastName", "Last name", true, true);
            assertUserProfileAttributeMetadata(user, "attr_readonly", "attr_readonly", false, true);

            assertNull(getUserProfileAttributeMetadata(user, "attr_no_permission"));

        } finally {
            RealmRepresentation realmRep = testRealm().toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void testGetUserProfileMetadata_RoAccessToUsernameAndEmail() throws IOException {

        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(false);
            adminClient.realm("test").update(realmRep);

            setUserProfileConfiguration(UP_CONFIG_RO_USERNAME_AND_EMAIL);

            UserRepresentation user = getUser();
            assertNotNull(user.getUserProfileMetadata());

            assertUserProfileAttributeMetadata(user, "username", "${username}", true, true);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, true);

            assertUserProfileAttributeMetadata(user, "attr_readonly", "attr_readonly", false, true);
            assertNull(getUserProfileAttributeMetadata(user, "attr_no_permission"));
        } finally {
            RealmRepresentation realmRep = testRealm().toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            testRealm().update(realmRep);
        }
    }


    @Test
    public void testEditUsernameDisallowed() throws IOException {
        
        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(false);
            adminClient.realm("test").update(realmRep);

            setUserProfileConfiguration(UP_CONFIG_FOR_METADATA);
            
            UserRepresentation user = getUser();
            assertNotNull(user.getUserProfileMetadata());
            
            assertUserProfileAttributeMetadata(user, "username", "${username}", true, true);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);
            
            UserProfileAttributeMetadata uam = assertUserProfileAttributeMetadata(user, "firstName", "${profile.firstName}", true, false);
            assertNull(uam.getAnnotations());
            Map<String, Object> vc = assertValidatorExists(uam, "length");
            assertEquals(255, vc.get("max"));
            
            uam = assertUserProfileAttributeMetadata(user, "lastName", "Last name", true, false);
            assertNotNull(uam.getAnnotations());
            assertEquals(3, uam.getAnnotations().size());
            assertAnnotationValue(uam, "formHintKey", "userEmailFormFieldHint");
            assertAnnotationValue(uam, "anotherKey", 10);
            
            assertUserProfileAttributeMetadata(user, "attr_with_scope_selector", "attr_with_scope_selector", false, false);
            
            assertUserProfileAttributeMetadata(user, "attr_required", "attr_required", true, false);
            assertUserProfileAttributeMetadata(user, "attr_required_by_role", "attr_required_by_role", true, false);
            
            assertUserProfileAttributeMetadata(user, "attr_required_by_scope", "attr_required_by_scope", true, false);
            
            assertUserProfileAttributeMetadata(user, "attr_not_required_due_to_role", "attr_not_required_due_to_role", false, false);
            assertUserProfileAttributeMetadata(user, "attr_readonly", "attr_readonly", false, true);
            
            assertNull(getUserProfileAttributeMetadata(user, "attr_no_permission"));
        } finally {
            RealmRepresentation realmRep = testRealm().toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            realmRep.setRegistrationEmailAsUsername(false);
            testRealm().update(realmRep);
        }
    }
    
    protected void assertAnnotationValue(UserProfileAttributeMetadata uam, String key, Object value) {
        assertNotNull("Missing annotations for attribute " + uam.getName(), uam.getAnnotations());
        assertEquals("Unexpexted value of the "+key+" annotation for attribute " + uam.getName(), value, uam.getAnnotations().get(key));
    }

    protected Map<String, Object> assertValidatorExists(UserProfileAttributeMetadata uam, String validatorId) {
        assertNotNull("Missing validators for attribute " + uam.getName(), uam.getValidators());
        assertTrue("Missing validtor "+validatorId+" for attribute " + uam.getName(), uam.getValidators().containsKey(validatorId));
        return uam.getValidators().get(validatorId);
    }
    
    @Test
    public void testUpdateProfileEventWithAdditionalAttributesAuditing() throws IOException {
        
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"attr1\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\"," + PERMISSIONS_ALL + "}"
                + "]}");
        
        UserRepresentation user = getUser();
        String originalUsername = user.getUsername();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalEmail = user.getEmail();
        user.setAttributes(Optional.ofNullable(user.getAttributes()).orElse(new HashMap<>()));
        Map<String, List<String>> originalAttributes = new HashMap<>(user.getAttributes());

        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

            user.setEmail("bobby@localhost");
            user.setFirstName("Homer");
            user.setLastName("Simpsons");
            user.getAttributes().put("attr1", Collections.singletonList("val11"));
            user.getAttributes().put("attr2", Collections.singletonList("val22"));

            events.clear();
            user = updateAndGet(user);

            //skip login to the REST API event
            events.expectAccount(EventType.UPDATE_PROFILE).user(user.getId())
                .detail(Details.CONTEXT, UserProfileContext.ACCOUNT.name())
                .detail(Details.PREVIOUS_EMAIL, originalEmail)
                .detail(Details.UPDATED_EMAIL, "bobby@localhost")
                .detail(Details.PREVIOUS_FIRST_NAME, originalFirstName)
                .detail(Details.PREVIOUS_LAST_NAME, originalLastName)
                .detail(Details.UPDATED_FIRST_NAME, "Homer")
                .detail(Details.UPDATED_LAST_NAME, "Simpsons")
                .detail(Details.PREF_UPDATED+"attr2", "val22")
                .assertEvent();
            events.assertEmpty();
            
        } finally {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            adminClient.realm("test").update(realmRep);

            user.setUsername(originalUsername);
            user.setFirstName(originalFirstName);
            user.setLastName(originalLastName);
            user.setEmail(originalEmail);
            user.setAttributes(originalAttributes);
            SimpleHttpResponse response = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
            System.out.println(response.asString());
            assertEquals(204, response.getStatus());
        }
    }

    @Test
    public void testManageUserLocaleAttribute() throws IOException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Boolean internationalizationEnabled = realmRep.isInternationalizationEnabled();
        realmRep.setInternationalizationEnabled(false);
        testRealm().update(realmRep);
        UserRepresentation user = getUser();
        user.setAttributes(Optional.ofNullable(user.getAttributes()).orElse(new HashMap<>()));

        try {
            user.getAttributes().put(UserModel.LOCALE, List.of("pt_BR"));
            user = updateAndGet(user);
            assertNull(user.getAttributes());

            realmRep.setInternationalizationEnabled(true);
            testRealm().update(realmRep);

            user.singleAttribute(UserModel.LOCALE, "pt_BR");
            user = updateAndGet(user);
            assertEquals("pt_BR", user.getAttributes().get(UserModel.LOCALE).get(0));

            user.getAttributes().remove(UserModel.LOCALE);
            user = updateAndGet(user);
            assertNull(user.getAttributes());

            UserProfileMetadata metadata = user.getUserProfileMetadata();

            assertTrue(metadata.getAttributes().stream()
                    .map(UserProfileAttributeMetadata::getName)
                    .filter(UserModel.LOCALE::equals).findAny()
                    .isPresent()
            );
        } finally {
            realmRep.setInternationalizationEnabled(internationalizationEnabled);
            testRealm().update(realmRep);
            updateAndGet(user);
        }
    }

    protected void setUserProfileConfiguration(String configuration) {
        UserProfileUtil.setUserProfileConfiguration(testRealm(), configuration);
    }

    protected UserRepresentation getUser() throws IOException {
        return getUser(true);
    }

    protected UserRepresentation getUser(boolean fetchMetadata) throws IOException {
        String accountUrl = getAccountUrl(null) + "?userProfileMetadata=" + fetchMetadata;
        return AccountRestServiceTest.getUser(accountUrl, httpClient, tokenUtil);
    }

    protected UserRepresentation updateAndGet(UserRepresentation user) throws IOException {
        SimpleHttpRequest a = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user);
        try {
            assertEquals(204, a.asStatus());
        } catch (AssertionError e) {
            System.err.println("Error during user update: " + a.asString());
            throw e;
        }
        return getUser();
    }

}
