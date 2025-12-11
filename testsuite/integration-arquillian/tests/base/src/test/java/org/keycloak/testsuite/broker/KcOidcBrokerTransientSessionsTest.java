package org.keycloak.testsuite.broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.ExternalKeycloakRoleToRoleMapper;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.provider.HardcodedRoleMapper;
import org.keycloak.common.Profile;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.TokenUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.broker.KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_CLIENT_ID;
import static org.keycloak.testsuite.broker.KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Final class as it's not intended to be overriden. Feel free to remove "final" if you really know what you are doing.
 */
@EnableFeature(value = Profile.Feature.TRANSIENT_USERS, skipRestart = true)
public final class KcOidcBrokerTransientSessionsTest extends AbstractAdvancedBrokerTest {
    private final static String USER_ATTRIBUTE_NAME = "user-attribute";
    private final static String USER_ATTRIBUTE_VALUE = "attribute-value";
    private final static String CLAIM_FILTER_REGEXP = ".*-value";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return BROKER_CONFIG_INSTANCE;
    }

    @Page
    UpdateAccountInformationPage updateAccountInformationPage;

    @Before
    public void setUpTotp() {
        totp = new TimeBasedOTP();
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers(IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation attrMapper1 = new IdentityProviderMapperRepresentation();
        attrMapper1.setName("manager-role-mapper");
        attrMapper1.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper1.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("external.role", ROLE_MANAGER)
                .put("role", ROLE_MANAGER)
                .build());

        IdentityProviderMapperRepresentation attrMapper2 = new IdentityProviderMapperRepresentation();
        attrMapper2.setName("user-role-mapper");
        attrMapper2.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper2.setConfig(ImmutableMap.<String,String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("external.role", ROLE_USER)
                .put("role", ROLE_USER)
                .build());

        return Lists.newArrayList(attrMapper1, attrMapper2);
    }

    @Override
    protected void createAdditionalMapperWithCustomSyncMode(IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation friendlyManagerMapper = new IdentityProviderMapperRepresentation();
        friendlyManagerMapper.setName("friendly-manager-role-mapper");
        friendlyManagerMapper.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        friendlyManagerMapper.setConfig(ImmutableMap.<String,String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("external.role", ROLE_FRIENDLY_MANAGER)
                .put("role", ROLE_FRIENDLY_MANAGER)
                .build());
        friendlyManagerMapper.setIdentityProviderAlias(bc.getIDPAlias());
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        IdentityProviderResource idpResource = realm.identityProviders().get(bc.getIDPAlias());
        idpResource.addMapper(friendlyManagerMapper).close();
    }

    @Test
    public void mapperDoesNothingForLegacyMode() {
        createRolesForRealm(bc.providerRealmName());
        createRolesForRealm(bc.consumerRealmName());

        createRoleMappersForConsumerRealm(IdentityProviderMapperSyncMode.LEGACY);

        RoleRepresentation managerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_MANAGER).toRepresentation();
        RoleRepresentation userRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();

        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(managerRole));

        oauth.client("broker-app", CONSUMER_BROKER_APP_SECRET);
        loginPage.open(bc.consumerRealmName());
        logInAsUserInIDPForFirstTime();

        String consumerClientBrokerAppId = adminClient.realm(bc.consumerRealmName()).clients().findByClientId("broker-app").get(0).getId();
        String transientUserId = adminClient.realm(bc.consumerRealmName()).clients().get(consumerClientBrokerAppId).getUserSessions(0, 10).get(0).getUserId();
        assertThat(adminClient.realm(bc.consumerRealmName()).users().list(), empty());

        UserResource consumerUserResource = adminClient.realm(bc.consumerRealmName()).users().get(transientUserId);
        Set<String> currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        assertThat(currentRoles, hasItems(ROLE_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER)));

        logoutFromConsumerRealm();
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        userResource.roles().realmLevel().add(Collections.singletonList(userRole));

        oauth.client("broker-app", CONSUMER_BROKER_APP_SECRET);
        loginPage.open(bc.consumerRealmName());

        if (! isUsingTransientSessions()) {
            logInAsUserInIDP();

            currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toSet());
            assertThat(currentRoles, hasItems(ROLE_MANAGER));
            assertThat(currentRoles, not(hasItems(ROLE_USER)));

            logoutFromConsumerRealm();
            logoutFromRealm(getProviderRoot(), bc.providerRealmName());
        }
    }

    @Test
    public void loginFetchingUserFromUserEndpoint() {
        loginFetchingUserFromUserEndpoint(false);
    }

    private void loginFetchingUserFromUserEndpoint(boolean loginIsDenied) {
        RealmResource realm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = realm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);

        try {
            IdentityProviderResource identityProviderResource = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
            IdentityProviderRepresentation idp = identityProviderResource.toRepresentation();

            idp.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/certs");
            identityProviderResource.update(idp);

            brokerApp.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS256);
            brokerApp.getAttributes().put("validateSignature", Boolean.TRUE.toString());
            clients.get(brokerApp.getId()).update(brokerApp);

            oauth.client("broker-app", CONSUMER_BROKER_APP_SECRET);
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, loginIsDenied? "We are sorry..." : "update account information", false);
            if (loginIsDenied) {
                return;
            }

            updateAccountInformationPage.assertCurrent();
            Assert.assertTrue("We must be on correct realm right now",
                    driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

            log.debug("Updating info on updateAccount page");
            updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

            List<UserRepresentation> consumerUsers = getConsumerUserRepresentations().collect(Collectors.toList());

            int userCount = consumerUsers.size();
            Assert.assertTrue("There must be at least one user", userCount > 0);

            boolean isUserFound = false;
            for (UserRepresentation user : consumerUsers) {
                if (user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail())) {
                    isUserFound = true;
                    break;
                }
            }

            Assert.assertTrue("There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName(),
                    isUserFound);
        } finally {
            brokerApp.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, null);
            brokerApp.getAttributes().put("validateSignature", Boolean.FALSE.toString());
            clients.get(brokerApp.getId()).update(brokerApp);
        }
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.OIDCBrokerUserPropertyTest
     */
    @Test
    public void loginFetchingUserFromUserEndpointWithClaimMapper() {
        RealmResource realm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = realm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);
        IdentityProviderResource identityProviderResource = getIdentityProviderResource();

        clients.get(brokerApp.getId()).getProtocolMappers().createMapper(createHardcodedClaim("hard-coded", "hard-coded", "hard-coded", "String", true, true, true)).close();

        IdentityProviderMapperRepresentation hardCodedSessionNoteMapper = new IdentityProviderMapperRepresentation();

        hardCodedSessionNoteMapper.setName("hard-coded");
        hardCodedSessionNoteMapper.setIdentityProviderAlias(bc.getIDPAlias());
        hardCodedSessionNoteMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        hardCodedSessionNoteMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString())
                .put(UserAttributeMapper.USER_ATTRIBUTE, "hard-coded")
                .put(UserAttributeMapper.CLAIM, "hard-coded")
                .build());

        identityProviderResource.addMapper(hardCodedSessionNoteMapper).close();

        oauth.client("broker-app", CONSUMER_BROKER_APP_SECRET);
        loginPage.open(bc.consumerRealmName());

        loginFetchingUserFromUserEndpoint();

        UserRepresentation user = getFederatedIdentity();

        Assert.assertEquals(1, user.getAttributes().size());
        Assert.assertEquals("hard-coded", user.getAttributes().get("hard-coded").get(0));
    }

    @Test
    public void testInvalidIssuedFor() {
        loginUser();
        logoutFromConsumerRealm();
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        oauth.client("broker-app", CONSUMER_BROKER_APP_SECRET);
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);

        RealmResource realm = adminClient.realm(bc.providerRealmName());
        ClientRepresentation rep = realm.clients().findByClientId(BrokerTestConstants.CLIENT_ID).get(0);
        ClientResource clientResource = realm.clients().get(rep.getId());
        ProtocolMapperRepresentation hardCodedAzp = createHardcodedClaim("hard", "azp", "invalid-azp", ProviderConfigProperty.STRING_TYPE, true, true, true);
        clientResource.getProtocolMappers().createMapper(hardCodedAzp);

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        errorPage.assertCurrent();
    }

    @Test
    public void testInvalidAudience() {
        loginUser();
        logoutFromConsumerRealm();
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        oauth.client("broker-app", CONSUMER_BROKER_APP_SECRET);
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);

        RealmResource realm = adminClient.realm(bc.providerRealmName());
        ClientRepresentation rep = realm.clients().findByClientId(BrokerTestConstants.CLIENT_ID).get(0);
        ClientResource clientResource = realm.clients().get(rep.getId());
        ProtocolMapperRepresentation hardCodedAzp = createHardcodedClaim("hard", "aud", "invalid-aud", ProviderConfigProperty.LIST_TYPE, true, true, true);
        clientResource.getProtocolMappers().createMapper(hardCodedAzp);

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        errorPage.assertCurrent();
    }

    @Test
    public void testIdPNotFound() {
        final String notExistingIdP = "not-exists";
        final String realmName = realmsResouce().realm(bc.providerRealmName()).toRepresentation().getRealm();
        assertThat(realmName, notNullValue());
        final String LINK = OAuthClient.AUTH_SERVER_ROOT + "/realms/" + realmName + "/broker/" + notExistingIdP + "/endpoint";

        driver.navigate().to(LINK);

        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is("Page not found"));

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            SimpleHttpResponse simple = SimpleHttpDefault.doGet(LINK, client).asResponse();
            assertThat(simple, notNullValue());
            assertThat(simple.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));

            OAuth2ErrorRepresentation error = simple.asJson(OAuth2ErrorRepresentation.class);
            assertThat(error, notNullValue());
            assertThat(error.getError(), is("Identity Provider [" + notExistingIdP + "] not found."));
        } catch (IOException ex) {
            Assert.fail("Cannot create HTTP client. Details: " + ex.getMessage());
        }
    }

    @Test
    public void loginWithClaimFilter() {
        IdentityProviderResource identityProviderResource = getIdentityProviderResource();

        IdentityProviderRepresentation identityProvider = identityProviderResource.toRepresentation();
        updateIdPClaimFilter(identityProvider, identityProviderResource, true, USER_ATTRIBUTE_NAME, USER_ATTRIBUTE_VALUE);

        WaitUtils.waitForPageToLoad();

        loginFetchingUserFromUserEndpoint();

        UserRepresentation user = getFederatedIdentity();

        Assert.assertNotNull(user);
    }

    @Test
    public void loginWithClaimRegexpFilter() {
        IdentityProviderResource identityProviderResource = getIdentityProviderResource();

        IdentityProviderRepresentation identityProvider = identityProviderResource.toRepresentation();
        updateIdPClaimFilter(identityProvider, identityProviderResource, true, USER_ATTRIBUTE_NAME, CLAIM_FILTER_REGEXP);

        WaitUtils.waitForPageToLoad();

        loginFetchingUserFromUserEndpoint();

        UserRepresentation user = getFederatedIdentity();

        Assert.assertNotNull(user);
    }

    @Test
    public void denyLoginWithClaimFilter() {
        IdentityProviderResource identityProviderResource = getIdentityProviderResource();

        IdentityProviderRepresentation identityProvider = identityProviderResource.toRepresentation();
        updateIdPClaimFilter(identityProvider, identityProviderResource, true, "hardcoded-missing-claim", "hardcoded-missing-claim-value");
        WaitUtils.waitForPageToLoad();

        loginFetchingUserFromUserEndpoint(true);
        Assert.assertEquals("The ID token issued by the identity provider does not match the configured essential claim. Please contact your administrator.",
            loginPage.getInstruction());


        List<UserRepresentation> users = realmsResouce().realm(bc.consumerRealmName()).users().search(bc.getUserLogin());
        assertThat(users, Matchers.empty());
    }

    protected void postInitializeUser(UserRepresentation user) {
        user.setAttributes(ImmutableMap.<String, List<String>> builder()
                .put(USER_ATTRIBUTE_NAME, ImmutableList.<String> builder().add(USER_ATTRIBUTE_VALUE).build())
                .build());
    }


    private void updateIdPClaimFilter(IdentityProviderRepresentation idProvider, IdentityProviderResource idProviderResource, boolean filteredByClaim, String claimFilterName, String claimFilterValue) {
        assertThat(idProvider, Matchers.notNullValue());
        assertThat(idProviderResource, Matchers.notNullValue());
        assertThat(claimFilterName, Matchers.notNullValue());
        assertThat(claimFilterValue, Matchers.notNullValue());

        if (idProvider.getConfig().getOrDefault(IdentityProviderModel.FILTERED_BY_CLAIMS, "false").equals(Boolean.toString(filteredByClaim)) &&
            idProvider.getConfig().getOrDefault(IdentityProviderModel.CLAIM_FILTER_NAME, "").equals(claimFilterName) &&
            idProvider.getConfig().getOrDefault(IdentityProviderModel.CLAIM_FILTER_VALUE, "").equals(claimFilterValue)
        ) {
            return;
        }

        idProvider.getConfig().put(IdentityProviderModel.FILTERED_BY_CLAIMS, Boolean.toString(filteredByClaim));
        idProvider.getConfig().put(IdentityProviderModel.CLAIM_FILTER_NAME, claimFilterName);
        idProvider.getConfig().put(IdentityProviderModel.CLAIM_FILTER_VALUE, claimFilterValue);
        idProviderResource.update(idProvider);

        idProvider = idProviderResource.toRepresentation();
        assertThat("Cannot get Identity Provider", idProvider, Matchers.notNullValue());
        assertThat("Filtered by claim didn't change", idProvider.getConfig().get(IdentityProviderModel.FILTERED_BY_CLAIMS), Matchers.equalTo(Boolean.toString(filteredByClaim)));
        assertThat("Claim name didn't change", idProvider.getConfig().get(IdentityProviderModel.CLAIM_FILTER_NAME), Matchers.equalTo(claimFilterName));
        assertThat("Claim value didn't change", idProvider.getConfig().get(IdentityProviderModel.CLAIM_FILTER_VALUE), Matchers.equalTo(claimFilterValue));
    }

    private UserRepresentation getFederatedIdentity() {
        return getConsumerUserRepresentation(bc.getUserLogin());
    }

    private IdentityProviderResource getIdentityProviderResource() {
        return realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
    }

    @Test
    public void testSingleSignOn() {
        loginWithBrokerUsingOAuthClient(CustomKcOidcBrokerConfiguration.CONSUMER_ADDITIONAL_BROKER_APP_CLIENT_ID);

        oauth.clientId(CONSUMER_BROKER_APP_CLIENT_ID);
        oauth.openLoginForm();

        Assert.assertTrue("Should be logged in", driver.getTitle().endsWith("AUTH_RESPONSE"));
    }

    // Based on ConsentsTest.testConsents, modified to use consumer realm instead
    @Test
    public void testConsents() throws Exception {
        try (var c = ClientAttributeUpdater.forClient(adminClient, bc.consumerRealmName(), CONSUMER_BROKER_APP_CLIENT_ID).setConsentRequired(true).update()) {
            oauth.clientId(CONSUMER_BROKER_APP_CLIENT_ID);
            oauth.realm(bc.consumerRealmName());
            doLoginSocial(oauth, bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword());

            WaitUtils.waitForPageToLoad();
            consentPage.assertCurrent();
            consentPage.confirm();

            EventRepresentation loginEvent;
            do {
                loginEvent = events.poll();
            } while (loginEvent != null && (!Objects.equals(EventType.LOGIN.name(), loginEvent.getType()) ||
                    !Objects.equals(loginEvent.getClientId(), CONSUMER_BROKER_APP_CLIENT_ID)));

            assertThat(loginEvent, notNullValue());
            assertThat(loginEvent.getUserId(), Matchers.containsString(LightweightUserAdapter.ID_PREFIX));

            final String lwUserId = loginEvent.getUserId();

            final UserResource userResource = adminClient.realm(bc.consumerRealmName()).users().get(lwUserId);
            List<Map<String, Object>> consents = userResource.getConsents();
            assertThat("There should be one consent", consents, hasSize(1));

            Map<String, Object> consent = consents.get(0);
            Assert.assertEquals("Consent should be given to " + CONSUMER_BROKER_APP_CLIENT_ID, CONSUMER_BROKER_APP_CLIENT_ID, consent.get("clientId"));

            // list sessions. Single client should be in user session
            List<UserSessionRepresentation> sessions = userResource.getUserSessions();
            assertThat("There should be one active session", sessions, hasSize(1));
            assertThat("There should be one client in user session", sessions.get(0).getClients(), aMapWithSize(1));

            // Try SSO relogging into the app before revoking consent.
            oauth.clientId(CONSUMER_BROKER_APP_CLIENT_ID);
            oauth.openLoginForm();
            assertThat("Should be logged in", driver.getTitle(), containsString("AUTH_RESPONSE"));

            // revoke consent
            userResource.revokeConsent(CONSUMER_BROKER_APP_CLIENT_ID);

            // list consents
            consents = userResource.getConsents();
            assertThat("There should be no consents", consents, empty());

            // list sessions
            sessions = userResource.getUserSessions();
            assertThat("There should be one active session", sessions, hasSize(1));
            assertThat("There should be no client in user session", sessions.get(0).getClients(), aMapWithSize(0));

            // Try relogging into the app after consent was revoked.
            oauth.clientId(CONSUMER_BROKER_APP_CLIENT_ID);
            oauth.openLoginForm();

            WaitUtils.waitForPageToLoad();
            consentPage.assertCurrent();
            consentPage.confirm();

            WaitUtils.waitForPageToLoad();
            assertThat("Should be logged in", driver.getTitle(), containsString("AUTH_RESPONSE"));
        }
    }

    @Test
    public void testUserInfoEndpoint() throws Exception {
        EventRepresentation loginEvent = loginWithBrokerUsingOAuthClient(CONSUMER_BROKER_APP_CLIENT_ID);
        String lwUserId = loginEvent.getUserId();

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.client(CONSUMER_BROKER_APP_CLIENT_ID, CONSUMER_BROKER_APP_SECRET).doAccessTokenRequest(code);

        // Check that userInfo can be invoked
        var userInfoResponse = oauth.doUserInfoRequest(tokenResponse.getAccessToken());
        assertThat(userInfoResponse.getUserInfo().getSub(), is(lwUserId));
        assertThat(userInfoResponse.getUserInfo().getPreferredUsername(), is(bc.getUserLogin()));
        assertThat(userInfoResponse.getUserInfo().getEmail(), is(bc.getUserEmail()));

        // Check that tokenIntrospection can be invoked
        JsonNode jsonNode = oauth.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken()).asJsonNode();
        org.junit.Assert.assertEquals(true, jsonNode.get("active").asBoolean());
        org.junit.Assert.assertEquals(bc.getUserEmail(), jsonNode.get("email").asText());
    }

    private EventRepresentation loginWithBrokerUsingOAuthClient(String consumerClientId) {
        oauth.client(consumerClientId, CONSUMER_BROKER_APP_SECRET);
        oauth.realm(bc.consumerRealmName());

        doLoginSocial(oauth, bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword());

        EventRepresentation loginEvent;
        do {
            loginEvent = events.poll();
        } while (loginEvent != null && (!Objects.equals(EventType.LOGIN.name(), loginEvent.getType()) ||
                !Objects.equals(loginEvent.getClientId(), consumerClientId)));

        assertThat(loginEvent, notNullValue());
        assertThat(loginEvent.getUserId(), Matchers.containsString(LightweightUserAdapter.ID_PREFIX));

        return loginEvent;
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Test   // Based on OfflineTokenTest.offlineTokenBrowserFlow()
    public void offlineTokenBrowserFlow() throws Exception {
        final RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation consumerRealmRep = consumerRealm.toRepresentation();

        // Create mapper which assigns offline_access role to users from "provider" IdP
        try (var c = Creator.create(consumerRealm, bc.getIDPAlias(), createHardcodedOfflineRoleMapper())) {
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            EventRepresentation loginEvent = loginWithBrokerUsingOAuthClient(CONSUMER_BROKER_APP_CLIENT_ID);
            String lwUserId = loginEvent.getUserId();

            final String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

            AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
            String offlineTokenString = tokenResponse.getRefreshToken();
            RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

            events.expectCodeToToken(codeId, sessionId)
                    .realm(consumerRealmRep)
                    .client(CONSUMER_BROKER_APP_CLIENT_ID)
                    .user(lwUserId)
                    .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                    .assertEvent();

            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
            assertNull(offlineToken.getExp());

            assertTrue(tokenResponse.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));

            String newRefreshTokenString = testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, sessionId, consumerRealmRep, lwUserId);

            // Change offset to very big value to ensure offline session expires
            setTimeOffset(3000000);

            AccessTokenResponse response = oauth.doRefreshTokenRequest(newRefreshTokenString);
            RefreshToken newRefreshToken = oauth.parseRefreshToken(newRefreshTokenString);
            org.junit.Assert.assertEquals(400, response.getStatusCode());
            assertEquals("invalid_grant", response.getError());

            events.expectRefresh(offlineToken.getId(), newRefreshToken.getSessionState())
                    .realm(consumerRealmRep)
                    .client(CONSUMER_BROKER_APP_CLIENT_ID)
                    .user((String) null)
                    .error(Errors.INVALID_TOKEN)
                    .clearDetails()
                    .assertEvent();
        } finally {
            setTimeOffset(0);
        }
    }

    private String testRefreshWithOfflineToken(AccessToken oldToken, RefreshToken offlineToken, String offlineTokenString,
                                               final String sessionId, RealmRepresentation consumerRealmRep, String userId) {
        // Change offset to big value to ensure userSession expired
        setTimeOffset(99999);
        assertFalse(oldToken.isActive());
        assertTrue(offlineToken.isActive());

        // Assert userSession expired
        testingClient.testing().removeExpired(bc.consumerRealmName());
        try {
            testingClient.testing().removeUserSession(bc.consumerRealmName(), sessionId);
        } catch (NotFoundException nfe) {
            // Ignore
        }

        AccessTokenResponse response = oauth.doRefreshTokenRequest(offlineTokenString);
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        org.junit.Assert.assertEquals(200, response.getStatusCode());

        // Assert new refreshToken in the response
        String newRefreshToken = response.getRefreshToken();
        org.junit.Assert.assertNotNull(newRefreshToken);
        org.junit.Assert.assertNotEquals(oldToken.getId(), refreshedToken.getId());

        // Assert scope parameter contains "offline_access"
        assertTrue(response.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));

        org.junit.Assert.assertEquals(userId, refreshedToken.getSubject());

        assertTrue(refreshedToken.getRealmAccess().isUserInRole(Constants.OFFLINE_ACCESS_ROLE));

        EventRepresentation refreshEvent = events.expectRefresh(offlineToken.getId(), sessionId)
                .realm(consumerRealmRep)
                .client(CONSUMER_BROKER_APP_CLIENT_ID)
                .user(userId)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();
        org.junit.Assert.assertNotEquals(oldToken.getId(), refreshEvent.getDetails().get(Details.TOKEN_ID));

        setTimeOffset(0);
        return newRefreshToken;
    }

    private IdentityProviderMapperRepresentation createHardcodedOfflineRoleMapper() {
        var res = new IdentityProviderMapperRepresentation();
        res.setName("hardcoded-role-mapper");
        res.setIdentityProviderMapper(HardcodedRoleMapper.PROVIDER_ID);
        res.setConfig(ImmutableMap.<String, String> builder()
                .put(ConfigConstants.ROLE, OAuth2Constants.OFFLINE_ACCESS)
                .build());

        return res;
    }


    private static final CustomKcOidcBrokerConfiguration BROKER_CONFIG_INSTANCE = new CustomKcOidcBrokerConfiguration();

    static class CustomKcOidcBrokerConfiguration extends KcOidcBrokerConfiguration {

        public static final String CONSUMER_ADDITIONAL_BROKER_APP_CLIENT_ID = "additional-broker-app";
        public static final String CONSUMER_ADDITIONAL_BROKER_APP_SECRET = "broker-app-secret";

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clients = super.createProviderClients();

            ClientRepresentation client = clients.get(0);
            ProtocolMapperRepresentation userAttrMapper = new ProtocolMapperRepresentation();
            userAttrMapper.setName(USER_ATTRIBUTE_NAME);
            userAttrMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            userAttrMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);

            Map<String, String> userAttrMapperConfig = userAttrMapper.getConfig();
            userAttrMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, USER_ATTRIBUTE_NAME);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, USER_ATTRIBUTE_NAME);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
            userAttrMapperConfig.put(ProtocolMapperUtils.MULTIVALUED, "false");
            userAttrMapperConfig.put(ProtocolMapperUtils.AGGREGATE_ATTRS, "false");
            List<ProtocolMapperRepresentation> mappers = new ArrayList<>(client.getProtocolMappers());
            mappers.add(userAttrMapper);
            client.setProtocolMappers(mappers);

            return clients;
        }

        @Override
        public List<ClientRepresentation> createConsumerClients() {
            List<ClientRepresentation> res = new LinkedList<>(super.createConsumerClients());

            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(CONSUMER_ADDITIONAL_BROKER_APP_CLIENT_ID);
            client.setName("additional-broker-app");
            client.setSecret(CONSUMER_ADDITIONAL_BROKER_APP_SECRET);
            client.setEnabled(true);
            client.setDirectAccessGrantsEnabled(true);

            client.setRedirectUris(Collections.singletonList(getConsumerRoot() +
                    "/auth/*"));

            client.setBaseUrl(getConsumerRoot() +
                    "/auth/realms/" + REALM_CONS_NAME + "/app2");

            OIDCAdvancedConfigWrapper.fromClientRepresentation(client).setPostLogoutRedirectUris(Collections.singletonList("+"));
            OIDCAdvancedConfigWrapper.fromClientRepresentation(client).setUseRefreshTokenForClientCredentialsGrant(true);

            res.add(client);
            return res;
        }

        @Override
        protected void applyDefaultConfiguration(Map<String, String> config, IdentityProviderSyncMode syncMode) {
            super.applyDefaultConfiguration(config, syncMode);
            config.put(IdentityProviderModel.DO_NOT_STORE_USERS, "true");
        }

    }
}
