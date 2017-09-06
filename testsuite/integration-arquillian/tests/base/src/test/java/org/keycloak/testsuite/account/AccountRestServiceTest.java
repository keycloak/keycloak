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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountRestServiceTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public TokenUtil tokenUtil = new TokenUtil();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.getUsers().add(UserBuilder.create().username("no-account-access").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-account-access").role("account", "view-profile").password("password").build());
    }

    @Test
    public void testGetProfile() throws IOException {
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), client).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        assertEquals("Tom", user.getFirstName());
        assertEquals("Brady", user.getLastName());
        assertEquals("test-user@localhost", user.getEmail());
        assertFalse(user.isEmailVerified());
        assertTrue(user.getAttributes().isEmpty());
    }

    @Test
    public void testUpdateProfile() throws IOException {
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), client).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
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
        updateError(user, 409, "email_exists");

        user.setEmail("test-user@localhost");
        user = updateAndGet(user);
        assertEquals("test-user@localhost", user.getEmail());

        // Update username
        user.setUsername("updatedUsername");
        user = updateAndGet(user);
        assertEquals("updatedusername", user.getUsername());

        user.setUsername("john-doh@localhost");
        updateError(user, 409, "username_exists");

        user.setUsername("test-user@localhost");
        user = updateAndGet(user);
        assertEquals("test-user@localhost", user.getUsername());

        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        realmRep.setEditUsernameAllowed(false);
        adminClient.realm("test").update(realmRep);

        user.setUsername("updatedUsername2");
        updateError(user, 400, "username_read_only");
    }

    private UserRepresentation updateAndGet(UserRepresentation user) throws IOException {
        int status = SimpleHttp.doPost(getAccountUrl(null), client).auth(tokenUtil.getToken()).json(user).asStatus();
        assertEquals(200, status);
        return SimpleHttp.doGet(getAccountUrl(null), client).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
    }


    private void updateError(UserRepresentation user, int expectedStatus, String expectedMessage) throws IOException {
        SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), client).auth(tokenUtil.getToken()).json(user).asResponse();
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedMessage, response.asJson(ErrorRepresentation.class).getErrorMessage());
    }

    @Test
    public void testProfilePermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");

        // Read with no access
        assertEquals(403, SimpleHttp.doGet(getAccountUrl(null), client).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());

        // Update with no access
        assertEquals(403, SimpleHttp.doPost(getAccountUrl(null), client).auth(noaccessToken.getToken()).json(new UserRepresentation()).asStatus());

        // Update with read only
        assertEquals(403, SimpleHttp.doPost(getAccountUrl(null), client).auth(viewToken.getToken()).json(new UserRepresentation()).asStatus());
    }

    @Test
    public void testUpdateProfilePermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        int status = SimpleHttp.doGet(getAccountUrl(null), client).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus();
        assertEquals(403, status);

        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        status = SimpleHttp.doGet(getAccountUrl(null), client).header("Accept", "application/json").auth(viewToken.getToken()).asStatus();
        assertEquals(200, status);
    }

    @Test
    public void testGetSessions() throws IOException {
        List<SessionRepresentation> sessions = SimpleHttp.doGet(getAccountUrl("sessions"), client).auth(tokenUtil.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});

        assertEquals(1, sessions.size());
    }

    private String getAccountUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account" + (resource != null ? "/" + resource : "");
    }

}
