package org.keycloak.tests.admin;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.Constants.ADMIN_CLI_CLIENT_ID;

@KeycloakIntegrationTest
public class AdminConsoleWhoAmILocaleTest {

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectRealm(ref = "realm-i18n-off", config = LocaleOffRealmConfig.class)
    ManagedRealm managedRealmOff;

    @InjectRealm(ref = "realm-i18n-on", config = LocaleOnRealmConfig.class)
    ManagedRealm managedRealmOn;

    @InjectUser(realmRef = "master", config = MasterAdminUserConfig.class)
    ManagedUser masterAdmin;

    @InjectClient(realmRef = "master", config = MasterAdminClientConfig.class)
    ManagedClient masterClient;

    @InjectOAuthClient(ref = "master", realmRef = "master")
    OAuthClient oAuthClientMaster;

    @InjectOAuthClient(ref = "locale-off", realmRef = "realm-i18n-off")
    OAuthClient oAuthClientLocaleOff;

    @InjectOAuthClient(ref = "locale-on", realmRef = "realm-i18n-on")
    OAuthClient oAuthClientLocaleOn;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    CloseableHttpClient client;

    private static final String REALM_I18N_OFF = "realm-i18n-off";
    private static final String REALM_I18N_ON = "realm-i18n-on";
    private static final String USER_WITHOUT_LOCALE = "user-without-locale";
    private static final String USER_WITH_LOCALE = "user-with-locale";
    private static final String USER_NO_ACCESS = "user-no-access";
    private static final String PASSWORD = "password";
    private static final String ADMIN_CLI_NOT_ALLOWED = "admin-cli-not-allowed";
    private static final String SECRET = "secret";
    private static final String DEFAULT_LOCALE = "en";
    private static final String REALM_LOCALE = "no";
    private static final String USER_LOCALE = "de";
    private static final String EXTRA_LOCALE = "zh-CN";

    @Test
    public void testLocaleRealmI18nDisabledUserWithoutLocale() throws Exception {
        AccessTokenResponse response = accessToken(oAuthClientLocaleOff, ADMIN_CLI_CLIENT_ID, SECRET, USER_WITHOUT_LOCALE, PASSWORD);

        JsonNode whoAmI = getHttpJsonResponse(whoAmiUrl(managedRealmOff), response);

        Assertions.assertEquals(REALM_I18N_OFF, whoAmI.get("realm").asText());
        Assertions.assertEquals(DEFAULT_LOCALE, whoAmI.get("locale").asText());
        checkRealmAccess(REALM_I18N_OFF, whoAmI);

        oAuthClientLocaleOff.doLogout(response.getRefreshToken());
    }

    @Test
    public void testLocaleRealmI18nDisabledUserWithLocale() throws Exception {
        AccessTokenResponse response = accessToken(oAuthClientLocaleOff, ADMIN_CLI_CLIENT_ID, SECRET, USER_WITH_LOCALE, PASSWORD);

        JsonNode whoAmI = getHttpJsonResponse(whoAmiUrl(managedRealmOff), response);

        Assertions.assertEquals(REALM_I18N_OFF, whoAmI.get("realm").asText());
        Assertions.assertEquals(DEFAULT_LOCALE, whoAmI.get("locale").asText());
        checkRealmAccess(REALM_I18N_OFF, whoAmI);

        oAuthClientLocaleOff.doLogout(response.getRefreshToken());
    }

    @Test
    public void testLocaleRealmI18nEnabledUserWithoutLocale() throws Exception {
        AccessTokenResponse response = accessToken(oAuthClientLocaleOn, ADMIN_CLI_CLIENT_ID, SECRET, USER_WITHOUT_LOCALE, PASSWORD);

        JsonNode whoAmI = getHttpJsonResponse(whoAmiUrl(managedRealmOn), response);

        Assertions.assertEquals(REALM_I18N_ON, whoAmI.get("realm").asText());
        Assertions.assertEquals(REALM_LOCALE, whoAmI.get("locale").asText());
        checkRealmAccess(REALM_I18N_ON, whoAmI);

        oAuthClientLocaleOn.doLogout(response.getRefreshToken());
    }

    @Test
    public void testLocaleRealmI18nEnabledUserWithLocale() throws Exception {
        AccessTokenResponse response = accessToken(oAuthClientLocaleOn, ADMIN_CLI_CLIENT_ID, SECRET, USER_WITH_LOCALE, PASSWORD);

        JsonNode whoAmI = getHttpJsonResponse(whoAmiUrl(managedRealmOn), response);

        Assertions.assertEquals(REALM_I18N_ON, whoAmI.get("realm").asText());
        Assertions.assertEquals(USER_LOCALE, whoAmI.get("locale").asText());
        checkRealmAccess(REALM_I18N_ON, whoAmI);

        oAuthClientLocaleOn.doLogout(response.getRefreshToken());
    }

    @Test
    public void testLocaleRealmI18nEnabledAcceptLanguageHeader() throws Exception {
        AccessTokenResponse response = accessToken(oAuthClientLocaleOn, ADMIN_CLI_CLIENT_ID, SECRET, USER_WITHOUT_LOCALE, PASSWORD);

        JsonNode whoAmI = getHttpJsonResponse(whoAmiUrl(managedRealmOn), response, new BasicHeader("Accept-Language", EXTRA_LOCALE));

        Assertions.assertEquals(REALM_I18N_ON, whoAmI.get("realm").asText());
        Assertions.assertEquals(EXTRA_LOCALE, whoAmI.get("locale").asText());
        checkRealmAccess(REALM_I18N_ON, whoAmI);

        oAuthClientLocaleOn.doLogout(response.getRefreshToken());
    }

    @Test
    public void testLocaleRealmI18nEnabledKeycloakLocaleCookie() throws Exception {
        AccessTokenResponse response = accessToken(oAuthClientLocaleOn, ADMIN_CLI_CLIENT_ID, SECRET, USER_WITHOUT_LOCALE, PASSWORD);

        JsonNode whoAmI = getHttpJsonResponse(whoAmiUrl(managedRealmOn), response, new BasicHeader("Cookie", "KEYCLOAK_LOCALE=" + EXTRA_LOCALE));

        Assertions.assertEquals(REALM_I18N_ON, whoAmI.get("realm").asText());
        Assertions.assertEquals(EXTRA_LOCALE, whoAmI.get("locale").asText());
        checkRealmAccess(REALM_I18N_ON, whoAmI);

        oAuthClientLocaleOn.doLogout(response.getRefreshToken());
    }

    @Test
    public void testMasterRealm() throws Exception {
        updateMasterAdminRole();
        AccessTokenResponse response = accessToken(oAuthClientMaster, masterClient.getClientId(), SECRET, masterAdmin.getUsername(), PASSWORD);
        JsonNode whoAmI = getHttpJsonResponse(whoAmiUrl(masterRealm), response);
        Assertions.assertEquals(masterRealm.getName(), whoAmI.get("realm").asText());
        Assertions.assertEquals(DEFAULT_LOCALE, whoAmI.get("locale").asText());
        checkRealmAccess(masterRealm.getName(), whoAmI);
        oAuthClientMaster.doLogout(response.getRefreshToken());
    }

    @Test
    public void testMasterRealmCurrentRealm() throws Exception {
        updateMasterAdminRole();
        AccessTokenResponse response = accessToken(oAuthClientMaster, masterClient.getClientId(), SECRET, masterAdmin.getUsername(), PASSWORD);
        JsonNode whoAmI = getHttpJsonResponse(whoAmiUrl(masterRealm, REALM_I18N_ON), response);
        Assertions.assertEquals(masterRealm.getName(), whoAmI.get("realm").asText());
        Assertions.assertEquals(DEFAULT_LOCALE, whoAmI.get("locale").asText());
        checkRealmAccess(REALM_I18N_ON, whoAmI);
        oAuthClientMaster.doLogout(response.getRefreshToken());
    }

    @Test
    public void testLocaleRealmNoToken() throws Exception {
        HttpGet httpGet = new HttpGet(whoAmiUrl(managedRealmOn));
        httpGet.addHeader("Accept", "application/json");

        CloseableHttpResponse httpGetResponse = client.execute(httpGet);
        Assertions.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), httpGetResponse.getStatusLine().getStatusCode());
        httpGetResponse.close();
    }

    @Test
    public void testLocaleRealmUserNoAccess() throws Exception {
        AccessTokenResponse response = accessToken(oAuthClientLocaleOn, ADMIN_CLI_CLIENT_ID, SECRET, USER_NO_ACCESS, PASSWORD);

        HttpGet httpGet = new HttpGet(whoAmiUrl(managedRealmOn));
        httpGet.addHeader("Accept", "application/json");
        httpGet.addHeader("Authorization", "Bearer " + response.getAccessToken());

        CloseableHttpResponse httpGetResponse = client.execute(httpGet);

        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpGetResponse.getStatusLine().getStatusCode());
        httpGetResponse.close();
        oAuthClientLocaleOn.doLogout(response.getRefreshToken());
    }

    @Test
    public void testLocaleRealmTokenAdminNotAllowed() throws Exception {
        AccessTokenResponse response = accessToken(oAuthClientLocaleOn, ADMIN_CLI_NOT_ALLOWED, SECRET, USER_WITH_LOCALE, PASSWORD);

        Assertions.assertNotNull(response.getAccessToken());

        HttpGet httpGet = new HttpGet(whoAmiUrl(managedRealmOn));
        httpGet.addHeader("Accept", "application/json");
        httpGet.addHeader("Authorization", "Bearer " + response.getAccessToken());
        CloseableHttpResponse httpGetResponse = client.execute(httpGet);

        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpGetResponse.getStatusLine().getStatusCode());
        httpGetResponse.close();
    }

    private void updateMasterAdminRole() {
        RoleRepresentation roleRep = masterRealm.admin().roles().get("admin").toRepresentation();
        masterRealm.admin().users().get(masterAdmin.getId()).roles().realmLevel().add(List.of(roleRep));
    }

    private AccessTokenResponse accessToken(OAuthClient oAuth, String clientId, String clientSecret, String username, String password) {
        return oAuth.client(clientId, clientSecret).doPasswordGrantRequest(username, password);
    }

    private JsonNode getHttpJsonResponse(String url, AccessTokenResponse response, Header... headers) throws IOException{
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Accept", "application/json");
        httpGet.addHeader("Authorization", "Bearer " + response.getAccessToken());

        for (Header header : headers) {
            httpGet.addHeader(header.getName(), header.getValue());
        }

        return new ObjectMapper().readTree(client.execute(httpGet).getEntity().getContent());
    }

    private String whoAmiUrl(ManagedRealm realm) {
        return whoAmiUrl(realm, null);
    }

    private String whoAmiUrl(ManagedRealm realm, String currentRealm) {
        StringBuilder sb = new StringBuilder()
                .append(keycloakUrls.getBaseUrl())
                .append("/admin/")
                .append(realm.getName())
                .append("/console/whoami");
        if (currentRealm != null) {
            sb.append("?currentRealm=").append(currentRealm);
        }
        return sb.toString();
    }

    private void checkRealmAccess(String realm, JsonNode whoAmI) {
        Assertions.assertNotNull(whoAmI.get("realm_access"));
        Assertions.assertNotNull(whoAmI.get("realm_access").get(realm));
        Assertions.assertTrue(whoAmI.get("realm_access").get(realm).isArray());
        Assertions.assertTrue(whoAmI.get("realm_access").get(realm).size() > 0);
    }

    private static class LocaleOffRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.internationalizationEnabled(false);
            realm.addUser(USER_WITHOUT_LOCALE).password(PASSWORD)
                    .name("My", "Locale Off")
                    .email("locale-off@email.org").emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
            realm.addUser(USER_WITH_LOCALE).password(PASSWORD)
                    .name("My", "Locale On")
                    .email("locale-on@email.org").emailVerified(true)
                    .attribute("locale", USER_LOCALE)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
            realm.addClient(ADMIN_CLI_CLIENT_ID).name(ADMIN_CLI_CLIENT_ID).secret(SECRET)
                    .attribute(Constants.SECURITY_ADMIN_CONSOLE_ATTR, "true")
                    .directAccessGrantsEnabled(true);

            return realm;
        }
    }

    private static class LocaleOnRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.internationalizationEnabled(true);
            realm.supportedLocales(REALM_LOCALE, USER_LOCALE, EXTRA_LOCALE);
            realm.defaultLocale(REALM_LOCALE);

            realm.addUser(USER_WITHOUT_LOCALE).password(PASSWORD)
                    .name("My", "Locale Off")
                    .email("locale-off@email.org").emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
            realm.addUser(USER_WITH_LOCALE).password(PASSWORD)
                    .email("locale-on@email.org").emailVerified(true).name("My", "Locale On")
                    .attribute("locale", USER_LOCALE)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
            realm.addUser(USER_NO_ACCESS).password(PASSWORD)
                    .name("No", "Access")
                    .email("no-access@email.org").emailVerified(true)
                    .attribute("locale", USER_LOCALE);
            realm.addClient(ADMIN_CLI_CLIENT_ID).name(ADMIN_CLI_CLIENT_ID).secret(SECRET)
                    .attribute(Constants.SECURITY_ADMIN_CONSOLE_ATTR, "true")
                    .directAccessGrantsEnabled(true);
            realm.addClient(ADMIN_CLI_NOT_ALLOWED).name(ADMIN_CLI_NOT_ALLOWED).secret(SECRET)
                    .attribute(Constants.SECURITY_ADMIN_CONSOLE_ATTR, null)
                    .directAccessGrantsEnabled(true);

            return realm;
        }
    }

    private static class MasterAdminUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            user.username("master-admin");
            user.password(PASSWORD);
            user.name("My", "Admin");
            user.roles("admin");
            user.email("master-admin@email.org");
            user.emailVerified(true);
            user.attribute("locale", DEFAULT_LOCALE);


            return user;
        }
    }

    private static class MasterAdminClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            client.clientId("master-admin-cli");
            client.name("master-admin-cli");
            client.secret(SECRET);
            client.attribute(Constants.SECURITY_ADMIN_CONSOLE_ATTR, "true");
            client.directAccessGrantsEnabled(true);

            return client;
        }
    }
}
