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
package org.keycloak.testsuite.account;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.DeleteCredentialAction;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.enums.AccountRestApiVersion;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.ConsentRepresentation;
import org.keycloak.representations.account.ConsentScopeRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.testsuite.AbstractAuthenticationTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.keycloak.userprofile.UserProfileContext;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.Header;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.PERMISSIONS_ALL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountRestServiceTest extends AbstractRestServiceTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    @Before
    public void before() {
        super.before();
        setUserProfileConfiguration(null);
    }

    @Test
    public void testEditUsernameAllowed() throws IOException {
        UserRepresentation user = getUser();
        String originalUsername = user.getUsername();
        String originalEmail = user.getEmail();
        RealmResource realm = adminClient.realm("test");
        RealmRepresentation realmRep = realm.toRepresentation();
        Boolean registrationEmailAsUsername = realmRep.isRegistrationEmailAsUsername();
        Boolean editUsernameAllowed = realmRep.isEditUsernameAllowed();

        try {
            realmRep.setRegistrationEmailAsUsername(false);
            realmRep.setEditUsernameAllowed(true);
            realm.update(realmRep);
            user = getUser();

            assertNotNull(user.getUserProfileMetadata());
            // can write both username and email
            assertUserProfileAttributeMetadata(user, "username", "${username}", true, false);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);
            assertUserProfileAttributeMetadata(user, "firstName", "${firstName}", true, false);
            assertUserProfileAttributeMetadata(user, "lastName", "${lastName}", true, false);

            user.setUsername("changed-username");
            user.setEmail("changed-email@keycloak.org");
            user = updateAndGet(user);
            assertEquals("changed-username", user.getUsername());
            assertEquals("changed-email@keycloak.org", user.getEmail());

            realmRep.setRegistrationEmailAsUsername(false);
            realmRep.setEditUsernameAllowed(false);
            realm.update(realmRep);
            user = getUser();

            assertNotNull(user.getUserProfileMetadata());
            // username is readonly but email is writable
            assertUserProfileAttributeMetadata(user, "username", "${username}", true, true);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);

            user.setUsername("should-not-change");
            user.setEmail("changed-email@keycloak.org");
            updateError(user, 400, Messages.READ_ONLY_USERNAME);

            realmRep.setRegistrationEmailAsUsername(true);
            realmRep.setEditUsernameAllowed(true);
            realm.update(realmRep);
            user = getUser();

            assertNotNull(user.getUserProfileMetadata());
            // username is read-only, not required, and is the same as email
            // but email is writable
            assertUserProfileAttributeMetadata(user, "username", "${username}", false, true);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);

            user.setUsername("should-be-the-email");
            user.setEmail("user@keycloak.org");
            user = updateAndGet(user);
            assertEquals("user@keycloak.org", user.getUsername());
            assertEquals("user@keycloak.org", user.getEmail());

            realmRep.setRegistrationEmailAsUsername(true);
            realmRep.setEditUsernameAllowed(false);
            realm.update(realmRep);
            user = getUser();

            assertNotNull(user.getUserProfileMetadata());
            // username is read-only and is the same as email, but email is read-only
            assertUserProfileAttributeMetadata(user, "username", "${username}", false, true);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, true);

            user.setUsername("should-be-the-email");
            user.setEmail("should-not-change@keycloak.org");
            user = updateAndGet(user);
            assertEquals("user@keycloak.org", user.getUsername());
            assertEquals("user@keycloak.org", user.getEmail());

            realmRep.setRegistrationEmailAsUsername(false);
            realmRep.setEditUsernameAllowed(true);
            realm.update(realmRep);
            user = getUser();
            user.setUsername("different-than-email");
            user.setEmail("user@keycloak.org");
            user = updateAndGet(user);
            assertEquals("different-than-email", user.getUsername());
            assertEquals("user@keycloak.org", user.getEmail());

            realmRep.setRegistrationEmailAsUsername(true);
            realmRep.setEditUsernameAllowed(false);
            realm.update(realmRep);
            user = getUser();
            user.setEmail("should-not-change@keycloak.org");
            user = updateAndGet(user);
            assertEquals("user@keycloak.org", user.getEmail());
            assertEquals(user.getEmail(), user.getUsername());
        } finally {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realmRep.setEditUsernameAllowed(editUsernameAllowed);
            realm.update(realmRep);
            user.setUsername(originalUsername);
            user.setEmail(originalEmail);
            updateAndGet(user);
        }
    }

    @Test
    public void testGetUserProfileWithoutMetadata() throws IOException {
        UserRepresentation user = getUser(false);
        assertNull(user.getUserProfileMetadata());
    }

    protected static UserProfileAttributeMetadata getUserProfileAttributeMetadata(UserRepresentation user, String attName) {
        if(user.getUserProfileMetadata() == null)
            return null;
        for(UserProfileAttributeMetadata uam : user.getUserProfileMetadata().getAttributes()) {
            if(attName.equals(uam.getName())) {
                return uam;
            }
        }
        return null;
    }

    protected static UserProfileAttributeMetadata assertUserProfileAttributeMetadata(UserRepresentation user, String attName, String displayName, boolean required, boolean readOnly) {
        UserProfileAttributeMetadata uam = getUserProfileAttributeMetadata(user, attName);

        assertNotNull(uam);
        assertEquals("Unexpected display name for attribute " + uam.getName(), displayName, uam.getDisplayName());
        assertEquals("Unexpected required flag for attribute " + uam.getName(), required, uam.isRequired());
        assertEquals("Unexpected readonly flag for attribute " + uam.getName(), readOnly, uam.isReadOnly());

        return uam;
    }


    @Test
    public void testGetProfile() throws IOException {

        UserRepresentation user = getUser();
        assertEquals("Tom", user.getFirstName());
        assertEquals("Brady", user.getLastName());
        assertEquals("test-user@localhost", user.getEmail());
        assertFalse(user.isEmailVerified());
        assertNull(user.getAttributes());
    }

    @Test
    public void testUpdateSingleField() throws IOException {
        String userProfileConfig = "{\"attributes\": ["
                + "{\"name\": \"email\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}}"
                + "]}";
        setUserProfileConfiguration(userProfileConfig);

        UserRepresentation user = getUser();
        String originalUsername = user.getUsername();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalEmail = user.getEmail();
        user.setAttributes(Optional.ofNullable(user.getAttributes()).orElse(new HashMap<>()));

        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

            user.setFirstName(null);
            user.setLastName("Bob");
            user.setEmail(null);
            user.getAttributes().clear();

            user = updateAndGet(user);

            assertEquals(user.getLastName(), "Bob");
            assertNull(user.getFirstName());
            assertNull(user.getEmail());

        } finally {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            adminClient.realm("test").update(realmRep);

            user.setUsername(originalUsername);
            user.setFirstName(originalFirstName);
            user.setLastName(originalLastName);
            user.setEmail(originalEmail);
            SimpleHttpResponse response = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
            System.out.println(response.asString());
            assertEquals(204, response.getStatus());
        }

    }

    /**
     * Reproducer for bugs KEYCLOAK-17424 and KEYCLOAK-17582
     */
    @Test
    public void testUpdateProfileEmailChangeSetsEmailVerified() throws IOException {
        UserRepresentation user = getUser();
        String originalEmail = user.getEmail();
        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

            //set flag over adminClient to initial value
            UserResource userResource = adminClient.realm("test").users().get(user.getId());
            org.keycloak.representations.idm.UserRepresentation ur = userResource.toRepresentation();
            ur.setEmailVerified(true);
            userResource.update(ur);
            //make sure flag is correct before the test
            user = getUser();
            assertEquals(true, user.isEmailVerified());

            // Update without email change - flag not reset to false
            user.setEmail(originalEmail);
            user = updateAndGet(user);
            assertEquals(originalEmail, user.getEmail());
            assertEquals(true, user.isEmailVerified());


            // Update email - flag must be reset to false
            user.setEmail("bobby@localhost");
            user = updateAndGet(user);
            assertEquals("bobby@localhost", user.getEmail());
            assertEquals(false, user.isEmailVerified());

        } finally {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            adminClient.realm("test").update(realmRep);

            user.setEmail(originalEmail);
            SimpleHttpResponse response = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
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

        UserRepresentation user = getUser();
        String originalUsername = user.getUsername();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalEmail = user.getEmail();
        assertNull(user.getAttributes());
        user.setAttributes(new HashMap<>());

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
            SimpleHttpResponse response = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
            System.out.println(response.asString());
            assertEquals(204, response.getStatus());
        }
    }

    @Test
    public void testUpdateProfile() throws IOException {
        String userProfileCfg = "{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"attr1\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\"," + PERMISSIONS_ALL + ", \"multivalued\": true}"
                + "]}";
        setUserProfileConfiguration(userProfileCfg);

        UserRepresentation user = getUser();
        String originalUsername = user.getUsername();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalEmail = user.getEmail();
        user.setAttributes(Optional.ofNullable(user.getAttributes()).orElse(new HashMap<>()));

        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

            user.setFirstName("Homer");
            user.setLastName("Simpsons");
            user.getAttributes().put("attr1", Collections.singletonList("val1"));
            user.getAttributes().put("attr2", Collections.singletonList("val2"));

            user = updateAndGet(user);

            assertEquals("Homer", user.getFirstName());
            assertEquals("Simpsons", user.getLastName());
            assertEquals(2, user.getAttributes().size());
            assertEquals(1, user.getAttributes().get("attr1").size());
            assertEquals("val1", user.getAttributes().get("attr1").get(0));
            assertEquals(1, user.getAttributes().get("attr2").size());
            assertEquals("val2", user.getAttributes().get("attr2").get(0));

            // Update attributes
            user.getAttributes().remove("attr1");
            user.getAttributes().get("attr2").add("val3");

            user = updateAndGet(user);

            assertEquals(1, user.getAttributes().size());
            assertEquals(2, user.getAttributes().get("attr2").size());
            assertThat(user.getAttributes().get("attr2"), containsInAnyOrder("val2", "val3"));

            // Update email
            user.setEmail("bobby@localhost");
            user = updateAndGet(user);
            assertEquals("bobby@localhost", user.getEmail());

            user.setEmail("john-doh@localhost");
            updateError(user, 409, Messages.EMAIL_EXISTS);

            user.setEmail("test-user@localhost");
            user = updateAndGet(user);
            assertEquals("test-user@localhost", user.getEmail());

            user.setUsername("john-doh@localhost");
            updateError(user, 409, Messages.USERNAME_EXISTS);

            user.setUsername("test-user@localhost");
            user = updateAndGet(user);
            assertEquals("test-user@localhost", user.getUsername());

            realmRep.setRegistrationEmailAsUsername(true);
            adminClient.realm("test").update(realmRep);

            user.setUsername("updatedUsername");
            user = updateAndGet(user);
            assertEquals("test-user@localhost", user.getUsername());

            user.setEmail("new@localhost");
            user = updateAndGet(user);
            assertEquals("new@localhost", user.getUsername());

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

            user.setUsername("updatedUsername");
            user = updateAndGet(user);
            assertThat("updatedusername", Matchers.equalTo(user.getUsername()));


            realmRep.setEditUsernameAllowed(false);
            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

            user.setUsername("updatedUsername2");
            updateError(user, 400, Messages.READ_ONLY_USERNAME);
        } finally {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            adminClient.realm("test").update(realmRep);

            user.setUsername(originalUsername);
            user.setFirstName(originalFirstName);
            user.setLastName(originalLastName);
            user.setEmail(originalEmail);
            SimpleHttpResponse response = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
            System.out.println(response.asString());
            assertEquals(204, response.getStatus());
        }

    }

    @Test
    public void testEmailReadableWhenEditUsernameDisabled() throws IOException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Boolean emailAsUsername = realmRep.isRegistrationEmailAsUsername();
        Boolean editUsernameAllowed = realmRep.isEditUsernameAllowed();
        realmRep.setRegistrationEmailAsUsername(true);
        realmRep.setEditUsernameAllowed(false);
        testRealm().update(realmRep);

        try {
            UserRepresentation user = getUser();
            String email = user.getEmail();
            assertNotNull(email);
            user = updateAndGet(user);
            assertEquals(email, user.getEmail());
        } finally {
            realmRep.setRegistrationEmailAsUsername(emailAsUsername);
            realmRep.setEditUsernameAllowed(editUsernameAllowed);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void testUpdateProfileCannotChangeThroughAttributes() throws IOException {
        UserRepresentation user = getUser();
        String originalUsername = user.getUsername();
        user.setAttributes(Optional.ofNullable(user.getAttributes()).orElse(new HashMap<>()));
        Map<String, List<String>> originalAttributes = new HashMap<>(user.getAttributes());

        try {
            user.getAttributes().put("username", Collections.singletonList("Username"));
            user.getAttributes().put("attr2", Collections.singletonList("val2"));

            user = updateAndGet(user);

            assertEquals(user.getUsername(), originalUsername);
        } finally {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            adminClient.realm("test").update(realmRep);

            user.setUsername(originalUsername);
            user.setAttributes(originalAttributes);
            SimpleHttpResponse response = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
            System.out.println(response.asString());
            assertEquals(204, response.getStatus());
        }
    }

    // KEYCLOAK-7572
    @Test
    public void testUpdateProfileWithRegistrationEmailAsUsername() throws IOException {
        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        realmRep.setRegistrationEmailAsUsername(true);
        adminClient.realm("test").update(realmRep);

        UserRepresentation user = getUser();
        String originalFirstname = user.getFirstName();

        try {
            user.setFirstName("Homer1");

            user = updateAndGet(user);

            assertEquals("Homer1", user.getFirstName());
        } finally {
            user.setFirstName(originalFirstname);
            int status = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asStatus();
            assertEquals(204, status);
        }
    }

    @Test
    public void testCors() throws IOException {
        String accountUrl = getAccountUrl(null);
        SimpleHttpRequest a = SimpleHttpDefault.doGet(accountUrl + "/linked-accounts", httpClient).auth(tokenUtil.getToken())
                .header("Origin", "http://localtest.me:8180")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (SimpleHttpResponse response = a.asResponse()) {
            Set<String> expected = new HashSet<>();
            Header[] actual = response.getAllHeaders();

            for (Header header : actual) {
                assertTrue(expected.add(header.getName()));
            }

            assertThat(expected, Matchers.hasItems(Cors.ACCESS_CONTROL_ALLOW_ORIGIN, Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        }

    }

    protected UserRepresentation getUser() throws IOException {
        return getUser(true);
    }

    protected UserRepresentation getUser(boolean fetchMetadata) throws IOException {
        String accountUrl = getAccountUrl(null) + "?userProfileMetadata=" + fetchMetadata;
        return getUser(accountUrl, httpClient, tokenUtil);
    }

    protected static UserRepresentation getUser(String accountUrl, CloseableHttpClient httpClient, TokenUtil tokenUtil) throws IOException {
        SimpleHttpRequest a = SimpleHttpDefault.doGet(accountUrl, httpClient).auth(tokenUtil.getToken());

        try {
            return a.asJson(UserRepresentation.class);
        } catch (IOException e) {
            System.err.println("Error during user reading: " + a.asString());
            throw e;
        }
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


    protected void updateError(UserRepresentation user, int expectedStatus, String expectedMessage) throws IOException {
        SimpleHttpResponse response = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
        assertEquals(expectedStatus, response.getStatus());
        ErrorRepresentation errorRep = response.asJson(ErrorRepresentation.class);
        List<ErrorRepresentation> errors = errorRep.getErrors();

        if (errors == null) {
            assertEquals(expectedMessage, errorRep.getErrorMessage());
        } else {
            assertThat(errors.stream().map(ErrorRepresentation::getErrorMessage)
                    .filter(expectedMessage::equals).collect(Collectors.toList()), containsInAnyOrder(expectedMessage));
        }
    }

    @Test
    public void testProfilePermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");

        // Read with no access
        assertEquals(403, SimpleHttpDefault.doGet(getAccountUrl(null), httpClient).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());

        // Update with no access
        assertEquals(403, SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(noaccessToken.getToken()).json(new UserRepresentation()).asStatus());

        // Update with read only
        assertEquals(403, SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(viewToken.getToken()).json(new UserRepresentation()).asStatus());
    }

    @Test
    public void testUpdateProfilePermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        int status = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus();
        assertEquals(403, status);

        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        status = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient).header("Accept", "application/json").auth(viewToken.getToken()).asStatus();
        assertEquals(200, status);
    }

    @Test
    public void testCredentialsGet() throws IOException {
        configureBrowserFlowWithWebAuthnAuthenticator("browser-webauthn");

        // Register requiredActions for WebAuthn and WebAuthn Passwordless
        RequiredActionProviderSimpleRepresentation requiredAction = new RequiredActionProviderSimpleRepresentation();
        requiredAction.setId("12345");
        requiredAction.setName(WebAuthnRegisterFactory.PROVIDER_ID);
        requiredAction.setProviderId(WebAuthnRegisterFactory.PROVIDER_ID);

        try {
            testRealm().flows().registerRequiredAction(requiredAction);
        } catch (ClientErrorException e) {
            assertThat(e.getResponse(), notNullValue());
            assertThat(e.getResponse().getStatus(), is(409));
        }

        getCleanup().addRequiredAction(requiredAction.getProviderId());

        requiredAction = new RequiredActionProviderSimpleRepresentation();
        requiredAction.setId("6789");
        requiredAction.setName(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        requiredAction.setProviderId(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);

        try {
            testRealm().flows().registerRequiredAction(requiredAction);
        } catch (ClientErrorException e) {
            assertThat(e.getResponse(), notNullValue());
            assertThat(e.getResponse().getStatus(), is(409));
        }

        getCleanup().addRequiredAction(requiredAction.getProviderId());

        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

        Assert.assertEquals(4, credentials.size());

        AccountCredentialResource.CredentialContainer password = credentials.get(0);
        assertCredentialContainerExpected(password, PasswordCredentialModel.TYPE, CredentialTypeMetadata.Category.BASIC_AUTHENTICATION.toString(),
                "password-display-name", "password-help-text", "kcAuthenticatorPasswordClass",
                null, UserModel.RequiredAction.UPDATE_PASSWORD.toString(), false, 1);

        CredentialRepresentation password1 = password.getUserCredentialMetadatas().get(0).getCredential();
        assertNull(password1.getSecretData());
        Assert.assertNotNull(password1.getCredentialData());

        AccountCredentialResource.CredentialContainer otp = credentials.get(1);
        assertCredentialContainerExpected(otp, OTPCredentialModel.TYPE, CredentialTypeMetadata.Category.TWO_FACTOR.toString(),
                "otp-display-name", "otp-help-text", "kcAuthenticatorOTPClass",
                UserModel.RequiredAction.CONFIGURE_TOTP.toString(), null, true, 0);

        // WebAuthn credentials will be returned, but createAction will be still null because requiredAction "webauthn register" not yet registered
        AccountCredentialResource.CredentialContainer webauthn = credentials.get(2);
        assertCredentialContainerExpected(webauthn, WebAuthnCredentialModel.TYPE_TWOFACTOR, CredentialTypeMetadata.Category.TWO_FACTOR.toString(),
                "webauthn-display-name", "webauthn-help-text", "kcAuthenticatorWebAuthnClass",
                WebAuthnRegisterFactory.PROVIDER_ID, null, true, 0);

        AccountCredentialResource.CredentialContainer webauthnPasswordless = credentials.get(3);
        assertCredentialContainerExpected(webauthnPasswordless, WebAuthnCredentialModel.TYPE_PASSWORDLESS, CredentialTypeMetadata.Category.PASSWORDLESS.toString(),
                "webauthn-passwordless-display-name", "webauthn-passwordless-help-text", "kcAuthenticatorWebAuthnPasswordlessClass",
                WebAuthnPasswordlessRegisterFactory.PROVIDER_ID, null, true, 0);

        // disable WebAuthn passwordless required action. User doesn't have WebAuthnPasswordless credential, so WebAuthnPasswordless credentialType won't be returned
        setRequiredActionEnabledStatus(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID, false);

        credentials = getCredentials();
        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE, WebAuthnCredentialModel.TYPE_TWOFACTOR);

        // Test that WebAuthn won't be returned when removed from the authentication flow
        removeWebAuthnFlow("browser-webauthn");

        credentials = getCredentials();

        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE);

        // Test password-only
        credentials = SimpleHttpDefault.doGet(getAccountUrl("credentials?" + AccountCredentialResource.TYPE + "=password"), httpClient)
                .auth(tokenUtil.getToken()).asJson(new TypeReference<List<AccountCredentialResource.CredentialContainer>>() {});
        Assert.assertEquals(1, credentials.size());
        password = credentials.get(0);
        Assert.assertEquals(PasswordCredentialModel.TYPE, password.getType());
        Assert.assertEquals(1, password.getUserCredentialMetadatas().size());

        // Test password-only and user-credentials
        credentials = SimpleHttpDefault.doGet(getAccountUrl("credentials?" + AccountCredentialResource.TYPE + "=password&" +
                                                            AccountCredentialResource.USER_CREDENTIALS + "=false"), httpClient)
                .auth(tokenUtil.getToken()).asJson(new TypeReference<List<AccountCredentialResource.CredentialContainer>>() {});
        Assert.assertEquals(1, credentials.size());
        password = credentials.get(0);
        Assert.assertEquals(PasswordCredentialModel.TYPE, password.getType());
        assertNull(password.getUserCredentialMetadatas());
    }


    @Test
    public void testCRUDCredentialOfDifferentUser() throws IOException {
        // Get credential ID of the OTP credential of the different user thant currently logged user
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "user-with-one-configured-otp");
        CredentialRepresentation otpCredential = user.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();

        // Test that current user can't update the credential, which belongs to the different user
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doPut(getAccountUrl("credentials/" + otpCredential.getId() + "/label"), httpClient)
                .auth(tokenUtil.getToken())
                .json("new-label")
                .asResponse()) {
            assertEquals(404, response.getStatus());
        }

        // Test that current user can't delete the credential, which belongs to the different user
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doDelete(getAccountUrl("credentials/" + otpCredential.getId()), httpClient)
                .acceptJson()
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(404, response.getStatus());
        }

        // Assert credential was not updated or removed
        CredentialRepresentation otpCredentialLoaded = user.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();
        Assert.assertTrue(ObjectUtil.isEqualOrBothNull(otpCredential.getUserLabel(), otpCredentialLoaded.getUserLabel()));
    }

    @Test
    public void testRemoveCredentialWithNonOtpCredentialTriggeringNoEvent() throws IOException {

        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        assertEquals(1, user.credentials().size());

        // Add non-OTP credential to the user through admin REST API
        CredentialRepresentation nonOtpCredential = ModelToRepresentation.toRepresentation(
                WebAuthnCredentialModel.create(WebAuthnCredentialModel.TYPE_TWOFACTOR, "foo", "foo", "foo", "foo", "foo", 2L, "foo"));
        org.keycloak.representations.idm.UserRepresentation userRep = UserBuilder.edit(user.toRepresentation())
                .secret(nonOtpCredential)
                .build();
        user.update(userRep);

        credentials = getCredentials();
        Assert.assertEquals(2, credentials.size());
        Assert.assertTrue(credentials.get(1).isRemoveable());

        // Remove credential
        CredentialRepresentation credential = user.credentials().stream()
                .filter(credentialRep -> WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(credentialRep.getType()))
                .findFirst()
                .get();
        Assert.assertNotNull(credential);
        user.removeCredential(credential.getId());

        events.poll();
        events.assertEmpty();
    }

    @Test
    public void testRemoveCredentialWithOtpCredentialTriggeringEvent() throws IOException {

        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        assertEquals(1, user.credentials().size());

        // Add OTP credential to the user through admin REST API
        org.keycloak.representations.idm.UserRepresentation userRep = UserBuilder.edit(user.toRepresentation())
                .totpSecret("totpSecret")
                .build();
        userRep.getCredentials().get(0).setUserLabel("totpCredentialUserLabel");
        user.update(userRep);

        credentials = getCredentials();
        Assert.assertEquals(2, credentials.size());
        Assert.assertTrue(credentials.get(1).isRemoveable());

        // Remove credential
        CredentialRepresentation otpCredential = user.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doDelete(getAccountUrl("credentials/" + otpCredential.getId()), httpClient)
                .acceptJson()
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(204, response.getStatus());
        }

        events.poll();
        events.expect(EventType.REMOVE_TOTP)
                .client("account")
                .user(user.toRepresentation().getId())
                .detail(Details.SELECTED_CREDENTIAL_ID, otpCredential.getId())
                .detail(Details.CREDENTIAL_USER_LABEL, "totpCredentialUserLabel")
                .detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE)
                .assertEvent();
        events.expect(EventType.REMOVE_CREDENTIAL)
                .client("account")
                .user(user.toRepresentation().getId())
                .detail(Details.SELECTED_CREDENTIAL_ID, otpCredential.getId())
                .detail(Details.CREDENTIAL_USER_LABEL, "totpCredentialUserLabel")
                .detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE)
                .assertEvent();
        events.assertEmpty();
    }

    // Send REST request to get all credential containers and credentials of current user
    private List<AccountCredentialResource.CredentialContainer> getCredentials() throws IOException {
        return SimpleHttpDefault.doGet(getAccountUrl("credentials"), httpClient)
                .auth(tokenUtil.getToken()).asJson(new TypeReference<List<AccountCredentialResource.CredentialContainer>>() {});
    }

    @Test
    public void testCredentialsGetDisabledOtp() throws IOException {
        // Disable OTP in all built-in flows

        // Disable parent subflow - that should treat OTP execution as disabled too
        AuthenticationExecutionModel.Requirement currentBrowserReq = setExecutionRequirement(DefaultAuthenticationFlows.BROWSER_FLOW,
                "Browser - Conditional 2FA", AuthenticationExecutionModel.Requirement.DISABLED);

        // Disable OTP directly in first-broker-login and direct-grant
        AuthenticationExecutionModel.Requirement currentFBLReq = setExecutionRequirement(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW,
                "OTP Form", AuthenticationExecutionModel.Requirement.DISABLED);
        AuthenticationExecutionModel.Requirement currentDirectGrantReq = setExecutionRequirement(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW,
                "Direct Grant - Conditional OTP", AuthenticationExecutionModel.Requirement.DISABLED);
        try {
            // Test that OTP credential is not included. Only password
            List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

            Assert.assertEquals(1, credentials.size());
            Assert.assertEquals(PasswordCredentialModel.TYPE, credentials.get(0).getType());

            // Enable browser subflow. OTP should be available then
            setExecutionRequirement(DefaultAuthenticationFlows.BROWSER_FLOW,
                    "Browser - Conditional 2FA", currentBrowserReq);
            credentials = getCredentials();
            Assert.assertEquals(2, credentials.size());
            Assert.assertEquals(OTPCredentialModel.TYPE, credentials.get(1).getType());

            // Disable browser subflow and enable FirstBrokerLogin. OTP should be available then
            setExecutionRequirement(DefaultAuthenticationFlows.BROWSER_FLOW,
                    "Browser - Conditional 2FA", AuthenticationExecutionModel.Requirement.DISABLED);
            setExecutionRequirement(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW,
                    "OTP Form", currentFBLReq);
            credentials = getCredentials();
            Assert.assertEquals(2, credentials.size());
            Assert.assertEquals(OTPCredentialModel.TYPE, credentials.get(1).getType());
        } finally {
            // Revert flows
            setExecutionRequirement(DefaultAuthenticationFlows.BROWSER_FLOW,
                    "Browser - Conditional 2FA", currentBrowserReq);
            setExecutionRequirement(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW,
                    "Direct Grant - Conditional OTP", currentDirectGrantReq);
        }
    }

    @Test
    public void testCredentialsGetWithDisabledOtpRequiredAction() throws IOException {
        // Assert OTP will be returned by default
        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();
        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE);

        // Disable OTP required action
        setRequiredActionEnabledStatus(UserModel.RequiredAction.CONFIGURE_TOTP.name(), false);

        // Assert OTP won't be returned
        credentials = getCredentials();
        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE);

        // Add OTP credential to the user through admin REST API
        UserResource adminUserResource = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        org.keycloak.representations.idm.UserRepresentation userRep = UserBuilder.edit(adminUserResource.toRepresentation())
                .totpSecret("abcdefabcdef")
                .build();
        adminUserResource.update(userRep);

        // Assert OTP will be returned without requiredAction
        credentials = getCredentials();
        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE);
        AccountCredentialResource.CredentialContainer otpCredential = credentials.get(1);
        assertNull(otpCredential.getCreateAction());
        assertNull(otpCredential.getUpdateAction());
        assertTrue(otpCredential.isRemoveable());

        String otpCredentialId = otpCredential.getUserCredentialMetadatas().get(0).getCredential().getId();

        // remove credential using account console as otp is removable
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doDelete(getAccountUrl("credentials/" + otpCredentialId), httpClient)
                .acceptJson()
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(204, response.getStatus());
        }

        // Revert - re-enable requiredAction
        setRequiredActionEnabledStatus(UserModel.RequiredAction.CONFIGURE_TOTP.name(), true);
    }

    // Issue 30204
    @Test
    public void testCredentialsGetWithDisabledDeleteCredentialAction() throws IOException {
        // Assert OTP will be returned by default
        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();
        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE);

        // Assert OTP removeable
        AccountCredentialResource.CredentialContainer otpCredential = credentials.get(1);
        assertTrue(otpCredential.isRemoveable());

        // Disable "Delete credential" action
        setRequiredActionEnabledStatus(DeleteCredentialAction.PROVIDER_ID, false);

        // Assert OTP not removeable
        credentials = getCredentials();
        otpCredential = credentials.get(1);
        assertFalse(otpCredential.isRemoveable());

        // Revert - re-enable requiredAction
        setRequiredActionEnabledStatus(DeleteCredentialAction.PROVIDER_ID, true);
    }

    private void setRequiredActionEnabledStatus(String requiredActionProviderId, boolean enabled) {
        RequiredActionProviderRepresentation requiredActionRep = testRealm().flows().getRequiredAction(requiredActionProviderId);
        requiredActionRep.setEnabled(enabled);
        testRealm().flows().updateRequiredAction(requiredActionProviderId, requiredActionRep);
    }

    private void assertExpectedCredentialTypes(List<AccountCredentialResource.CredentialContainer> credentialTypes, String... expectedCredentialTypes) {
        Assert.assertEquals(credentialTypes.size(), expectedCredentialTypes.length);
        int i = 0;
        for (AccountCredentialResource.CredentialContainer credential : credentialTypes) {
            Assert.assertEquals(credential.getType(), expectedCredentialTypes[i]);
            i++;
        }
    }

    @Test
    public void testCredentialsForUserWithoutPassword() throws IOException {
        // This is just to call REST to ensure tokenUtil will authenticate user and create the tokens.
        // We won't be able to authenticate later as user won't have password
        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

        // delete password should fail as it is not removable
        AccountCredentialResource.CredentialContainer password = credentials.get(0);
        assertCredentialContainerExpected(password, PasswordCredentialModel.TYPE, CredentialTypeMetadata.Category.BASIC_AUTHENTICATION.toString(),
                "password-display-name", "password-help-text", "kcAuthenticatorPasswordClass",
                null, UserModel.RequiredAction.UPDATE_PASSWORD.toString(), false, 1);
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doDelete(getAccountUrl("credentials/" + password.getUserCredentialMetadatas().get(0).getCredential().getId()), httpClient)
                .acceptJson()
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(400, response.getStatus());
            Assert.assertEquals("Credential type cannot be removed", response.asJson(OAuth2ErrorRepresentation.class).getError());
        }

        // Remove password from the user now
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        for (CredentialRepresentation credential : user.credentials()) {
            if (PasswordCredentialModel.TYPE.equals(credential.getType())) {
                user.removeCredential(credential.getId());
            }
        }

        // Get credentials. Ensure user doesn't have password credential and create action is UPDATE_PASSWORD
        credentials = getCredentials();
        password = credentials.get(0);
        assertCredentialContainerExpected(password, PasswordCredentialModel.TYPE, CredentialTypeMetadata.Category.BASIC_AUTHENTICATION.toString(),
                "password-display-name", "password-help-text", "kcAuthenticatorPasswordClass",
                UserModel.RequiredAction.UPDATE_PASSWORD.toString(), null, false, 0);

        // Re-add the password to the user
        ApiUtil.resetUserPassword(user, "password", false);

    }

    // Sets new requirement and returns current requirement
    private AuthenticationExecutionModel.Requirement setExecutionRequirement(String flowAlias, String executionDisplayName, AuthenticationExecutionModel.Requirement newRequirement) {
        List<AuthenticationExecutionInfoRepresentation> executionInfos = testRealm().flows().getExecutions(flowAlias);
        for (AuthenticationExecutionInfoRepresentation exInfo : executionInfos) {
            if (executionDisplayName.equals(exInfo.getDisplayName())) {
                AuthenticationExecutionModel.Requirement currentRequirement = AuthenticationExecutionModel.Requirement.valueOf(exInfo.getRequirement());
                exInfo.setRequirement(newRequirement.toString());
                testRealm().flows().updateExecutions(flowAlias, exInfo);
                return currentRequirement;
            }
        }

        throw new IllegalStateException("Not found execution '" + executionDisplayName + "' in flow '" + flowAlias + "'.");
    }

    private void configureBrowserFlowWithWebAuthnAuthenticator(String newFlowAlias) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("newName", newFlowAlias);
        Response response = testRealm().flows().copy("browser", params);
        response.close();
        String flowId = AbstractAuthenticationTest.findFlowByAlias(newFlowAlias, testRealm().flows().getFlows()).getId();

        AuthenticationExecutionRepresentation execution = new AuthenticationExecutionRepresentation();
        execution.setParentFlow(flowId);
        execution.setAuthenticator(WebAuthnAuthenticatorFactory.PROVIDER_ID);
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString());
        response = testRealm().flows().addExecution(execution);
        response.close();

        execution = new AuthenticationExecutionRepresentation();
        execution.setParentFlow(flowId);
        execution.setAuthenticator( WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID);
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());
        response = testRealm().flows().addExecution(execution);
        response.close();
    }

    private void removeWebAuthnFlow(String flowToDeleteAlias) {
        List<AuthenticationFlowRepresentation> flows = testRealm().flows().getFlows();
        AuthenticationFlowRepresentation flowRepresentation = AbstractAuthenticationTest.findFlowByAlias(flowToDeleteAlias, flows);
        testRealm().flows().deleteFlow(flowRepresentation.getId());
    }

    private void assertCredentialContainerExpected(AccountCredentialResource.CredentialContainer credential, String type, String category, String displayName, String helpText, String iconCssClass,
                                                   String createAction, String updateAction, boolean removeable, int userCredentialsCount) {
        Assert.assertEquals(type, credential.getType());
        Assert.assertEquals(category, credential.getCategory());
        Assert.assertEquals(displayName, credential.getDisplayName());
        Assert.assertEquals(helpText, credential.getHelptext());
        Assert.assertEquals(iconCssClass, credential.getIconCssClass());
        Assert.assertEquals(createAction, credential.getCreateAction());
        Assert.assertEquals(updateAction, credential.getUpdateAction());
        Assert.assertEquals(removeable, credential.isRemoveable());
        Assert.assertEquals(userCredentialsCount, credential.getUserCredentialMetadatas().size());
    }

    public void testDeleteSessions() throws IOException {
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        oauth.doLogin("view-account-access", "password");
        List<SessionRepresentation> sessions = SimpleHttpDefault.doGet(getAccountUrl("sessions"), httpClient).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(2, sessions.size());
        int status = SimpleHttpDefault.doDelete(getAccountUrl("sessions?current=false"), httpClient).acceptJson().auth(viewToken.getToken()).asStatus();
        assertEquals(200, status);
        sessions = SimpleHttpDefault.doGet(getAccountUrl("sessions"), httpClient).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(1, sessions.size());
    }

    @Test
    public void listApplications() throws Exception {
        oauth.client("in-use-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("view-applications-access", "password");
        assertNull(tokenResponse.getErrorDescription());

        TokenUtil token = new TokenUtil("view-applications-access", "password");
        List<ClientRepresentation> applications = SimpleHttpDefault
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        assertThat(apps.keySet(), containsInAnyOrder("in-use-client", "always-display-client", "direct-grant"));

        assertClientRep(apps.get("in-use-client"), "In Use Client", null, false, true, false, null, inUseClientAppUri);
        assertClientRep(apps.get("always-display-client"), "Always Display Client", null, false, false, false, null, alwaysDisplayClientAppUri);
        assertClientRep(apps.get("direct-grant"), null, null, false, true, false, null, null);
    }

    @Test
    public void listApplicationsFiltered() throws Exception {
        oauth.client("in-use-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("view-applications-access", "password");
        assertNull(tokenResponse.getErrorDescription());

        TokenUtil token = new TokenUtil("view-applications-access", "password");
        List<ClientRepresentation> applications = SimpleHttpDefault
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .param("name", "In Use")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        assertThat(apps.keySet(), containsInAnyOrder("in-use-client"));

        assertClientRep(apps.get("in-use-client"), "In Use Client", null, false, true, false, null, inUseClientAppUri);
    }

    @Test
    public void listApplicationsOfflineAccess() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse offlineTokenResponse = oauth.doPasswordGrantRequest("view-applications-access", "password");
        assertNull(offlineTokenResponse.getErrorDescription());

        oauth.client("offline-client-without-base-url", "secret1");
        offlineTokenResponse = oauth.doPasswordGrantRequest("view-applications-access", "password");
        assertNull(offlineTokenResponse.getErrorDescription());

        TokenUtil token = new TokenUtil("view-applications-access", "password");
        List<ClientRepresentation> applications = SimpleHttpDefault
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        assertThat(apps.keySet(), containsInAnyOrder("offline-client", "offline-client-without-base-url", "always-display-client", "direct-grant"));

        assertClientRep(apps.get("offline-client"), "Offline Client", null, false, false, true, null, offlineClientAppUri);
        assertClientRep(apps.get("offline-client-without-base-url"), "Offline Client Without Base URL", null, false, false, true, null, null);
    }

    @Test
    public void listApplicationsThirdPartyWithoutConsentText() throws Exception {
        listApplicationsThirdParty("acr", false);
    }

    @Test
    public void listApplicationsThirdPartyWithConsentText() throws Exception {
        listApplicationsThirdParty("profile", true);
    }

    public void listApplicationsThirdParty(String clientScopeName, boolean expectConsentTextAsName) throws Exception {
        String appId = "third-party";
        TokenUtil token = new TokenUtil("view-applications-access", "password");

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().stream()
                .filter(s -> s.getName().equals(clientScopeName))
                .findFirst().get();
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));
        SimpleHttpDefault
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);

        List<ClientRepresentation> applications = SimpleHttpDefault
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        SimpleHttpDefault
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse().close();

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        assertThat(apps.keySet(), containsInAnyOrder(appId, "always-display-client", "direct-grant"));

        ClientRepresentation app = apps.get(appId);
        assertClientRep(app, null, "A third party application", true, false, false, null, "http://localhost:8180/auth/realms/master/app/auth");
        assertFalse(app.getConsent().getGrantedScopes().isEmpty());
        ConsentScopeRepresentation grantedScope = app.getConsent().getGrantedScopes().get(0);
        assertEquals(clientScopeRepresentation.getId(), grantedScope.getId());

        if (expectConsentTextAsName) {
            assertEquals(clientScopeRepresentation.getAttributes().get(ClientScopeModel.CONSENT_SCREEN_TEXT), grantedScope.getName());
        }
        else {
            assertEquals(clientScopeRepresentation.getName(), grantedScope.getName());
        }
    }

    @Test
    public void listApplicationsWithRootUrl() throws Exception {
        oauth.clientId("root-url-client");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("view-applications-access", "password");
        assertNull(tokenResponse.getErrorDescription());

        TokenUtil token = new TokenUtil("view-applications-access", "password");
        List<ClientRepresentation> applications = SimpleHttpDefault
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        assertThat(apps.keySet(), containsInAnyOrder("root-url-client", "always-display-client", "direct-grant"));

        assertClientRep(apps.get("root-url-client"), null, null, false, true, false, "http://localhost:8180/foo/bar", "/baz");
    }

    private void assertClientRep(ClientRepresentation clientRep, String name, String description, boolean userConsentRequired, boolean inUse, boolean offlineAccess, String rootUrl, String baseUrl) {
        assertNotNull(clientRep);
        assertEquals(name, clientRep.getClientName());
        assertEquals(description, clientRep.getDescription());
        assertEquals(userConsentRequired, clientRep.isUserConsentRequired());
        assertEquals(inUse, clientRep.isInUse());
        assertEquals(offlineAccess, clientRep.isOfflineAccess());
        assertEquals(rootUrl, clientRep.getRootUrl());
        assertEquals(baseUrl, clientRep.getBaseUrl());
        assertEquals(ResolveRelative.resolveRelativeUri(null, null, rootUrl, baseUrl), clientRep.getEffectiveUrl());
    }

    @Test
    public void listApplicationsWithoutPermission() throws IOException {
        TokenUtil token = new TokenUtil("no-account-access", "password");
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse()) {
            assertEquals(403, response.getStatus());
        }
    }

    @Test
    public void getNotExistingApplication() throws IOException {
        TokenUtil token = new TokenUtil("view-applications-access", "password");
        String appId = "not-existing";
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doGet(getAccountUrl("applications/" + appId), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse()) {
            assertEquals(404, response.getStatus());
        }
    }

    private ConsentRepresentation createRequestedConsent(List<ClientScopeRepresentation> scopes) {
        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setGrantedScopes(scopes.stream().map((scope)-> {
            ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
            consentScopeRepresentation.setId(scope.getId());
            return consentScopeRepresentation;
        }).collect(Collectors.toList()));
        return requestedConsent;
    }

    @Test
    public void createConsentForClient() throws IOException {
        tokenUtil = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";
        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,2);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        ConsentRepresentation consentRepresentation = SimpleHttpDefault
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertThat(consentRepresentation.getGrantedScopes().stream().map(ConsentScopeRepresentation::getId).collect(Collectors.toList()),
                containsInAnyOrder(requestedScopes.stream().map(ClientScopeRepresentation::getId).toArray()));

        events.poll();
        String expectedScopeDetails = requestedScopes.stream().map(cs->cs.getName()).collect(Collectors.joining(" "));
        events.expectAccount(EventType.GRANT_CONSENT)
                .user(getUser().getId())
                .detail(Details.GRANTED_CLIENT,appId)
                .detail(Details.SCOPE,expectedScopeDetails)
                .assertEvent();
        events.assertEmpty();

        //cleanup
        SimpleHttpDefault.doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse().close();
    }

    @Test
    public void updateConsentForClient() throws IOException {
        tokenUtil = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";
        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,1);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        ConsentRepresentation consentRepresentation = SimpleHttpDefault
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getGrantedScopes().size());
        assertEquals(requestedScopes.get(0).getId(), consentRepresentation.getGrantedScopes().get(0).getId());

        requestedScopes = testRealm().clientScopes().findAll().subList(1,2);
        requestedConsent = createRequestedConsent(requestedScopes);

        ConsentRepresentation consentRepresentation2 = SimpleHttpDefault
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation2.getCreatedDate() > 0);
        assertEquals(consentRepresentation.getCreatedDate(), consentRepresentation2.getCreatedDate());
        assertTrue(consentRepresentation2.getLastUpdatedDate() > 0);
        assertTrue(consentRepresentation2.getLastUpdatedDate() > consentRepresentation.getLastUpdatedDate());
        assertEquals(1, consentRepresentation2.getGrantedScopes().size());
        assertEquals(requestedScopes.get(0).getId(), consentRepresentation2.getGrantedScopes().get(0).getId());

        events.poll();
        events.poll();
        events.expectAccount(EventType.UPDATE_CONSENT)
                .user(getUser().getId())
                .detail(Details.GRANTED_CLIENT,appId)
                .detail(Details.SCOPE,requestedScopes.get(0).getName())
                .assertEvent();
        events.assertEmpty();

        //Cleanup
        SimpleHttpDefault.doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse().close();
    }

    @Test
    public void createConsentForNotExistingClient() throws IOException {
        tokenUtil = new TokenUtil("manage-consent-access", "password");
        String appId = "not-existing";

        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,1);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        try (SimpleHttpResponse response = SimpleHttpDefault
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    public void createConsentForClientWithoutPermission() throws IOException {
        tokenUtil = new TokenUtil("view-consent-access", "password");
        String appId = "security-admin-console";

        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,1);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        try (SimpleHttpResponse response = SimpleHttpDefault
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(403, response.getStatus());
        }
    }

    @Test
    public void createConsentForClientWithPut() throws IOException {
        tokenUtil = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,1);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        ConsentRepresentation consentRepresentation = SimpleHttpDefault
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getGrantedScopes().size());
        assertEquals(requestedScopes.get(0).getId(), consentRepresentation.getGrantedScopes().get(0).getId());

        events.poll();
        events.expectAccount(EventType.GRANT_CONSENT)
                .user(getUser().getId())
                .detail(Details.GRANTED_CLIENT,appId)
                .detail(Details.SCOPE,requestedScopes.get(0).getName())
                .assertEvent();
        events.assertEmpty();

        //Cleanup
        SimpleHttpDefault.doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse().close();
    }

    @Test
    public void updateConsentForClientWithPut() throws IOException {
        tokenUtil = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,1);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        ConsentRepresentation consentRepresentation = SimpleHttpDefault
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getGrantedScopes().size());
        assertEquals(requestedScopes.get(0).getId(), consentRepresentation.getGrantedScopes().get(0).getId());

        requestedScopes = testRealm().clientScopes().findAll().subList(1,2);
        requestedConsent = createRequestedConsent(requestedScopes);

        ConsentRepresentation consentRepresentation2 = SimpleHttpDefault
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation2.getCreatedDate() > 0);
        assertEquals(consentRepresentation.getCreatedDate(), consentRepresentation2.getCreatedDate());
        assertTrue(consentRepresentation2.getLastUpdatedDate() > 0);
        assertTrue(consentRepresentation2.getLastUpdatedDate() > consentRepresentation.getLastUpdatedDate());
        assertEquals(1, consentRepresentation2.getGrantedScopes().size());
        assertEquals(requestedScopes.get(0).getId(), consentRepresentation2.getGrantedScopes().get(0).getId());

        events.poll();
        events.poll();
        events.expectAccount(EventType.UPDATE_CONSENT)
                .user(getUser().getId())
                .detail(Details.GRANTED_CLIENT,appId)
                .detail(Details.SCOPE,requestedScopes.get(0).getName())
                .assertEvent();
        events.assertEmpty();

        //Cleanup
        SimpleHttpDefault.doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse().close();
    }

    @Test
    public void createConsentForNotExistingClientWithPut() throws IOException {
        tokenUtil = new TokenUtil("manage-consent-access", "password");
        String appId = "not-existing";

        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,1);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        try (SimpleHttpResponse response = SimpleHttpDefault
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    public void createConsentForClientWithoutPermissionWithPut() throws IOException {
        tokenUtil = new TokenUtil("view-consent-access", "password");
        String appId = "security-admin-console";

        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,1);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        try (SimpleHttpResponse response = SimpleHttpDefault
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(403, response.getStatus());
        }
    }

    @Test
    public void getConsentForClient() throws IOException {
        tokenUtil = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,1);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        ConsentRepresentation consentRepresentation1 = SimpleHttpDefault
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation1.getCreatedDate() > 0);
        assertTrue(consentRepresentation1.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation1.getGrantedScopes().size());
        assertEquals(requestedScopes.get(0).getId(), consentRepresentation1.getGrantedScopes().get(0).getId());

        ConsentRepresentation consentRepresentation2 = SimpleHttpDefault
                .doGet(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asJson(ConsentRepresentation.class);
        assertEquals(consentRepresentation1.getLastUpdatedDate(), consentRepresentation2.getLastUpdatedDate());
        assertEquals(consentRepresentation1.getCreatedDate(), consentRepresentation2.getCreatedDate());
        assertEquals(consentRepresentation1.getGrantedScopes().get(0).getId(), consentRepresentation2.getGrantedScopes().get(0).getId());
    }

    @Test
    public void getConsentForNotExistingClient() throws IOException {
        tokenUtil = new TokenUtil("view-consent-access", "password");
        String appId = "not-existing";
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doGet(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    public void getNotExistingConsentForClient() throws IOException {
        tokenUtil = new TokenUtil("view-consent-access", "password");
        String appId = "security-admin-console";
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doGet(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(204, response.getStatus());
        }
    }

    @Test
    public void getConsentWithoutPermission() throws IOException {
        tokenUtil = new TokenUtil("no-account-access", "password");
        String appId = "security-admin-console";
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doGet(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(403, response.getStatus());
        }
    }

    @Test
    public void deleteConsentForClient() throws IOException {
        tokenUtil = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        List<ClientScopeRepresentation> requestedScopes = testRealm().clientScopes().findAll().subList(0,1);
        ConsentRepresentation requestedConsent = createRequestedConsent(requestedScopes);

        ConsentRepresentation consentRepresentation = SimpleHttpDefault
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(tokenUtil.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getGrantedScopes().size());
        assertEquals(requestedScopes.get(0).getId(), consentRepresentation.getGrantedScopes().get(0).getId());

        try (SimpleHttpResponse response = SimpleHttpDefault
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(204, response.getStatus());
        }

        events.poll();
        events.poll();
        events.expectAccount(EventType.REVOKE_GRANT)
                .user(getUser().getId())
                .detail(Details.REVOKED_CLIENT,appId)
                .assertEvent();
        events.assertEmpty();

        try (SimpleHttpResponse response = SimpleHttpDefault
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(204, response.getStatus());
        }
    }

    @Test
    public void deleteConsentForNotExistingClient() throws IOException {
        tokenUtil = new TokenUtil("manage-consent-access", "password");
        String appId = "not-existing";
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    public void deleteConsentWithoutPermission() throws IOException {
        tokenUtil = new TokenUtil("view-consent-access", "password");
        String appId = "security-admin-console";
        try (SimpleHttpResponse response = SimpleHttpDefault
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(403, response.getStatus());
        }
    }

    //KEYCLOAK-14344
    @Test
    public void revokeOfflineAccess() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse offlineTokenResponse = oauth.doPasswordGrantRequest("view-applications-access", "password");
        assertNull(offlineTokenResponse.getErrorDescription());

        tokenUtil = new TokenUtil("view-applications-access", "password");

        try (SimpleHttpResponse response = SimpleHttpDefault
                .doDelete(getAccountUrl("applications/offline-client/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asResponse()) {
            assertEquals(204, response.getStatus());
        }

        List<ClientRepresentation> applications = SimpleHttpDefault
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(tokenUtil.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        assertThat(apps.keySet(), containsInAnyOrder("always-display-client", "direct-grant"));

        assertNull(apps.get("offline-client"));
    }

    @Test
    public void testApiVersion() throws IOException {
        apiVersion = AccountRestApiVersion.DEFAULT.getStrVersion();

        // a smoke test to check API with version works
        testUpdateProfile(); // profile endpoint is the root URL of account REST service, i.e. the URL will be like "/v1/"
        testCredentialsGet(); // "/v1/credentials"
    }

    @Test
    public void testInvalidApiVersion() throws IOException {
        apiVersion = "v2-foo";

        try (SimpleHttpResponse response = SimpleHttpDefault.doGet(getAccountUrl("credentials"), httpClient).auth(tokenUtil.getToken()).asResponse()) {
            assertEquals("API version not found", response.asJson().get("error").textValue());
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    public void testAudience() throws Exception {
        oauth.clientId("custom-audience");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertNull(tokenResponse.getErrorDescription());

        try (SimpleHttpResponse response = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient)
                .auth(tokenResponse.getAccessToken())
                .header("Accept", "application/json")
                .asResponse()) {
            assertEquals(401, response.getStatus());
        }

        // update to correct audience
        org.keycloak.representations.idm.ClientRepresentation clientRep = testRealm().clients().findByClientId("custom-audience").get(0);
        ProtocolMapperRepresentation mapperRep = clientRep.getProtocolMappers().stream().filter(m -> m.getName().equals("aud")).findFirst().orElse(null);
        assertNotNull("Audience mapper not found", mapperRep);
        mapperRep.getConfig().put("included.custom.audience", "account");
        testRealm().clients().get(clientRep.getId()).getProtocolMappers().update(mapperRep.getId(), mapperRep);

        tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertNull(tokenResponse.getErrorDescription());

        try (SimpleHttpResponse response = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient)
                .auth(tokenResponse.getAccessToken())
                .header("Accept", "application/json")
                .asResponse()) {
            assertEquals(200, response.getStatus());
        }

        // remove audience completely
        testRealm().clients().get(clientRep.getId()).getProtocolMappers().delete(mapperRep.getId());

        tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertNull(tokenResponse.getErrorDescription());

        try (SimpleHttpResponse response = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient)
                .auth(tokenResponse.getAccessToken())
                .header("Accept", "application/json")
                .asResponse()) {
            assertEquals(401, response.getStatus());
        }

        // custom-audience client is used only in this test so no need to revert the changes
    }

    @Test
    public void testCustomAccountResourceTheme() throws Exception {
        String accountTheme = "";
        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            accountTheme = realmRep.getAccountTheme();
            realmRep.setAccountTheme("custom-account-provider");
            adminClient.realm("test").update(realmRep);

            try (SimpleHttpResponse response = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient)
                       .header("Accept", "text/html")
                       .asResponse()) {
                assertEquals(200, response.getStatus());

                String html = response.asString();
                assertTrue(html.contains("Custom Account Console"));
            }
        } finally {
            RealmRepresentation realmRep = testRealm().toRepresentation();
            realmRep.setAccountTheme(accountTheme);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void testUpdateProfileUnrecognizedPropertyInRepresentation() throws IOException {
        final UserRepresentation user = getUser();
        final Map<String,String> invalidRep = Map.of("id", user.getId(), "username", user.getUsername(), "invalid", "something");
        try (SimpleHttpResponse response = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient)
                .auth(tokenUtil.getToken())
                .json(invalidRep)
                .asResponse()) {
            assertEquals(400, response.getStatus());
            final OAuth2ErrorRepresentation error = response.asJson(OAuth2ErrorRepresentation.class);
            assertThat(error.getError(), containsString("Invalid json representation for UserRepresentation. Unrecognized field \"invalid\" at line"));
        }
    }

    @Test
    public void testEmailWhenUpdateEmailEnabled() throws Exception {
        reconnectAdminClient();
        RealmRepresentation realm = testRealm().toRepresentation();
        Boolean registrationEmailAsUsername = realm.isRegistrationEmailAsUsername();
        Boolean editUsernameAllowed = realm.isEditUsernameAllowed();
        ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, true);

        try {
            realm.setRegistrationEmailAsUsername(true);
            realm.setEditUsernameAllowed(true);
            testRealm().update(realm);
            UserRepresentation user = getUser(true);
            assertNotNull(user.getEmail());
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, true);

            realm.setRegistrationEmailAsUsername(false);
            realm.setEditUsernameAllowed(false);
            testRealm().update(realm);
            user = getUser(true);
            assertNotNull(user.getEmail());
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, true);
        } finally {
            ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, false);
            realm.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.setEditUsernameAllowed(editUsernameAllowed);
            testRealm().update(realm);
        }
    }

    protected void setUserProfileConfiguration(String configuration) {
        UserProfileUtil.setUserProfileConfiguration(testRealm(), configuration);
    }
}
