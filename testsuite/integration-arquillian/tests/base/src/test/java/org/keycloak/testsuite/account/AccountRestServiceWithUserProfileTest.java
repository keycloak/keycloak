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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_EDITABLE;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_ONLY;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.account.UserProfileAttributeMetadata;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.userprofile.UserProfileContext;

/**
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE)
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class AccountRestServiceWithUserProfileTest extends AccountRestServiceTest {
    
    @Override
    @Before
    public void before() {
        super.before();
        enableDynamicUserProfile();
        setUserProfileConfiguration(null);
    }

    @Override
    protected boolean isDeclarativeUserProfile() {
        return true;
    }

    private static String UP_CONFIG_FOR_METADATA = "{\"attributes\": ["
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

    private static String UP_CONFIG_NO_ACCESS_TO_NAME_FIELDS = "{\"attributes\": ["
            + "{\"name\": \"firstName\"," + PERMISSIONS_ADMIN_ONLY + ", \"required\": {}, \"displayName\": \"${profile.firstName}\", \"validations\": {\"length\": { \"max\": 255 }}},"
            + "{\"name\": \"lastName\"," + PERMISSIONS_ADMIN_ONLY + ", \"required\": {}, \"displayName\": \"Last name\", \"annotations\": {\"formHintKey\" : \"userEmailFormFieldHint\", \"anotherKey\" : 10, \"yetAnotherKey\" : \"some value\"}},"
            + "{\"name\": \"attr_readonly\"," + PERMISSIONS_ADMIN_EDITABLE + "},"
            + "{\"name\": \"attr_no_permission\"," + PERMISSIONS_ADMIN_ONLY + "}"
            + "]}";

    private static String UP_CONFIG_RO_ACCESS_TO_NAME_FIELDS = "{\"attributes\": ["
            + "{\"name\": \"firstName\"," + PERMISSIONS_ADMIN_EDITABLE + ", \"required\": {}, \"displayName\": \"${profile.firstName}\", \"validations\": {\"length\": { \"max\": 255 }}},"
            + "{\"name\": \"lastName\"," + PERMISSIONS_ADMIN_EDITABLE + ", \"required\": {}, \"displayName\": \"Last name\", \"annotations\": {\"formHintKey\" : \"userEmailFormFieldHint\", \"anotherKey\" : 10, \"yetAnotherKey\" : \"some value\"}},"
            + "{\"name\": \"attr_readonly\"," + PERMISSIONS_ADMIN_EDITABLE + "},"
            + "{\"name\": \"attr_no_permission\"," + PERMISSIONS_ADMIN_ONLY + "}"
            + "]}";


    @Test
    @Override
    public void testGetUserProfileMetadata_EditUsernameAllowed() throws IOException {

        setUserProfileConfiguration(UP_CONFIG_FOR_METADATA);
        
        UserRepresentation user = getUser();
        assertNotNull(user.getUserProfileMetadata());
        
        assertUserProfileAttributeMetadata(user, "username", "${username}", true, false);
        assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);
        
        UserProfileAttributeMetadata uam = assertUserProfileAttributeMetadata(user, "firstName", "${profile.firstName}", false, false);
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
        
        assertUserProfileAttributeMetadata(user, "attr_required_by_scope", "attr_required_by_scope", false, false);
        
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
    @Override
    public void testGetUserProfileMetadata_EditUsernameDisallowed() throws IOException {
        
        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(false);
            adminClient.realm("test").update(realmRep);

            setUserProfileConfiguration(UP_CONFIG_FOR_METADATA);
            
            UserRepresentation user = getUser();
            assertNotNull(user.getUserProfileMetadata());
            
            assertUserProfileAttributeMetadata(user, "username", "${username}", true, true);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);
            
            UserProfileAttributeMetadata uam = assertUserProfileAttributeMetadata(user, "firstName", "${profile.firstName}", false, false);
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
            
            assertUserProfileAttributeMetadata(user, "attr_required_by_scope", "attr_required_by_scope", false, false);
            
            assertUserProfileAttributeMetadata(user, "attr_not_required_due_to_role", "attr_not_required_due_to_role", false, false);
            assertUserProfileAttributeMetadata(user, "attr_readonly", "attr_readonly", false, true);
            
            assertNull(getUserProfileAttributeMetadata(user, "attr_no_permission"));
        } finally {
            RealmRepresentation realmRep = testRealm().toRepresentation();
            realmRep.setEditUsernameAllowed(true);
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
        Map<String, List<String>> originalAttributes = new HashMap<>(user.getAttributes());

        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

            user.setEmail("bobby@localhost");
            user.setFirstName("Homer");
            user.setLastName("Simpsons");
            user.getAttributes().put("attr1", Collections.singletonList("val1"));
            user.getAttributes().put("attr2", Collections.singletonList("val2"));

            user = updateAndGet(user);

            //skip login to the REST API event
            events.poll();
            events.expectAccount(EventType.UPDATE_PROFILE).user(user.getId())
                .detail(Details.CONTEXT, UserProfileContext.ACCOUNT.name())
                .detail(Details.PREVIOUS_EMAIL, originalEmail)
                .detail(Details.UPDATED_EMAIL, "bobby@localhost")
                .detail(Details.PREVIOUS_FIRST_NAME, originalFirstName)
                .detail(Details.PREVIOUS_LAST_NAME, originalLastName)
                .detail(Details.UPDATED_FIRST_NAME, "Homer")
                .detail(Details.UPDATED_LAST_NAME, "Simpsons")
                .detail(Details.PREF_UPDATED+"attr2", "val2")
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
            SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
            System.out.println(response.asString());
            assertEquals(204, response.getStatus());
        }
    }
    
    @Test
    public void testUpdateProfileEvent() throws IOException {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"attr1\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\"," + PERMISSIONS_ALL + "}"
                + "]}");
        super.testUpdateProfileEvent();
    }
    
    @Test
    @Override
    public void testUpdateProfile() throws IOException {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"attr1\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\"," + PERMISSIONS_ALL + "}"
                + "]}");
        super.testUpdateProfile();
    }
    
    @Test
    @Override
    public void testUpdateSingleField() throws IOException {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}}"
                + "]}");
         super.testUpdateSingleField();
    }
    
    protected void setUserProfileConfiguration(String configuration) {
        VerifyProfileTest.setUserProfileConfiguration(testRealm(), configuration);
    }
   
    protected void enableDynamicUserProfile() {
        RealmRepresentation testRealm = testRealm().toRepresentation();
        
        VerifyProfileTest.enableDynamicUserProfile(testRealm);

        testRealm().update(testRealm);
    }

}
