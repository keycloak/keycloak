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

import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.page.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.WebDriver;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.Assert.assertNames;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserTest extends AbstractAdminTest {

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Drone
    protected WebDriver driver;

    @Page
    protected LoginPasswordUpdatePage passwordUpdatePage;


    @ArquillianResource
    protected OAuthClient oAuthClient;

    @Page
    protected InfoPage infoPage;

    @Page
    protected LoginPage loginPage;

    public String createUser() {
        return createUser("user1", "user1@localhost");
    }

    public String createUser(String username, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setRequiredActions(Collections.<String>emptyList());
        user.setEnabled(true);

        return createUser(user);
    }

    private String createUser(UserRepresentation userRep) {
        Response response = realm.users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(createdId), userRep, ResourceType.USER);
        return createdId;
    }

    private void updateUser(UserResource user, UserRepresentation userRep) {
        user.update(userRep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.userResourcePath(userRep.getId()), userRep, ResourceType.USER);
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
        assertAdminEvents.assertEmpty();

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

        assertAdminEvents.assertEmpty();

    }
    
    private void createUsers() {
        for (int i = 1; i < 10; i++) {
            UserRepresentation user = new UserRepresentation();
            user.setUsername("username" + i);
            user.setEmail("user" + i + "@localhost");
            user.setFirstName("First" + i);
            user.setLastName("Last" + i);

            createUser(user);
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
    public void count() {
        createUsers();

        Integer count = realm.users().count();
        assertEquals(9, count.intValue());
    }

    @Test
    public void delete() {
        String userId = createUser();
        Response response = realm.users().delete( userId );
        assertEquals(204, response.getStatus());
        response.close();
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.userResourcePath(userId), ResourceType.USER);
    }

    @Test
    public void deleteNonExistent() {
        Response response = realm.users().delete( "does-not-exist" );
        assertEquals(404, response.getStatus());
        response.close();
        assertAdminEvents.assertEmpty();
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
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userFederatedIdentityLink(id, "social-provider-id"), link, ResourceType.USER);

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
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.userFederatedIdentityLink(id, "social-provider-id"), ResourceType.USER);
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
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.identityProviderPath(rep.getAlias()), rep, ResourceType.IDENTITY_PROVIDER);
    }

    private void removeSampleIdentityProvider() {
        IdentityProviderResource resource = realm.identityProviders().get("social-provider-id");
        Assert.assertNotNull(resource);
        resource.remove();
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.identityProviderPath("social-provider-id"), ResourceType.IDENTITY_PROVIDER);
    }

    @Test
    public void addRequiredAction() {
        String id = createUser();

        UserResource user = realm.users().get(id);
        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());

        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add("UPDATE_PASSWORD");
        updateUser(user, userRep);

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
        updateUser(user, userRep);

        user = realm.users().get(id);
        userRep = user.toRepresentation();
        userRep.getRequiredActions().clear();
        updateUser(user, userRep);

        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());
    }

    @Test
    public void attributes() {
        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        user1.singleAttribute("attr1", "value1user1");
        user1.singleAttribute("attr2", "value2user1");

        String user1Id = createUser(user1);

        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername("user2");
        user2.singleAttribute("attr1", "value1user2");
        List<String> vals = new ArrayList<>();
        vals.add("value2user2");
        vals.add("value2user2_2");
        user2.getAttributesAsListValues().put("attr2", vals);

        String user2Id = createUser(user2);

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

        updateUser(realm.users().get(user1Id), user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        assertEquals(3, user1.getAttributesAsListValues().size());
        assertAttributeValue("value3user1", user1.getAttributesAsListValues().get("attr1"));
        assertAttributeValue("value2user1", user1.getAttributesAsListValues().get("attr2"));
        assertAttributeValue("value4user1", user1.getAttributesAsListValues().get("attr3"));

        user1.getAttributes().remove("attr1");
        updateUser(realm.users().get(user1Id), user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        assertEquals(2, user1.getAttributesAsListValues().size());
        assertAttributeValue("value2user1", user1.getAttributesAsListValues().get("attr2"));
        assertAttributeValue("value4user1", user1.getAttributesAsListValues().get("attr3"));

        user1.getAttributes().clear();
        updateUser(realm.users().get(user1Id), user1);

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

        String id = createUser(userRep);

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
            updateUser(user, userRep);

            user.executeActionsEmail(actions);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User is disabled", error.getErrorMessage());
        }
        try {
            userRep.setEnabled(true);
            updateUser(user, userRep);

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

        String id = createUser(userRep);

        UserResource user = realm.users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail("account", actions);
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String link = MailUtils.getPasswordResetEmailLink(message);

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

        String id = createUser(userRep);

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
            updateUser(user, userRep);

            user.sendVerifyEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User is disabled", error.getErrorMessage());
        }
        try {
            userRep.setEnabled(true);
            updateUser(user, userRep);

            user.sendVerifyEmail("invalidClientId");
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("invalidClientId not enabled", error.getErrorMessage());
        }

        user.sendVerifyEmail();
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/send-verify-email", ResourceType.USER);

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        String link = MailUtils.getPasswordResetEmailLink(greenMail.getReceivedMessages()[0]);

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
        updateUser(user, userRep);

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
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.userResourcePath(id), rep, ResourceType.USER);

        rep = new UserRepresentation();
        rep.setLastName("Lastname");

        user.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.userResourcePath(id), rep, ResourceType.USER);

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
        updateUser(user, userRep);

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
            updateUser(user, userRep);

            realm.users().get("user11").toRepresentation();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(404, e.getResponse().getStatus());
        }
    }

    @Test
    public void updateUserWithExistingUsername() {
        switchEditUsernameAllowedOn();
        enableBruteForce();
        createUser();

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user2");

        String createdId = createUser(userRep);

        try {
            UserResource user = realm.users().get(createdId);
            userRep = user.toRepresentation();
            userRep.setUsername("user1");
            user.update(userRep);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(409, e.getResponse().getStatus());

            // TODO adminEvents: Event queue should be empty, but it's not because of bug in UsersResource.updateUser, which sends event earlier than transaction commit.
            // assertAdminEvents.assertEmpty();
            assertAdminEvents.poll();
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
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userResetPasswordPath(userId), ResourceType.USER);

        String accountUrl = RealmsResource.accountUrl(UriBuilder.fromUri(getAuthServerRoot())).build(REALM_NAME).toString();

        driver.navigate().to(accountUrl);

        assertEquals("Log in to admin-client-test", driver.getTitle());

        loginPage.login("user1", "password");

        assertTrue(driver.getTitle().contains("Account Management"));
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
            assertAdminEvents.assertEmpty();
        }
    }

    @Test
    public void testDefaultRequiredActionAdded() {
        // Add UPDATE_PASSWORD as default required action
        RequiredActionProviderRepresentation updatePasswordReqAction = realm.flows().getRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        updatePasswordReqAction.setDefaultAction(true);
        realm.flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), updatePasswordReqAction);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authRequiredActionPath(UserModel.RequiredAction.UPDATE_PASSWORD.toString()), updatePasswordReqAction, ResourceType.REQUIRED_ACTION);

        // Create user
        String userId = createUser("user1", "user1@localhost");

        UserRepresentation userRep = realm.users().get(userId).toRepresentation();
        Assert.assertEquals(1, userRep.getRequiredActions().size());
        Assert.assertEquals(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), userRep.getRequiredActions().get(0));

        // Remove UPDATE_PASSWORD default action
        updatePasswordReqAction = realm.flows().getRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        updatePasswordReqAction.setDefaultAction(true);
        realm.flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), updatePasswordReqAction);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authRequiredActionPath(UserModel.RequiredAction.UPDATE_PASSWORD.toString()), updatePasswordReqAction, ResourceType.REQUIRED_ACTION);
    }

    @Test
    public void roleMappings() {
        RealmResource realm = adminClient.realms().realm("test");

        // Enable events
        RealmRepresentation realmRep = RealmBuilder.edit(realm.toRepresentation()).testEventListener().build();
        realm.update(realmRep);

        realm.roles().create(RoleBuilder.create().name("realm-role").build());
        realm.roles().create(RoleBuilder.create().name("realm-composite").build());
        realm.roles().create(RoleBuilder.create().name("realm-child").build());
        realm.roles().get("realm-composite").addComposites(Collections.singletonList(realm.roles().get("realm-child").toRepresentation()));

        Response response = realm.clients().create(ClientBuilder.create().clientId("myclient").build());
        String clientUuid = ApiUtil.getCreatedId(response);
        response.close();

        realm.clients().get(clientUuid).roles().create(RoleBuilder.create().name("client-role").build());
        realm.clients().get(clientUuid).roles().create(RoleBuilder.create().name("client-role2").build());
        realm.clients().get(clientUuid).roles().create(RoleBuilder.create().name("client-composite").build());
        realm.clients().get(clientUuid).roles().create(RoleBuilder.create().name("client-child").build());
        realm.clients().get(clientUuid).roles().get("client-composite").addComposites(Collections.singletonList(realm.clients().get(clientUuid).roles().get("client-child").toRepresentation()));

        response = realm.users().create(UserBuilder.create().username("myuser").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        // Admin events for creating role, client or user tested already in other places
        assertAdminEvents.clear();

        RoleMappingResource roles = realm.users().get(userId).roles();
        assertNames(roles.realmLevel().listAll(), "user", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION);

        // Add realm roles
        List<RoleRepresentation> l = new LinkedList<>();
        l.add(realm.roles().get("realm-role").toRepresentation());
        l.add(realm.roles().get("realm-composite").toRepresentation());
        roles.realmLevel().add(l);
        assertAdminEvents.assertEvent("test", OperationType.CREATE, AdminEventPaths.userRealmRoleMappingsPath(userId), l, ResourceType.REALM_ROLE_MAPPING);

        // Add client roles
        List<RoleRepresentation> list = Collections.singletonList(realm.clients().get(clientUuid).roles().get("client-role").toRepresentation());
        roles.clientLevel(clientUuid).add(list);
        assertAdminEvents.assertEvent("test", OperationType.CREATE, AdminEventPaths.userClientRoleMappingsPath(userId, clientUuid), list, ResourceType.CLIENT_ROLE_MAPPING);

        list = Collections.singletonList(realm.clients().get(clientUuid).roles().get("client-composite").toRepresentation());
        roles.clientLevel(clientUuid).add(list);
        assertAdminEvents.assertEvent("test", OperationType.CREATE, AdminEventPaths.userClientRoleMappingsPath(userId, clientUuid), ResourceType.CLIENT_ROLE_MAPPING);

        // List realm roles
        assertNames(roles.realmLevel().listAll(), "realm-role", "realm-composite", "user", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION);
        assertNames(roles.realmLevel().listAvailable(), "admin", "customer-user-premium");
        assertNames(roles.realmLevel().listEffective(), "realm-role", "realm-composite", "realm-child", "user", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION);

        // List client roles
        assertNames(roles.clientLevel(clientUuid).listAll(), "client-role", "client-composite");
        assertNames(roles.clientLevel(clientUuid).listAvailable(), "client-role2");
        assertNames(roles.clientLevel(clientUuid).listEffective(), "client-role", "client-composite", "client-child");

        // Get mapping representation
        MappingsRepresentation all = roles.getAll();
        assertNames(all.getRealmMappings(), "realm-role", "realm-composite", "user", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION);
        assertEquals(2, all.getClientMappings().size());
        assertNames(all.getClientMappings().get("myclient").getMappings(), "client-role", "client-composite");
        assertNames(all.getClientMappings().get("account").getMappings(), "manage-account", "view-profile");

        // Remove realm role
        RoleRepresentation realmRoleRep = realm.roles().get("realm-role").toRepresentation();
        roles.realmLevel().remove(Collections.singletonList(realmRoleRep));
        assertAdminEvents.assertEvent("test", OperationType.DELETE, AdminEventPaths.userRealmRoleMappingsPath(userId), Collections.singletonList(realmRoleRep), ResourceType.REALM_ROLE_MAPPING);

        assertNames(roles.realmLevel().listAll(), "realm-composite", "user", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION);

        // Remove client role
        RoleRepresentation clientRoleRep = realm.clients().get(clientUuid).roles().get("client-role").toRepresentation();
        roles.clientLevel(clientUuid).remove(Collections.singletonList(clientRoleRep));
        assertAdminEvents.assertEvent("test", OperationType.DELETE, AdminEventPaths.userClientRoleMappingsPath(userId, clientUuid), Collections.singletonList(clientRoleRep), ResourceType.CLIENT_ROLE_MAPPING);

        assertNames(roles.clientLevel(clientUuid).listAll(), "client-composite");
    }

    private void switchEditUsernameAllowedOn() {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setEditUsernameAllowed(true);
        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);
    }

    private void enableBruteForce() {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setBruteForceProtected(true);
        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);
    }

}
