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

package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.*;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.actions.RequiredActionEmailVerificationTest;
import org.keycloak.testsuite.forms.ResetPasswordTest;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.rule.GreenMailRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @WebResource
    protected LoginPasswordUpdatePage passwordUpdatePage;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected InfoPage infoPage;

    @WebResource
    protected LoginPage loginPage;

    public String createUser() {
        return createUser("user1", "user1@localhost");
    }

    public String createUser(String username, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);

        Response response = realm.users().create(user);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();
        return createdId;
    }

    @Test
    public void verifyCreateUser() {
        createUser();
    }

    @Test
    public void createDuplicatedUser1() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());

        // Just to show how to retrieve underlying error message
        ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
        Assert.assertEquals("User exists with same username", error.getErrorMessage());

        response.close();
    }

    @Test
    public void createDuplicatedUser2() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@localhost");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }
    
    @Test
    public void createDuplicatedUser3() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("User1");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }
    
    @Test
    public void createDuplicatedUser4() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("USER1");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }

    @Test
    public void createDuplicatedUser5() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("User1@localhost");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }
    
    @Test
    public void createDuplicatedUser6() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@LOCALHOST");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }

    @Test
    public void createDuplicatedUser7() {
        createUser("user1", "USer1@Localhost");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@localhost");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();

    }
    
    private void createUsers() {
        for (int i = 1; i < 10; i++) {
            UserRepresentation user = new UserRepresentation();
            user.setUsername("username" + i);
            user.setEmail("user" + i + "@localhost");
            user.setFirstName("First" + i);
            user.setLastName("Last" + i);

            realm.users().create(user).close();
        }
    }

    @Test
    public void searchByEmail() {
        createUsers();

        List<UserRepresentation> users = realm.users().search(null, null, null, "user1@localhost", null, null);
        assertEquals(1, users.size());

        users = realm.users().search(null, null, null, "@localhost", null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void searchByUsername() {
        createUsers();

        List<UserRepresentation> users = realm.users().search("username1", null, null, null, null, null);
        assertEquals(1, users.size());

        users = realm.users().search("user", null, null, null, null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void search() {
        createUsers();

        List<UserRepresentation> users = realm.users().search("username1", null, null);
        assertEquals(1, users.size());

        users = realm.users().search("first1", null, null);
        assertEquals(1, users.size());

        users = realm.users().search("last", null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void searchPaginated() {
        createUsers();

        List<UserRepresentation> users = realm.users().search("username", 0, 1);
        assertEquals(1, users.size());
        assertEquals("username1", users.get(0).getUsername());

        users = realm.users().search("username", 5, 2);
        assertEquals(2, users.size());
        assertEquals("username6", users.get(0).getUsername());
        assertEquals("username7", users.get(1).getUsername());

        users = realm.users().search("username", 7, 20);
        assertEquals(2, users.size());
        assertEquals("username8", users.get(0).getUsername());
        assertEquals("username9", users.get(1).getUsername());

        users = realm.users().search("username", 0, 20);
        assertEquals(9, users.size());
    }

    @Test
    public void getFederatedIdentities() {
        // Add sample identity provider
        addSampleIdentityProvider();

        // Add sample user
        String id = createUser();
        UserResource user = realm.users().get(id);
        assertEquals(0, user.getFederatedIdentity().size());

        // Add social link to the user
        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setUserId("social-user-id");
        link.setUserName("social-username");
        Response response = user.addFederatedIdentity("social-provider-id", link);
        assertEquals(204, response.getStatus());

        // Verify social link is here
        user = realm.users().get(id);
        List<FederatedIdentityRepresentation> federatedIdentities = user.getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        link = federatedIdentities.get(0);
        assertEquals("social-provider-id", link.getIdentityProvider());
        assertEquals("social-user-id", link.getUserId());
        assertEquals("social-username", link.getUserName());

        // Remove social link now
        user.removeFederatedIdentity("social-provider-id");
        assertEquals(0, user.getFederatedIdentity().size());

        removeSampleIdentityProvider();
    }

    private void addSampleIdentityProvider() {
        List<IdentityProviderRepresentation> providers = realm.identityProviders().findAll();
        Assert.assertEquals(0, providers.size());

        IdentityProviderRepresentation rep = new IdentityProviderRepresentation();
        rep.setAlias("social-provider-id");
        rep.setProviderId("social-provider-type");
        realm.identityProviders().create(rep);
    }

    private void removeSampleIdentityProvider() {
        IdentityProviderResource resource = realm.identityProviders().get("social-provider-id");
        Assert.assertNotNull(resource);
        resource.remove();
    }

    @Test
    public void addRequiredAction() {
        String id = createUser();

        UserResource user = realm.users().get(id);
        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());

        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add("UPDATE_PASSWORD");
        user.update(userRep);

        assertEquals(1, user.toRepresentation().getRequiredActions().size());
        assertEquals("UPDATE_PASSWORD", user.toRepresentation().getRequiredActions().get(0));
    }

    @Test
    public void removeRequiredAction() {
        String id = createUser();

        UserResource user = realm.users().get(id);
        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());

        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add("UPDATE_PASSWORD");
        user.update(userRep);

        user = realm.users().get(id);
        userRep = user.toRepresentation();
        userRep.getRequiredActions().clear();
        user.update(userRep);

        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());
    }

    @Test
    public void attributes() {
        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        user1.singleAttribute("attr1", "value1user1");
        user1.singleAttribute("attr2", "value2user1");

        Response response = realm.users().create(user1);
        String user1Id = ApiUtil.getCreatedId(response);
        response.close();

        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername("user2");
        user2.singleAttribute("attr1", "value1user2");
        List<String> vals = new ArrayList<>();
        vals.add("value2user2");
        vals.add("value2user2_2");
        user2.getAttributesAsListValues().put("attr2", vals);

        response = realm.users().create(user2);
        String user2Id = ApiUtil.getCreatedId(response);
        response.close();
        user1 = realm.users().get(user1Id).toRepresentation();
        assertEquals(2, user1.getAttributesAsListValues().size());
        assertAttributeValue("value1user1", user1.getAttributesAsListValues().get("attr1"));
        assertAttributeValue("value2user1", user1.getAttributesAsListValues().get("attr2"));

        user2 = realm.users().get(user2Id).toRepresentation();
        assertEquals(2, user2.getAttributesAsListValues().size());
        assertAttributeValue("value1user2", user2.getAttributesAsListValues().get("attr1"));
        vals = user2.getAttributesAsListValues().get("attr2");
        assertEquals(2, vals.size());
        assertTrue(vals.contains("value2user2") && vals.contains("value2user2_2"));

        user1.singleAttribute("attr1", "value3user1");
        user1.singleAttribute("attr3", "value4user1");

        realm.users().get(user1Id).update(user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        assertEquals(3, user1.getAttributesAsListValues().size());
        assertAttributeValue("value3user1", user1.getAttributesAsListValues().get("attr1"));
        assertAttributeValue("value2user1", user1.getAttributesAsListValues().get("attr2"));
        assertAttributeValue("value4user1", user1.getAttributesAsListValues().get("attr3"));

        user1.getAttributes().remove("attr1");
        realm.users().get(user1Id).update(user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        assertEquals(2, user1.getAttributesAsListValues().size());
        assertAttributeValue("value2user1", user1.getAttributesAsListValues().get("attr2"));
        assertAttributeValue("value4user1", user1.getAttributesAsListValues().get("attr3"));

        user1.getAttributes().clear();
        realm.users().get(user1Id).update(user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        assertNull(user1.getAttributes());
    }

    private void assertAttributeValue(String expectedValue, List<String> attrValues) {
        assertEquals(1, attrValues.size());
        assertEquals(expectedValue, attrValues.get(0));
    }

    @Test
    public void sendResetPasswordEmail() {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user1");
        Response response = realm.users().create(userRep);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        UserResource user = realm.users().get(id);
        List<String> actions = new LinkedList<>();
        try {
            user.executeActionsEmail(actions);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User email missing", error.getErrorMessage());
        }
        try {
            userRep = user.toRepresentation();
            userRep.setEmail("user1@localhost");
            userRep.setEnabled(false);
            user.update(userRep);
            user.executeActionsEmail(actions);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User is disabled", error.getErrorMessage());
        }
        try {
            userRep.setEnabled(true);
            user.update(userRep);
            user.executeActionsEmail("invalidClientId", actions);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("invalidClientId not enabled", error.getErrorMessage());
        }
    }

    @Test
    public void sendResetPasswordEmailSuccess() throws IOException, MessagingException {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setEnabled(true);
        userRep.setUsername("user1");
        userRep.setEmail("user1@test.com");
        Response response = realm.users().create(userRep);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        UserResource user = realm.users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail("account", actions);

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String link = ResetPasswordTest.getPasswordResetEmailLink(message);

        driver.navigate().to(link);

        assertTrue(passwordUpdatePage.isCurrent());

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertEquals("Your account has been updated.", driver.getTitle());

        driver.navigate().to(link);

        assertEquals("We're sorry...", driver.getTitle());
    }


    @Test
    public void sendVerifyEmail() throws IOException, MessagingException {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user1");
        Response response = realm.users().create(userRep);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        UserResource user = realm.users().get(id);

        try {
            user.sendVerifyEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User email missing", error.getErrorMessage());
        }
        try {
            userRep = user.toRepresentation();
            userRep.setEmail("user1@localhost");
            userRep.setEnabled(false);
            user.update(userRep);
            user.sendVerifyEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User is disabled", error.getErrorMessage());
        }
        try {
            userRep.setEnabled(true);
            user.update(userRep);
            user.sendVerifyEmail("invalidClientId");
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("invalidClientId not enabled", error.getErrorMessage());
        }

        user.sendVerifyEmail();
        assertEquals(1, greenMail.getReceivedMessages().length);

        String link = RequiredActionEmailVerificationTest.getPasswordResetEmailLink(greenMail.getReceivedMessages()[0]);

        driver.navigate().to(link);

        Assert.assertEquals("Your account has been updated.", infoPage.getInfo());
    }

    @Test
    public void updateUserWithNewUsername() {
        switchEditUsernameAllowedOn();
        String id = createUser();

        UserResource user = realm.users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setUsername("user11");
        user.update(userRep);

        userRep = realm.users().get(id).toRepresentation();
        assertEquals("user11", userRep.getUsername());
    }

    @Test
    public void updateUserWithoutUsername() {
        switchEditUsernameAllowedOn();

        String id = createUser();

        UserResource user = realm.users().get(id);

        UserRepresentation rep = new UserRepresentation();
        rep.setFirstName("Firstname");

        user.update(rep);

        rep = new UserRepresentation();
        rep.setLastName("Lastname");

        user.update(rep);

        rep = realm.users().get(id).toRepresentation();

        assertEquals("user1", rep.getUsername());
        assertEquals("user1@localhost", rep.getEmail());
        assertEquals("Firstname", rep.getFirstName());
        assertEquals("Lastname", rep.getLastName());
    }

    @Test
    public void updateUserWithNewUsernameNotPossible() {
        String id = createUser();

        UserResource user = realm.users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setUsername("user11");
        user.update(userRep);

        userRep = realm.users().get(id).toRepresentation();
        assertEquals("user1", userRep.getUsername());
    }

    @Test
    public void updateUserWithNewUsernameAccessingViaOldUsername() {
        switchEditUsernameAllowedOn();
        createUser();

        try {
            UserResource user = realm.users().get("user1");
            UserRepresentation userRep = user.toRepresentation();
            userRep.setUsername("user1");
            user.update(userRep);

            realm.users().get("user11").toRepresentation();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(404, e.getResponse().getStatus());
        }
    }

    @Test
    public void updateUserWithExistingUsername() {
        switchEditUsernameAllowedOn();
        createUser();

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user2");
        Response response = realm.users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();

        try {
            UserResource user = realm.users().get(createdId);
            userRep = user.toRepresentation();
            userRep.setUsername("user1");
            user.update(userRep);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(409, e.getResponse().getStatus());
        }
    }

    @Test
    public void resetUserPassword() {
        String userId = createUser("user1", "user1@localhost");

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("password");
        cred.setTemporary(false);

        realm.users().get(userId).resetPassword(cred);

        String accountUrl = RealmsResource.accountUrl(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT)).build(REALM_NAME).toString();

        driver.navigate().to(accountUrl);

        assertEquals("Log in to admin-client-test", driver.getTitle());

        loginPage.login("user1", "password");

        assertEquals("Keycloak Account Management", driver.getTitle());
    }

    @Test
    public void resetUserInvalidPassword() {
        String userId = createUser("user1", "user1@localhost");

        try {
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(" ");
            cred.setTemporary(false);
            realm.users().get(userId).resetPassword(cred);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());
            e.getResponse().close();
        }
    }

    private void switchEditUsernameAllowedOn() {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setEditUsernameAllowed(true);
        realm.update(rep);
    }

}
