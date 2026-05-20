package org.keycloak.tests.account;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = AccountRestServiceRolesTest.AccountRolesServerConfig.class)
public class AccountRestServiceRolesTest {

    private static final String PASSWORD = "password";

    @InjectRealm(config = AccountRolesRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @Test
    public void applicationsEndpointAccess() throws IOException {
        assertEndpointStatus("manage-account-user", "applications", 200);
        assertEndpointStatus("view-applications-user", "applications", 200);
        assertEndpointStatus("no-access-user", "applications", 403);
    }

    @Test
    public void groupsEndpointAccess() throws IOException {
        assertEndpointStatus("manage-account-user", "groups", 200);
        assertEndpointStatus("view-groups-user", "groups", 200);
        assertEndpointStatus("no-access-user", "groups", 403);
    }

    @Test
    public void verifiableCredentialsEndpointAccess() throws IOException {
        assertEndpointStatus("manage-account-user", "verifiable-credentials", 200);
        addAccountConsoleScopeMapping(AccountRoles.MANAGE_VERIFIABLE_CREDENTIALS);
        assertEndpointStatus("view-verifiable-credentials-user", "verifiable-credentials", 200);
        assertEndpointStatus("manage-verifiable-credentials-user", "verifiable-credentials", 200);
        assertEndpointStatus("no-access-user", "verifiable-credentials", 403);
    }

    @Test
    public void accountConsoleFeaturesForManageAccountUser() throws IOException {
        assertAccountConsoleFeatures("manage-account-user", true, true);
        JsonNode features = getAccountConsoleFeatures("manage-account-user");
        assertTrue(features.get("isOid4VciEnabled").asBoolean(), "isOid4VciEnabled should be true for manage-account-user");
    }

    @Test
    public void accountConsoleFeaturesForViewApplicationsUser() throws IOException {
        // view-applications is not in the account-console default scope (only manage-account and view-groups are)
        addAccountConsoleScopeMapping(AccountRoles.VIEW_APPLICATIONS);
        assertAccountConsoleFeatures("view-applications-user", true, false);
    }

    @Test
    public void accountConsoleFeaturesForViewGroupsUser() throws IOException {
        assertAccountConsoleFeatures("view-groups-user", false, false);
    }

    @Test
    public void accountConsoleFeaturesForViewGroupsUserWithGroup() throws IOException {
        assertAccountConsoleFeatures("view-groups-user-with-group", false, true);
    }

    @Test
    public void accountConsoleFeaturesForNoAccessUser() throws IOException {
        assertAccountConsoleFeatures("no-access-user", false, false);
    }

    @Test
    public void accountConsoleFeaturesForViewVerifiableCredentialsUser() throws IOException {
        // view-verifiable-credentials is not in the account-console default scope
        addAccountConsoleScopeMapping(AccountRoles.VIEW_VERIFIABLE_CREDENTIALS);
        JsonNode features = getAccountConsoleFeatures("view-verifiable-credentials-user");
        assertEquals(true, features.get("isOid4VciEnabled").asBoolean(),
                "isOid4VciEnabled should be true for view-verifiable-credentials-user");
    }

    @Test
    public void accountConsoleFeaturesForManageVerifiableCredentialsUser() throws IOException {
        // manage-verifiable-credentials is not in the account-console default scope
        addAccountConsoleScopeMapping(AccountRoles.MANAGE_VERIFIABLE_CREDENTIALS);
        JsonNode features = getAccountConsoleFeatures("manage-verifiable-credentials-user");
        assertEquals(true, features.get("isOid4VciEnabled").asBoolean(),
                "isOid4VciEnabled should be true for manage-verifiable-credentials-user");
    }

    private void addAccountConsoleScopeMapping(String roleName) {
        ClientResource accountConsole = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ACCOUNT_CONSOLE_CLIENT_ID);
        ClientResource accountManagementClient = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        assertNotNull(accountConsole);
        assertNotNull(accountManagementClient);

        String accountClientUuid = accountManagementClient.toRepresentation().getId();

        RoleRepresentation role = accountManagementClient.roles().get(roleName).toRepresentation();
        accountConsole.getScopeMappings().clientLevel(accountClientUuid).add(List.of(role));
    }

    private void assertAccountConsoleFeatures(String username, boolean expectApps, boolean expectGroups) throws IOException {
        JsonNode features = getAccountConsoleFeatures(username);
        assertEquals(expectApps, features.get("isViewApplicationsEnabled").asBoolean(),
                "isViewApplicationsEnabled mismatch for " + username + ", features: " + features);
        assertEquals(expectGroups, features.get("isViewGroupsEnabled").asBoolean(),
                "isViewGroupsEnabled mismatch for " + username + ", features: " + features);
    }

    private JsonNode getAccountConsoleFeatures(String username) throws IOException {
        // Disable redirect handling and manage cookies manually via headers,
        // same approach as ConcurrentLoginTest — Apache HttpClient's cookie store
        // does not reliably propagate cookies through redirect chains.
        try (CloseableHttpClient client = HttpClientBuilder.create()
                .disableRedirectHandling()
                .build()) {

            // Build authorization URL for account-console to trigger login flow
            // PKCE is required for the account-console public client
            PkceGenerator pkce = PkceGenerator.s256();
            String authUrl = realm.getBaseUrl() + "/protocol/openid-connect/auth"
                    + "?client_id=" + Constants.ACCOUNT_CONSOLE_CLIENT_ID
                    + "&redirect_uri=" + URLEncoder.encode(realm.getBaseUrl() + "/account/", StandardCharsets.UTF_8)
                    + "&response_type=code"
                    + "&scope=openid"
                    + "&" + OAuth2Constants.CODE_CHALLENGE + "=" + pkce.getCodeChallenge()
                    + "&" + OAuth2Constants.CODE_CHALLENGE_METHOD + "=" + pkce.getCodeChallengeMethod();

            // Step 1: GET login page, capture cookies from response
            HttpResponse formResponse = client.execute(new HttpGet(authUrl));
            String cookies = parseCookies(formResponse.getAllHeaders());
            String loginPageHtml = EntityUtils.toString(formResponse.getEntity());

            // Step 2: Extract action URL and POST credentials with cookies
            Matcher actionMatcher = Pattern.compile("action=\"([^\"]*)\"").matcher(loginPageHtml);
            assertTrue(actionMatcher.find(), "Login form action URL not found in HTML");
            String actionUrl = actionMatcher.group(1).replace("&amp;", "&");

            HttpPost loginRequest = new HttpPost(actionUrl);
            loginRequest.setHeader("Cookie", cookies);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("username", username));
            params.add(new BasicNameValuePair("password", PASSWORD));
            loginRequest.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

            HttpResponse loginResponse = client.execute(loginRequest);
            assertEquals(302, loginResponse.getStatusLine().getStatusCode(),
                    "Expected redirect after login, got " + loginResponse.getStatusLine().getStatusCode());

            // Merge cookies from login response (includes KEYCLOAK_IDENTITY)
            String allCookies = mergeCookies(cookies, parseCookies(loginResponse.getAllHeaders()));
            EntityUtils.consume(loginResponse.getEntity());

            // Step 3: Fetch account console HTML with session cookies
            HttpGet accountRequest = new HttpGet(realm.getBaseUrl() + "/account/");
            accountRequest.setHeader("Cookie", allCookies);

            String accountHtml;
            try (CloseableHttpResponse response = client.execute(accountRequest)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                accountHtml = EntityUtils.toString(response.getEntity());
            }

            // Parse features from <script id="environment"> JSON block
            Matcher envMatcher = Pattern.compile(
                    "<script id=\"environment\"[^>]*>\\s*(\\{.*?})\\s*</script>", Pattern.DOTALL
            ).matcher(accountHtml);
            assertTrue(envMatcher.find(), "Environment script block not found in account console HTML");

            JsonNode environment = new ObjectMapper().readTree(envMatcher.group(1));
            return environment.get("features");
        }
    }

    private String parseCookies(Header[] headers) {
        return Arrays.stream(headers)
                .filter(h -> h.getName().equals("Set-Cookie"))
                .map(h -> h.getValue().split(";")[0])
                .collect(Collectors.joining("; "));
    }

    private String mergeCookies(String existing, String additional) {
        if (existing.isEmpty()) return additional;
        if (additional.isEmpty()) return existing;
        return existing + "; " + additional;
    }

    private void assertEndpointStatus(String username, String endpoint, int expectedStatus) throws IOException {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(username, PASSWORD);
        assertTrue(tokenResponse.isSuccess(), "Token request failed for " + username + ": " + tokenResponse.getErrorDescription());

        HttpGet request = new HttpGet(realm.getBaseUrl() + "/account/" + endpoint);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getAccessToken());
        request.addHeader(HttpHeaders.ACCEPT, "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode(),
                    "Unexpected status for /" + endpoint + " with user " + username);
        }
    }

    public static class AccountRolesRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                .attribute("verifiableCredentialsEnabled", "true")
                .groups("test-group")
                .users(
                    UserBuilder.create("manage-account-user")
                        .name("Manage", "Account")
                        .email("manage-account@localhost")
                        .password(PASSWORD)
                        .groups("test-group")
                        .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE),
                    UserBuilder.create("view-applications-user")
                        .name("View", "Applications")
                        .email("view-applications@localhost")
                        .password(PASSWORD)
                        .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.VIEW_APPLICATIONS, AccountRoles.VIEW_PROFILE),
                    UserBuilder.create("view-groups-user")
                        .name("View", "Groups")
                        .email("view-groups@localhost")
                        .password(PASSWORD)
                        .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.VIEW_GROUPS, AccountRoles.VIEW_PROFILE),
                    UserBuilder.create("view-groups-user-with-group")
                        .name("View", "GroupsMember")
                        .email("view-groups-member@localhost")
                        .password(PASSWORD)
                        .groups("test-group")
                        .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.VIEW_GROUPS, AccountRoles.VIEW_PROFILE),
                    UserBuilder.create("no-access-user")
                        .name("No", "Access")
                        .email("no-access@localhost")
                        .password(PASSWORD)
                        .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.VIEW_PROFILE),
                    UserBuilder.create("view-verifiable-credentials-user")
                        .name("View", "VerifiableCredentials")
                        .email("view-verifiable-credentials@localhost")
                        .password(PASSWORD)
                        .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.VIEW_VERIFIABLE_CREDENTIALS, AccountRoles.VIEW_PROFILE),
                    UserBuilder.create("manage-verifiable-credentials-user")
                        .name("Manage", "VerifiableCredentials")
                        .email("manage-verifiable-credentials@localhost")
                        .password(PASSWORD)
                        .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.MANAGE_VERIFIABLE_CREDENTIALS, AccountRoles.VIEW_PROFILE));
        }
    }

    public static class AccountRolesServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI);
        }
    }
}
