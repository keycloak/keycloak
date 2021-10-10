package org.keycloak.testsuite.admin;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.keycloak.models.AdminRoles.REALM_ADMIN;
import static org.keycloak.models.Constants.ADMIN_CLI_CLIENT_ID;
import static org.keycloak.models.Constants.REALM_MANAGEMENT_CLIENT_ID;
import static org.keycloak.testsuite.util.AdminClientUtil.createAdminClient;

public class AdminConsoleWhoAmILocaleTest extends AbstractKeycloakTest {

    private static final String REALM_I18N_OFF = "realm-i18n-off";
    private static final String REALM_I18N_ON = "realm-i18n-on";
    private static final String USER_WITHOUT_LOCALE = "user-without-locale";
    private static final String USER_WITH_LOCALE = "user-with-locale";
    private static final String PASSWORD = "password";
    private static final String DEFAULT_LOCALE = "en";
    private static final String REALM_LOCALE = "no";
    private static final String USER_LOCALE = "de";
    private static final String EXTRA_LOCALE = "zh-CN";

    private CloseableHttpClient client;

    @Before
    public void createHttpClient() throws Exception {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void closeHttpClient() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realm = RealmBuilder.create()
            .name(REALM_I18N_OFF)
            .internationalizationEnabled(false);
        realm.user(UserBuilder.create()
            .username(USER_WITHOUT_LOCALE)
            .password(PASSWORD)
            .role(REALM_MANAGEMENT_CLIENT_ID, REALM_ADMIN));
        realm.user(UserBuilder.create()
            .username(USER_WITH_LOCALE)
            .password(PASSWORD)
            .addAttribute("locale", USER_LOCALE)
            .role(REALM_MANAGEMENT_CLIENT_ID, REALM_ADMIN));
        testRealms.add(realm.build());
        
        realm = RealmBuilder.create()
            .name(REALM_I18N_ON)
            .internationalizationEnabled(true)
            .supportedLocales(new HashSet<>(asList(REALM_LOCALE, USER_LOCALE, EXTRA_LOCALE)))
            .defaultLocale(REALM_LOCALE);
        realm.user(UserBuilder.create()
            .username(USER_WITHOUT_LOCALE)
            .password(PASSWORD)
            .role(REALM_MANAGEMENT_CLIENT_ID, REALM_ADMIN));
        realm.user(UserBuilder.create()
            .username(USER_WITH_LOCALE)
            .password(PASSWORD)
            .addAttribute("locale", USER_LOCALE)
            .role(REALM_MANAGEMENT_CLIENT_ID, REALM_ADMIN));
        testRealms.add(realm.build());
    }

    private String accessToken(String realmName, String username) throws Exception {
        try (Keycloak adminClient = createAdminClient(true, realmName, username, PASSWORD, ADMIN_CLI_CLIENT_ID, null)) {
            AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            assertNotNull(accessToken);
            return accessToken.getToken();
        }
    }

    private String whoAmiUrl(String realmName) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/admin/" + realmName + "/console/whoami";
    }

    @Test
    public void testLocaleRealmI18nDisabledUserWithoutLocale() throws Exception {
        JsonNode whoAmI = SimpleHttp
            .doGet(whoAmiUrl(REALM_I18N_OFF), client)
            .header("Accept", "application/json")
            .auth(accessToken(REALM_I18N_OFF, USER_WITHOUT_LOCALE))
            .asJson();
        assertEquals(REALM_I18N_OFF, whoAmI.get("realm").asText());
        assertEquals(DEFAULT_LOCALE, whoAmI.get("locale").asText());
    }

    @Test
    public void testLocaleRealmI18nDisabledUserWithLocale() throws Exception {
        JsonNode whoAmI = SimpleHttp
            .doGet(whoAmiUrl(REALM_I18N_OFF), client)
            .header("Accept", "application/json")
            .auth(accessToken(REALM_I18N_OFF, USER_WITH_LOCALE))
            .asJson();
        assertEquals(REALM_I18N_OFF, whoAmI.get("realm").asText());
        assertEquals(DEFAULT_LOCALE, whoAmI.get("locale").asText());
    }

    @Test
    public void testLocaleRealmI18nEnabledUserWithoutLocale() throws Exception {
        JsonNode whoAmI = SimpleHttp
            .doGet(whoAmiUrl(REALM_I18N_ON), client)
            .header("Accept", "application/json")
            .auth(accessToken(REALM_I18N_ON, USER_WITHOUT_LOCALE))
            .asJson();
        assertEquals(REALM_I18N_ON, whoAmI.get("realm").asText());
        assertEquals(REALM_LOCALE, whoAmI.get("locale").asText());
    }

    @Test
    public void testLocaleRealmI18nEnabledUserWithLocale() throws Exception {
        JsonNode whoAmI = SimpleHttp
            .doGet(whoAmiUrl(REALM_I18N_ON), client)
            .header("Accept", "application/json")
            .auth(accessToken(REALM_I18N_ON, USER_WITH_LOCALE))
            .asJson();
        assertEquals(REALM_I18N_ON, whoAmI.get("realm").asText());
        assertEquals(USER_LOCALE, whoAmI.get("locale").asText());
    }

    @Test
    public void testLocaleRealmI18nEnabledAcceptLanguageHeader() throws Exception {
        JsonNode whoAmI = SimpleHttp
            .doGet(whoAmiUrl(REALM_I18N_ON), client)
            .header("Accept", "application/json")
            .auth(accessToken(REALM_I18N_ON, USER_WITHOUT_LOCALE))
            .header("Accept-Language", EXTRA_LOCALE)
            .asJson();
        assertEquals(REALM_I18N_ON, whoAmI.get("realm").asText());
        assertEquals(EXTRA_LOCALE, whoAmI.get("locale").asText());
    }

    @Test
    public void testLocaleRealmI18nEnabledKeycloakLocaleCookie() throws Exception {
        JsonNode whoAmI = SimpleHttp
            .doGet(whoAmiUrl(REALM_I18N_ON), client)
            .header("Accept", "application/json")
            .auth(accessToken(REALM_I18N_ON, USER_WITHOUT_LOCALE))
            .header("Cookie", "KEYCLOAK_LOCALE=" + EXTRA_LOCALE)
            .asJson();
        assertEquals(REALM_I18N_ON, whoAmI.get("realm").asText());
        assertEquals(EXTRA_LOCALE, whoAmI.get("locale").asText());
    }
}
