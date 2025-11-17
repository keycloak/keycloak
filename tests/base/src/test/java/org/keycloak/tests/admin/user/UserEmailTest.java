package org.keycloak.tests.admin.user;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.VerificationException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.ProceedPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class UserEmailTest extends AbstractUserTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectClient(config = UserEmailTestAppClientConf.class)
    ManagedClient client;

    @InjectMailServer
    MailServer mailServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectPage
    LoginPasswordUpdatePage passwordUpdatePage;

    @InjectPage
    InfoPage infoPage;

    @InjectPage
    ProceedPage proceedPage;

    @InjectPage
    ErrorPage errorPage;

    @Test
    public void sendResetPasswordEmail() {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user1");

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);
        List<String> actions = new LinkedList<>();
        try {
            user.executeActionsEmail(actions);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User email missing", error.getErrorMessage());
            Assertions.assertNull(adminEvents.poll());
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
            Assertions.assertEquals("User is disabled", error.getErrorMessage());
            Assertions.assertNull(adminEvents.poll());
        }
        try {
            userRep.setEnabled(true);
            updateUser(user, userRep);

            user.executeActionsEmail(Arrays.asList(
                    UserModel.RequiredAction.UPDATE_PASSWORD.name(),
                    "invalid\"<img src=\"alert(0)\">")
            );
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("Provided invalid required actions", error.getErrorMessage());
            Assertions.assertNull(adminEvents.poll());
        }

        try {
            user.executeActionsEmail(
                    "invalidClientId",
                    "invalidUri",
                    Collections.singletonList(UserModel.RequiredAction.UPDATE_PASSWORD.name())
            );
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("Client doesn't exist", error.getErrorMessage());
            Assertions.assertNull(adminEvents.poll());
        }
    }

    @Test
    public void sendResetPasswordEmailSuccess() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail(actions);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[0];

        MailUtils.EmailBody body = MailUtils.getBody(message);

        assertTrue(body.getText().contains("Update Password"));
        assertTrue(body.getText().contains("your Default account"));
        assertTrue(body.getText().contains("This link will expire within 12 hours"));

        String link = MailUtils.getPasswordResetEmailLink(body);

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertThat(driver.getCurrentUrl(), Matchers.containsString("client_id=" + Constants.ACCOUNT_MANAGEMENT_CLIENT_ID));

        assertEquals("Your account has been updated.", infoPage.getInfo());

        driver.navigate().to(link);

        errorPage.assertCurrent();
    }

    @Test
    public void sendResetPasswordEmailSuccessWithAccountClientDisabled() throws IOException {
        ClientRepresentation clientRepresentation = managedRealm.admin().clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);
        clientRepresentation.setEnabled(false);
        managedRealm.admin().clients().get(clientRepresentation.getId()).update(clientRepresentation);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(clientRepresentation.getId()), clientRepresentation, ResourceType.CLIENT);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setEnabled(true);
        userRep.setUsername("user1");
        userRep.setEmail("user1@test.com");

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail(actions);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[0];

        MailUtils.EmailBody body = MailUtils.getBody(message);

        String link = MailUtils.getPasswordResetEmailLink(body);

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertThat(driver.getCurrentUrl(), Matchers.containsString("client_id=" + SystemClientUtil.SYSTEM_CLIENT_ID));

        clientRepresentation.setEnabled(true);
        managedRealm.admin().clients().get(clientRepresentation.getId()).update(clientRepresentation);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(clientRepresentation.getId()), clientRepresentation, ResourceType.CLIENT);
    }

    @Test
    public void testEmailLinkBasedOnRealmFrontEndUrl() throws Exception {
        try {
            updateRealmFrontEndUrl(adminClient.realm("master"), keycloakUrls.getBase());
            String expectedFrontEndUrl = "https://mytestrealm";
            updateRealmFrontEndUrl(adminClient.realm(managedRealm.getName()), expectedFrontEndUrl);

            UserRepresentation userRep = new UserRepresentation();
            userRep.setEnabled(true);
            userRep.setUsername("user1");
            userRep.setEmail("user1@test.com");

            String id = createUser(userRep, false);
            UserResource user = managedRealm.admin().users().get(id);
            List<String> actions = new LinkedList<>();
            actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            user.executeActionsEmail(actions);
            Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

            MimeMessage message = mailServer.getReceivedMessages()[0];
            MailUtils.EmailBody body = MailUtils.getBody(message);
            String link = MailUtils.getPasswordResetEmailLink(body);
            assertTrue(link.contains(expectedFrontEndUrl));
        } finally {
            updateRealmFrontEndUrl(adminClient.realm("master"), null);
            updateRealmFrontEndUrl(adminClient.realm(managedRealm.getName()), null);
        }
    }

    @Test
    public void sendResetPasswordEmailWithCustomLifespan() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());

        final int lifespan = (int) TimeUnit.HOURS.toSeconds(5);
        user.executeActionsEmail(actions, lifespan);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[0];

        MailUtils.EmailBody body = MailUtils.getBody(message);

        assertTrue(body.getText().contains("Update Password"));
        assertTrue(body.getText().contains("your Default account"));
        assertTrue(body.getText().contains("This link will expire within 5 hours"));

        String link = MailUtils.getPasswordResetEmailLink(body);

        String token = link.substring(link.indexOf("key=") + "key=".length());

        try {
            final AccessToken accessToken = TokenVerifier.create(token, AccessToken.class).getToken();
            assertThat(accessToken.getExp() - accessToken.getIat(), allOf(greaterThanOrEqualTo(lifespan - 1L), lessThanOrEqualTo(lifespan + 1L)));
            assertEquals(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, accessToken.getIssuedFor());
        } catch (VerificationException e) {
            throw new IOException(e);
        }


        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertEquals("Your account has been updated.", infoPage.getInfo());

        driver.navigate().to(link);

        errorPage.assertCurrent();
    }

    @Test
    public void sendResetPasswordEmailSuccessTwoLinks() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail(actions);
        user.executeActionsEmail(actions);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assertions.assertEquals(2, mailServer.getReceivedMessages().length);

        int i = 1;
        for (MimeMessage message : mailServer.getReceivedMessages()) {
            String link = MailUtils.getPasswordResetEmailLink(message);

            driver.navigate().to(link);

            proceedPage.assertCurrent();
            assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
            proceedPage.clickProceedLink();
            passwordUpdatePage.assertCurrent();

            passwordUpdatePage.changePassword("new-pass" + i, "new-pass" + i);
            i++;

            assertEquals("Your account has been updated.", infoPage.getInfo());
        }

        for (MimeMessage message : mailServer.getReceivedMessages()) {
            String link = MailUtils.getPasswordResetEmailLink(message);
            driver.navigate().to(link);
            errorPage.assertCurrent();
        }
    }

    @Test
    public void sendResetPasswordEmailSuccessTwoLinksReverse() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail(actions);
        user.executeActionsEmail(actions);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assertions.assertEquals(2, mailServer.getReceivedMessages().length);

        int i = 1;
        for (int j = mailServer.getReceivedMessages().length - 1; j >= 0; j--) {
            MimeMessage message = mailServer.getReceivedMessages()[j];

            String link = MailUtils.getPasswordResetEmailLink(message);

            driver.navigate().to(link);

            proceedPage.assertCurrent();
            assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
            proceedPage.clickProceedLink();
            passwordUpdatePage.assertCurrent();

            passwordUpdatePage.changePassword("new-pass" + i, "new-pass" + i);
            i++;

            assertEquals("Your account has been updated.", infoPage.getInfo());
        }

        for (MimeMessage message : mailServer.getReceivedMessages()) {
            String link = MailUtils.getPasswordResetEmailLink(message);
            driver.navigate().to(link);
            errorPage.assertCurrent();
        }
    }

    @Test
    public void sendResetPasswordEmailSuccessLinkOpenDoesNotExpireWhenOpenedOnly() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail(actions);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[0];

        String link = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        driver.manage().deleteAllCookies();
        driver.navigate().to("about:blank");

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertEquals("Your account has been updated.", infoPage.getInfo());
    }

    @Test
    public void sendResetPasswordEmailSuccessTokenShortLifespan() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        final AtomicInteger originalValue = new AtomicInteger();

        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        originalValue.set(realmRep.getActionTokenGeneratedByAdminLifespan());
        realmRep.setActionTokenGeneratedByAdminLifespan(60);
        managedRealm.admin().update(realmRep);

        try {
            UserResource user = managedRealm.admin().users().get(id);
            List<String> actions = new LinkedList<>();
            actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            user.executeActionsEmail(actions);

            Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

            MimeMessage message = mailServer.getReceivedMessages()[0];

            String link = MailUtils.getPasswordResetEmailLink(message);

            timeOffSet.set(70);

            driver.navigate().to(link);

            errorPage.assertCurrent();
            assertEquals("Action expired.", errorPage.getError());
        } finally {
            timeOffSet.set(0);

            realmRep.setActionTokenGeneratedByAdminLifespan(originalValue.get());
            managedRealm.admin().update(realmRep);
        }
    }

    @Test
    public void sendResetPasswordEmailSuccessWithRecycledAuthSession() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());

        // The following block creates a client and requests updating password with redirect to this client.
        // After clicking the link (starting a fresh auth session with client), the user goes away and sends the email
        // with password reset again - now without the client - and attempts to complete the password reset.
        {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId("myclient2");
            client.setRedirectUris(new LinkedList<>());
            client.getRedirectUris().add("http://myclient.com/*");
            client.setName("myclient2");
            client.setEnabled(true);
            Response response = managedRealm.admin().clients().create(client);
            String createdId = ApiUtil.getCreatedId(response);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientResourcePath(createdId), client, ResourceType.CLIENT);

            user.executeActionsEmail("myclient2", "http://myclient.com/home.html", actions);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

            Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

            MimeMessage message = mailServer.getReceivedMessages()[0];

            String link = MailUtils.getPasswordResetEmailLink(message);

            driver.navigate().to(link);
        }

        user.executeActionsEmail(actions);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assertions.assertEquals(2, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[mailServer.getReceivedMessages().length - 1];

        String link = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertEquals("Your account has been updated.", infoPage.getInfo());

        driver.navigate().to(link);

        errorPage.assertCurrent();
    }

    @Test
    public void sendResetPasswordEmailWithRedirect() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("myclient");
        client.setRedirectUris(new LinkedList<>());
        client.getRedirectUris().add("http://myclient.com/*");
        client.setName("myclient");
        client.setEnabled(true);
        Response response = managedRealm.admin().clients().create(client);
        String createdId = ApiUtil.getCreatedId(response);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientResourcePath(createdId), client, ResourceType.CLIENT);


        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());

        try {
            // test that an invalid redirect uri is rejected.
            user.executeActionsEmail("myclient", "http://unregistered-uri.com/", actions);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("Invalid redirect uri.", error.getErrorMessage());
        }


        user.executeActionsEmail("myclient", "http://myclient.com/home.html", actions);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[0];

        String link = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertEquals("Your account has been updated.", driver.findElement(By.id("kc-page-title")).getText());

        String pageSource = driver.getPageSource();

        // check to make sure the back link is set.
        Assertions.assertTrue(pageSource.contains("http://myclient.com/home.html"));

        driver.navigate().to(link);

        errorPage.assertCurrent();
    }

    @Test
    public void sendResetPasswordEmailWithRedirectAndCustomLifespan() throws IOException {

        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("myclient");
        client.setRedirectUris(new LinkedList<>());
        client.getRedirectUris().add("http://myclient.com/*");
        client.setName("myclient");
        client.setEnabled(true);
        Response response = managedRealm.admin().clients().create(client);
        String createdId = ApiUtil.getCreatedId(response);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientResourcePath(createdId), client, ResourceType.CLIENT);


        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());

        final int lifespan = (int) TimeUnit.DAYS.toSeconds(128);

        try {
            // test that an invalid redirect uri is rejected.
            user.executeActionsEmail("myclient", "http://unregistered-uri.com/", lifespan, actions);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("Invalid redirect uri.", error.getErrorMessage());
        }


        user.executeActionsEmail("myclient", "http://myclient.com/home.html", lifespan, actions);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/execute-actions-email", ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[0];

        MailUtils.EmailBody body = MailUtils.getBody(message);

        assertTrue(body.getText().contains("This link will expire within 128 days"));
        assertTrue(body.getHtml().contains("This link will expire within 128 days"));

        String link = MailUtils.getPasswordResetEmailLink(message);

        String token = link.substring(link.indexOf("key=") + "key=".length());

        try {
            final AccessToken accessToken = TokenVerifier.create(token, AccessToken.class).getToken();
            assertEquals(lifespan, accessToken.getExp() - accessToken.getIat());
        } catch (VerificationException e) {
            throw new IOException(e);
        }

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Update Password"));
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertEquals("Your account has been updated.", driver.findElement(By.id("kc-page-title")).getText());

        String pageSource = driver.getPageSource();

        // check to make sure the back link is set.
        Assertions.assertTrue(pageSource.contains("http://myclient.com/home.html"));

        driver.navigate().to(link);

        errorPage.assertCurrent();
    }


    @Test
    public void sendVerifyEmail() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").build();

        String id = createUser(userRep);
        UserResource user = managedRealm.admin().users().get(id);

        try {
            user.sendVerifyEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User email missing", error.getErrorMessage());
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
            Assertions.assertEquals("User is disabled", error.getErrorMessage());
            Assertions.assertNull(adminEvents.poll());
        }
        try {
            userRep.setEnabled(true);
            updateUser(user, userRep);

            user.sendVerifyEmail("invalidClientId");
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("Client doesn't exist", error.getErrorMessage());
            Assertions.assertNull(adminEvents.poll());
        }

        user.sendVerifyEmail();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/send-verify-email", ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

        String link = MailUtils.getPasswordResetEmailLink(mailServer.getReceivedMessages()[0]);

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Confirm validity of e-mail address"));
        proceedPage.clickProceedLink();

        Assertions.assertEquals("Your account has been updated.", infoPage.getInfo());
        driver.navigate().to("about:blank");

        driver.navigate().to(link);
        infoPage.assertCurrent();
        assertEquals("Your email address has been verified already.", infoPage.getInfo());
    }

    @Test
    public void sendVerifyEmailWithRedirect() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);

        String clientId = client.getClientId();
        String redirectUri = keycloakUrls.getBase() + "/auth/some-page";
        try {
            // test that an invalid redirect uri is rejected.
            user.sendVerifyEmail(clientId, "http://unregistered-uri.com/");
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("Invalid redirect uri.", error.getErrorMessage());
        }


        user.sendVerifyEmail(clientId, redirectUri);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/send-verify-email", ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[0];

        String link = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Confirm validity of e-mail address"));
        proceedPage.clickProceedLink();

        assertEquals("Your account has been updated.", infoPage.getInfo());

        String pageSource = driver.getPageSource();
        Assertions.assertTrue(pageSource.contains(redirectUri));
    }

    @Test
    public void sendVerifyEmailWithRedirectAndCustomLifespan() throws IOException {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@test.com").build();

        String id = createUser(userRep);

        UserResource user = managedRealm.admin().users().get(id);

        final int lifespan = (int) TimeUnit.DAYS.toSeconds(1);
        String redirectUri = keycloakUrls.getBase() + "/auth/some-page";
        try {
            // test that an invalid redirect uri is rejected.
            user.sendVerifyEmail(client.getClientId(), "http://unregistered-uri.com/", lifespan);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("Invalid redirect uri.", error.getErrorMessage());
        }


        user.sendVerifyEmail(client.getClientId(), redirectUri, lifespan);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResourcePath(id) + "/send-verify-email", ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);
        MimeMessage message = mailServer.getReceivedMessages()[0];

        MailUtils.EmailBody body = MailUtils.getBody(message);
        assertThat(body.getText(), Matchers.containsString("This link will expire within 1 day"));
        assertThat(body.getHtml(), Matchers.containsString("This link will expire within 1 day"));

        String link = MailUtils.getPasswordResetEmailLink(message);
        String token = link.substring(link.indexOf("key=") + "key=".length());

        try {
            final AccessToken accessToken = TokenVerifier.create(token, AccessToken.class).getToken();
            assertEquals(lifespan, accessToken.getExp() - accessToken.getIat());
        } catch (VerificationException e) {
            throw new IOException(e);
        }

        driver.navigate().to(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Confirm validity of e-mail address"));
        proceedPage.clickProceedLink();

        assertEquals("Your account has been updated.", infoPage.getInfo());

        String pageSource = driver.getPageSource();
        Assertions.assertTrue(pageSource.contains(redirectUri));
    }

    private void updateRealmFrontEndUrl(RealmResource realm, String url) throws Exception {
        RealmRepresentation master = realm.toRepresentation();
        Map<String, String> attributes = Optional.ofNullable(master.getAttributes()).orElse(new HashMap<>());

        if (url == null) {
            attributes.remove("frontendUrl");
        } else {
            attributes.put("frontendUrl", url);
        }

        realm.update(master);
    }

    private static class UserEmailTestAppClientConf implements ClientConfig {

        public ClientConfigBuilder configure(ClientConfigBuilder builder) {
            builder.clientId("test-app-email");
            builder.secret("password");
            builder.baseUrl("http://localhost:8080/auth/");
            builder.redirectUris("http://localhost:8080/auth/*");
            builder.adminUrl("http://localhost:8080/auth/admin");

            return builder;
        }
    }
}
