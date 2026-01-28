package org.keycloak.tests.admin.realm;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Time;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.TestApp;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectTestApp;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class RealmOAuthActionsTest extends AbstractRealmTest {

    @InjectOAuthClient(ref = "managedOAuth", realmRef = "managedRealm")
    OAuthClient oauth;

    @InjectTestApp
    TestApp testApp;

    @InjectEvents(ref = "managedEvents", realmRef = "managedRealm")
    Events events;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void pushNotBefore() throws InterruptedException {
        setupTestAppAndUser();

        int time = Time.currentTime() - 60;

        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        rep.setNotBefore(time);
        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).representation(rep).resourceType(ResourceType.REALM);

        GlobalRequestResult globalRequestResult = managedRealm.admin().pushRevocation();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, "push-revocation", globalRequestResult, ResourceType.REALM);

        assertThat(globalRequestResult.getSuccessRequests(), containsInAnyOrder(testApp.getAdminUri()));
        assertNull(globalRequestResult.getFailedRequests());

        PushNotBeforeAction adminPushNotBefore = testApp.kcAdmin().getAdminPushNotBefore();
        assertEquals(time, adminPushNotBefore.getNotBefore());

        managedRealm.admin().clients().get("test-app-new").remove();
        managedRealm.admin().users().get(AdminApiUtil.findUserByUsername(managedRealm.admin(), "testuser").getId()).remove();
    }

    @Test
    public void pushNotBeforeWithSamlApp() throws InterruptedException {
        setupTestAppAndUser();
        setupTestSamlApp();

        int time = Time.currentTime() - 60;

        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        rep.setNotBefore(time);
        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).representation(rep).resourceType(ResourceType.REALM);

        GlobalRequestResult globalRequestResult = managedRealm.admin().pushRevocation();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, "push-revocation", globalRequestResult, ResourceType.REALM);

        assertThat(globalRequestResult.getSuccessRequests(), containsInAnyOrder(testApp.getAdminUri()));
        assertThat(globalRequestResult.getFailedRequests(), containsInAnyOrder(keycloakUrls.getBase() + "/realms/" + managedRealm.getName() + "/saml-app/saml"));

        PushNotBeforeAction adminPushNotBefore = testApp.kcAdmin().getAdminPushNotBefore();
        assertEquals(time, adminPushNotBefore.getNotBefore());

        managedRealm.admin().clients().get("test-saml-app").remove();
        managedRealm.admin().clients().get("test-app-new").remove();
        managedRealm.admin().users().get(AdminApiUtil.findUserByUsername(managedRealm.admin(), "testuser").getId()).remove();
    }

    @Test
    public void logoutAll() throws InterruptedException {
        setupTestAppAndUser();

        Response response = managedRealm.admin().users().create(UserConfigBuilder.create().username("user").name("User", "Name").email("user@name").emailVerified(true).build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userResourcePath(userId), ResourceType.USER);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");

        managedRealm.admin().users().get(userId).resetPassword(credential);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResetPasswordPath(userId), ResourceType.USER);

        oauth.doPasswordGrantRequest("user", "password");

        GlobalRequestResult globalRequestResult = managedRealm.admin().logoutAll();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, "logout-all", globalRequestResult, ResourceType.REALM);

        assertEquals(1, globalRequestResult.getSuccessRequests().size());
        assertEquals(testApp.getAdminUri(), globalRequestResult.getSuccessRequests().get(0));
        assertNull(globalRequestResult.getFailedRequests());

        assertNotNull(testApp.kcAdmin().getAdminLogoutAction());

        managedRealm.admin().clients().get("test-app-new").remove();
        managedRealm.admin().users().get(AdminApiUtil.findUserByUsername(managedRealm.admin(), "testuser").getId()).remove();
    }

    @Test
    public void deleteSession() {
        setupTestAppAndUser();

        oauth.doLogin("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(200, tokenResponse.getStatusCode());

        EventRepresentation event = events.poll();
        assertNotNull(event);

        managedRealm.admin().deleteSession(event.getSessionId(), false);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.deleteSessionPath(event.getSessionId()), ResourceType.USER_SESSION);
        try {
            managedRealm.admin().deleteSession(event.getSessionId(), false);
            fail("Expected 404");
        } catch (NotFoundException e) {
            // Expected
            Assertions.assertNull(adminEvents.poll());
        }

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("Session not active", tokenResponse.getErrorDescription());

        managedRealm.admin().clients().get("test-app-new").remove();
        managedRealm.admin().users().get(AdminApiUtil.findUserByUsername(managedRealm.admin(), "testuser").getId()).remove();
    }

    @Test
    public void clientSessionStats() {
        setupTestAppAndUser();

        List<Map<String, String>> sessionStats = managedRealm.admin().getClientSessionStats();
        assertTrue(sessionStats.isEmpty());

        oauth.doLogin("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(200, tokenResponse.getStatusCode());

        sessionStats = managedRealm.admin().getClientSessionStats();

        assertEquals(1, sessionStats.size());
        assertEquals("test-app-new", sessionStats.get(0).get("clientId"));
        assertEquals("1", sessionStats.get(0).get("active"));

        String clientUuid = sessionStats.get(0).get("id");
        managedRealm.admin().clients().get(clientUuid).remove();

        sessionStats = managedRealm.admin().getClientSessionStats();

        assertEquals(0, sessionStats.size());

        managedRealm.admin().users().get(AdminApiUtil.findUserByUsername(managedRealm.admin(), "testuser").getId()).remove();
    }

    private void setupTestAppAndUser() {
        testApp.kcAdmin().clear();

        ClientRepresentation client = ClientConfigBuilder.create()
                .id("test-app-new")
                .clientId("test-app-new")
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .adminUrl(testApp.getAdminUri())
                .redirectUris(testApp.getRedirectionUri())
                .secret("secret")
                .build();
        Response resp = managedRealm.admin().clients().create(client);
        String clientDbId = ApiUtil.getCreatedId(resp);
        resp.close();

        client.setSecret("**********"); // secrets are masked in events
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientResourcePath(clientDbId), client, ResourceType.CLIENT);

        oauth.client("test-app-new", "secret");

        UserRepresentation userRep = UserConfigBuilder.create().username("testuser").name("Test", "User").email("test@user").emailVerified(true).build();
        Response response = managedRealm.admin().users().create(userRep);
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userResourcePath(userId), userRep, ResourceType.USER);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");

        managedRealm.admin().users().get(userId).resetPassword(credential);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResetPasswordPath(userId), ResourceType.USER);

        testApp.kcAdmin().clear();
    }

    private void setupTestSamlApp() {
        ClientRepresentation client = ClientConfigBuilder.create()
                .id("test-saml-app")
                .clientId("test-saml-app")
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .adminUrl(keycloakUrls.getBase() + "/realms/" + managedRealm.getName() + "/saml-app/saml")
                .redirectUris(oauth.getRedirectUri())
                .secret("secret")
                .build();
        Response resp = managedRealm.admin().clients().create(client);
        String clientDbId = ApiUtil.getCreatedId(resp);
        resp.close();

        client.setSecret("**********"); // secrets are masked in events
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientResourcePath(clientDbId), client, ResourceType.CLIENT);
    }
}
