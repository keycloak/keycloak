package org.keycloak.testsuite.springboot;

import static org.keycloak.testsuite.admin.ApiUtil.assignRealmRoles;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;

public abstract class AbstractSpringBootTest extends AbstractKeycloakTest {

    protected static final String REALM_ID = "cd8ee421-5100-41ba-95dd-b27c8e5cf042";

    protected static final String REALM_NAME = "test";

    protected static final String CLIENT_ID = "spring-boot-app";
    protected static final String SECRET = "e3789ac5-bde6-4957-a7b0-612823dac101";

    protected static final String APPLICATION_URL = "http://localhost:8280";
    protected static final String BASE_URL = APPLICATION_URL + "/admin";

    protected static final String USER_LOGIN = "testuser";
    protected static final String USER_EMAIL = "user@email.test";
    protected static final String USER_PASSWORD = "user-password";

    protected static final String USER_LOGIN_2 = "testuser2";
    protected static final String USER_EMAIL_2 = "user2@email.test";
    protected static final String USER_PASSWORD_2 = "user2-password";

    protected static final String CORRECT_ROLE = "admin";
    protected static final String INCORRECT_ROLE = "wrong-admin";

    protected static final String REALM_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5" +
            "mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi7" +
            "9NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

    protected static final String REALM_PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3Bj" +
            "LGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vj" +
            "O2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jY" +
            "lQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn" +
            "9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEK" +
            "Xalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2w" +
            "Vl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJ" +
            "AY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZ" +
            "N39fOYAlo+nTixgeW7X8Y=";

    @Page
    protected LoginPage loginPage;

    @Page
    protected SpringApplicationPage applicationPage;

    @Page
    protected SpringAdminPage adminPage;
    
    @Page
    protected TokenPage tokenPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setId(REALM_ID);
        realm.setRealm(REALM_NAME);
        realm.setEnabled(true);

        realm.setPublicKey(REALM_PUBLIC_KEY);
        realm.setPrivateKey(REALM_PRIVATE_KEY);

        realm.setClients(Collections.singletonList(createClient()));

        List<String> eventListeners = new ArrayList<>();
        eventListeners.add("jboss-logging");
        eventListeners.add("event-queue");
        realm.setEventsListeners(eventListeners);

        testRealms.add(realm);
    }

    private ClientRepresentation createClient() {
        ClientRepresentation clientRepresentation = new ClientRepresentation();

        clientRepresentation.setId(CLIENT_ID);
        clientRepresentation.setSecret(SECRET);

        clientRepresentation.setBaseUrl(BASE_URL);
        clientRepresentation.setRedirectUris(Collections.singletonList(BASE_URL + "/*"));
        clientRepresentation.setAdminUrl(BASE_URL);

        return clientRepresentation;
    }

    private void addUser(String login, String email, String password, String... roles) {
        UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setUsername(login);
        userRepresentation.setEmail(email);
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);

        RealmResource realmResource = adminClient.realm(REALM_NAME);
        String userId = createUserWithAdminClient(realmResource, userRepresentation);

        resetUserPassword(realmResource.users().get(userId), password, false);

        for (String role : roles)
            assignRealmRoles(realmResource, userId, role);
    }

    private String getAuthRoot(SuiteContext suiteContext) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    private String encodeUrl(String url) {
        String result;
        try {
            result = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            result = url;
        }

        return result;
    }
    
    protected String logoutPage(String redirectUrl) {
    	return getAuthRoot(suiteContext)
                + "/auth/realms/" + REALM_NAME
                + "/protocol/" + "openid-connect"
                + "/logout?redirect_uri=" + encodeUrl(redirectUrl);
    }

    protected void setAdapterAndServerTimeOffset(int timeOffset, String url) {
        setTimeOffset(timeOffset);

        String timeOffsetUri = UriBuilder.fromUri(url)
                .queryParam("timeOffset", timeOffset)
                .build().toString();

        driver.navigate().to(timeOffsetUri);
        WaitUtils.waitUntilElement(By.tagName("body")).is().visible();
    }

    protected String getCorrectUserId() {
        return adminClient.realms().realm(REALM_NAME).users().search(USER_LOGIN)
                .get(0).getId();
    }

    @Before
    public void createRoles() {
        RealmResource realm = realmsResouce().realm(REALM_NAME);

        RoleRepresentation correct = new RoleRepresentation(CORRECT_ROLE, CORRECT_ROLE, false);
        realm.roles().create(correct);

        RoleRepresentation incorrect = new RoleRepresentation(INCORRECT_ROLE, INCORRECT_ROLE, false);
        realm.roles().create(incorrect);
    }

    @Before
    public void addUsers() {
        addUser(USER_LOGIN, USER_EMAIL, USER_PASSWORD, CORRECT_ROLE);
        addUser(USER_LOGIN_2, USER_EMAIL_2, USER_PASSWORD_2, INCORRECT_ROLE);
    }

    @After
    public void cleanupUsers() {
        RealmResource providerRealm = adminClient.realm(REALM_NAME);
        UserRepresentation userRep = ApiUtil.findUserByUsername(providerRealm, USER_LOGIN);
        if (userRep != null) {
            providerRealm.users().get(userRep.getId()).remove();
        }

        RealmResource childRealm = adminClient.realm(REALM_NAME);
        userRep = ApiUtil.findUserByUsername(childRealm, USER_LOGIN_2);
        if (userRep != null) {
            childRealm.users().get(userRep.getId()).remove();
        }
    }

    @After
    public void cleanupRoles() {
        RealmResource realm = realmsResouce().realm(REALM_NAME);

        RoleResource correctRole = realm.roles().get(CORRECT_ROLE);
        correctRole.remove();

        RoleResource incorrectRole = realm.roles().get(INCORRECT_ROLE);
        incorrectRole.remove();
    }
}
