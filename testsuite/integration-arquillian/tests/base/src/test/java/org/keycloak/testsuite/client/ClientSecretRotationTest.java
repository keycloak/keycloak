package org.keycloak.testsuite.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientPoliciesPoliciesResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCClientSecretConfigWrapper;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeCondition.Configuration;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutor;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.account.AbstractRestServiceTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
@EnableFeature(value = Feature.CLIENT_SECRET_ROTATION)
public class ClientSecretRotationTest extends AbstractRestServiceTest {

    private static final String OIDC = "openid-connect";
    private static final String DEFAULT_CLIENT_ID = KeycloakModelUtils.generateId();
    private static final String REALM_NAME = "test";
    private static final String CLIENT_NAME = "confidential-client";
    private static final String DEFAULT_SECRET = "GFyDEriVTA9nAu92DenBknb5bjR5jdUM";
    private static final String PROFILE_NAME = "ClientSecretRotationProfile";
    private static final String POLICY_NAME = "ClientSecretRotationPolicy";

    private static final String TEST_USER_NAME = "test-user@localhost";
    private static final String TEST_USER_PASSWORD = "password";

    private static final String ADMIN_USER_NAME = "admin-user";
    private static final String COMMON_USER_NAME = "common-user";
    private static final String COMMON_USER_ID = KeycloakModelUtils.generateId();
    private static final String USER_PASSWORD = "password";

    private static final Logger logger = Logger.getLogger(ClientSecretRotationTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.HOURS.toSeconds(1))
            .intValue();
    private static final int DEFAULT_ROTATED_EXPIRATION_PERIOD = Long.valueOf(
            TimeUnit.MINUTES.toSeconds(10)).intValue();
    private static final int DEFAULT_REMAIN_EXPIRATION_PERIOD = Long.valueOf(
            TimeUnit.MINUTES.toSeconds(30)).intValue();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @After
    public void after() {
        try {
            revertToBuiltinProfiles();
            revertToBuiltinPolicies();
        } catch (ClientPolicyException e) {
            throw new RuntimeException(e);
        }
        resetTimeOffset();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"),
                RealmRepresentation.class);

        List<UserRepresentation> users = realm.getUsers();

        UserRepresentation user = UserBuilder.create().enabled(Boolean.TRUE)
                .username(ADMIN_USER_NAME)
                .password(USER_PASSWORD).addRoles(new String[]{AdminRoles.MANAGE_CLIENTS}).build();
        users.add(user);

        UserRepresentation commonUser = UserBuilder.create().id(COMMON_USER_ID)
                .enabled(Boolean.TRUE)
                .username(COMMON_USER_NAME).email(COMMON_USER_NAME + "@localhost")
                .password(USER_PASSWORD)
                .build();
        users.add(commonUser);

        realm.setUsers(users);
        testRealms.add(realm);
    }

    /**
     * When create a client even without policy secret rotation enabled the client must have a
     * secret creation time
     *
     * @throws Exception
     */
    @Test
    public void whenCreateClientSecretCreationTimeMustExist() throws Exception {

        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);

        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        assertThat(wrapper.getClientSecretCreationTime(), is(notNullValue()));
        String secret = clientResource.getSecret().getValue();
        assertThat(secret, is(notNullValue()));
        assertThat(secret, equalTo(DEFAULT_SECRET));
    }

    /**
     * When regenerate a client secret the creation time attribute must be updated, when the
     * rotation secret policy is not enable
     *
     * @throws Exception
     */
    @Test
    public void regenerateSecret() throws Exception {
        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        String secret = clientResource.getSecret().getValue();
        int secretCreationTime = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation()).getClientSecretCreationTime();
        assertThat(secret, equalTo(DEFAULT_SECRET));
        String newSecret = clientResource.generateNewSecret().getValue();
        assertThat(newSecret, not(equalTo(secret)));
        int updatedSecretCreationTime = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation()).getClientSecretCreationTime();
        assertThat(updatedSecretCreationTime, greaterThanOrEqualTo(secretCreationTime));
    }

    /**
     * When update a client with policy enabled and secret expiration is still valid the rotation
     * must not be performed
     *
     * @throws Exception
     */
    @Test
    public void updateClientWithPolicyAndSecretNotExpired() throws Exception {

        configureDefaultProfileAndPolicy();

        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        String secret = clientResource.getSecret().getValue();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        int secretCreationTime = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                        clientRepresentation)
                .getClientSecretCreationTime();
        clientRepresentation.setDescription("New Description Updated");
        clientResource.update(clientRepresentation);
        assertThat(clientResource.getSecret().getValue(), equalTo(secret));
        assertThat(OIDCClientSecretConfigWrapper.fromClientRepresentation(
                        clientResource.toRepresentation())
                .getClientSecretCreationTime(), equalTo(secretCreationTime));
    }

    /**
     * When regenerate the secret for a client with policy enabled and the secret is not yet
     * expired, the secret must be rotated
     *
     * @throws Exception
     */
    @Test
    public void regenerateSecretOnCurrentSecretNotExpired() throws Exception {
        //apply policy
        configureDefaultProfileAndPolicy();

        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        String firstSecret = clientResource.getSecret().getValue();
        String secondSecret = clientResource.generateNewSecret().getValue();
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());

        assertThat(secondSecret, not(equalTo(firstSecret)));
        assertThat(wrapper.hasRotatedSecret(), is(Boolean.TRUE));
        assertThat(wrapper.getClientRotatedSecret(), equalTo(firstSecret));
    }

    /**
     * When regenerate secret for a client and the expiration date is reached the policy must force
     * a secret rotation
     *
     * @throws Exception
     */
    @Test
    public void regenerateSecretAfterCurrentSecretExpires() throws Exception {

        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        String firstSecret = clientResource.getSecret().getValue();
        String secondSecret = clientResource.generateNewSecret().getValue();
        assertThat(secondSecret, not(equalTo(firstSecret)));
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        assertThat(wrapper.hasRotatedSecret(), is(Boolean.FALSE));

        //apply policy
        configureDefaultProfileAndPolicy();

        //advance 1 hour
        setTimeOffset(3600);

        String newSecret = clientResource.generateNewSecret().getValue();
        assertThat(newSecret, not(equalTo(secondSecret)));
        wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        assertThat(wrapper.hasRotatedSecret(), is(Boolean.TRUE));
        assertThat(wrapper.getClientRotatedSecret(), equalTo(secondSecret));
        int rotatedCreationTime = wrapper.getClientSecretCreationTime();
        assertThat(rotatedCreationTime, is(notNullValue()));
        assertThat(rotatedCreationTime, greaterThan(0));

    }

    /**
     * When update a client with policy enabled and secret expired the secret rotation must be
     * performed
     *
     * @throws Exception
     */
    @Test
    public void updateClientPolicyEnabledSecretExpired() throws Exception {

        configureDefaultProfileAndPolicy();

        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        String firstSecret = clientResource.getSecret().getValue();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientRepresentation.setDescription("New Description Updated");
        clientResource.update(clientRepresentation);

        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        int secretCreationTime = wrapper.getClientSecretCreationTime();

        logger.debug("Current time " + Time.toDate(Time.currentTime()));
        //advance 1 hour
        setTimeOffset(3601);
        logger.debug("Time after offset " + Time.toDate(Time.currentTime()));

        clientRepresentation = clientResource.toRepresentation();
        clientRepresentation.setDescription("Force second Updated");
        clientResource.update(clientRepresentation);

        wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());

        assertThat(clientResource.getSecret().getValue(), not(equalTo(firstSecret)));

        wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        assertThat(wrapper.getClientSecretCreationTime(), not(equalTo(secretCreationTime)));
        assertThat(wrapper.hasRotatedSecret(), is(Boolean.TRUE));
        assertThat(wrapper.getClientRotatedSecret(), equalTo(firstSecret));
    }

    /**
     * When authenticate with client-id and secret and the policy is not enable the login must be
     * successfully (Keeps flow compatibility without secret rotation)
     *
     * @throws ClientPolicyException
     */
    @Test
    public void authenticateWithValidClientNoPolicy() throws ClientPolicyException {
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId);
        successfulLoginAndLogout(clientId, DEFAULT_SECRET);
    }

    /**
     * When the secret rotation policy is active and the client's main secret has not yet expired,
     * the login should be successful.
     *
     * @throws Exception
     */
    @Test
    public void authenticateWithValidClientPolicyEnable() throws Exception {
        configureDefaultProfileAndPolicy();
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId);
        successfulLoginAndLogout(clientId, DEFAULT_SECRET);
    }

    /**
     * When the secret rotation policy is active and the client's main secret has expired, the login
     * should not be successful.
     *
     * @throws Exception
     */
    @Test
    public void authenticateWithInvalidClientPolicyEnable() throws Exception {
        configureDefaultProfileAndPolicy();
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId);

        //The first login will be successful
        oauth.client(clientId, DEFAULT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.OK.getStatusCode()));
        oauth.doLogout(res.getRefreshToken());

        //advance 1 hour
        setTimeOffset(3601);

        oauth.client(clientId, DEFAULT_SECRET);

        // the second login must fail
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        code = oauth.parseLoginResponse().getCode();
        res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.UNAUTHORIZED.getStatusCode()));
    }

    /**
     * When a client goes through a secret rotation, the current secret becomes a rotated secret. A
     * login attempt with the new secret and the rotated secret should be successful as long as none
     * of the client's secrets are expired.
     *
     * @throws Exception
     */
    @Test
    public void authenticateWithValidActualAndRotatedSecret() throws Exception {
        configureDefaultProfileAndPolicy();
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cidConfidential = createClientByAdmin(clientId);

        // force client update. First update will not rotate the secret
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        String firstSecret = clientResource.getSecret().getValue();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientRepresentation.setDescription("New Description Updated");
        clientResource.update(clientRepresentation);

        //advance 1 hour
        setTimeOffset(3601);

        // force client update (rotate the secret according to the policy)
        clientRepresentation = clientResource.toRepresentation();
        clientResource.update(clientRepresentation);

        String updatedSecret = clientResource.getSecret().getValue();
        assertThat(clientResource.getSecret().getValue(), not(equalTo(firstSecret)));

        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());

        oauth.client(clientId, updatedSecret);

        //login with new secret
        AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME,
                TEST_USER_PASSWORD);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.OK.getStatusCode()));
        oauth.doLogout(res.getRefreshToken());

        //login with rotated secret
        oauth.client(clientId, firstSecret);
        loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        code = oauth.parseLoginResponse().getCode();
        res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.OK.getStatusCode()));
        oauth.doLogout(res.getRefreshToken());

    }

    /**
     * When a client goes through a secret rotation, the current secret becomes a rotated secret. A
     * login attempt with the rotated secret should not be successful if secret is expired.
     *
     * @throws Exception
     */
    @Test
    public void authenticateWithInValidRotatedSecret() throws Exception {
        configureDefaultProfileAndPolicy();
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cidConfidential = createClientByAdmin(clientId);

        // force client update (rotate the secret according to the policy)
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        String firstSecret = clientResource.getSecret().getValue();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientRepresentation.setDescription("New Description Updated");
        clientResource.update(clientRepresentation);

        logger.debug(">>> secret creation time " + Time.toDate(Time.currentTime()));

        setTimeOffset(3601);
        clientResource.update(clientResource.toRepresentation());

        logger.debug(">>> secret expiration time after first update " + Time.toDate(
                OIDCClientSecretConfigWrapper.fromClientRepresentation(
                                clientResource.toRepresentation())
                        .getClientSecretExpirationTime()) + " | Time: " + Time.toDate(Time.currentTime()));

        // force rotation
        String updatedSecret = clientResource.getSecret().getValue();
        assertThat(updatedSecret, not(equalTo(firstSecret)));
        clientRepresentation = clientResource.toRepresentation();
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientRepresentation);

        logger.debug(
                ">>> secret expiration configured " + Time.toDate(
                        wrapper.getClientSecretExpirationTime())
                        + " | Time: " + Time.toDate(Time.currentTime()));

        oauth.clientId(clientId);

        setTimeOffset(7201);

        logger.debug("client secret:" + updatedSecret + "\nsecret expiration: " + Time.toDate(
                wrapper.getClientSecretExpirationTime()) + "\nrotated secret: "
                + wrapper.getClientRotatedSecret() + "\nrotated expiration: " + Time.toDate(
                wrapper.getClientRotatedSecretExpirationTime()) + " | Time: " + Time.toDate(
                Time.currentTime()));
        logger.debug(">>> trying login at time " + Time.toDate(Time.currentTime()));

        oauth.client(clientId, firstSecret);

        // try to login with rotated secret (must fail)
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.UNAUTHORIZED.getStatusCode()));
        oauth.doLogout(res.getRefreshToken());

    }

    /**
     * When a client goes through a secret rotation and the configuration for rotated secret is zero
     * then the rotated secret is automatically invalidated, therefore the rotated secret is not
     * valid for a successful login
     *
     * @throws Exception
     */
    @Test
    public void authenticateWithRotatedSecretWithZeroExpirationTime() throws Exception {
        configureCustomProfileAndPolicy(DEFAULT_EXPIRATION_PERIOD, 0, 0);
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cidConfidential = createClientByAdmin(clientId);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        clientResource.update(clientResource.toRepresentation());

        //advance 1 hour
        setTimeOffset(3601);

        // force client update (rotate the secret according to the policy)
        String firstSecret = clientResource.getSecret().getValue();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientRepresentation.setDescription("New Description Updated");

        clientResource.update(clientRepresentation);
        String updatedSecret = clientResource.getSecret().getValue();
        //confirms rotation
        assertThat(updatedSecret, not(equalTo(firstSecret)));

        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        assertThat(wrapper.hasRotatedSecret(), is(Boolean.FALSE));

        oauth.client(clientId, firstSecret);

        // try to login with rotated secret (must fail)
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.UNAUTHORIZED.getStatusCode()));
        oauth.doLogout(res.getRefreshToken());

    }

    /**
     * When create a confidential client with policy enabled the client must have secret expiration
     * time configured
     *
     * @throws Exception
     */
    @Test
    public void createClientWithPolicyEnableSecretExpiredTime() throws Exception {

        configureDefaultProfileAndPolicy();

        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();

        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        int clientSecretExpirationTime = wrapper.getClientSecretExpirationTime();
        assertThat(clientSecretExpirationTime, is(not(0)));

    }

    /**
     * After rotate the secret the endpoint must return the rotated secret
     *
     * @throws Exception
     */
    @Test
    public void getClientRotatedSecret() throws Exception {
        configureDefaultProfileAndPolicy();

        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        String firstSecret = clientResource.getSecret().getValue();
        try {
            clientResource.getClientRotatedSecret();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(NotFoundException.class)));
        }

        String newSecret = clientResource.generateNewSecret().getValue();
        String rotatedSecret = clientResource.getClientRotatedSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));
        assertThat(firstSecret, equalTo(rotatedSecret));
    }

    /**
     * After rotate the secret it must be possible to invalidate the rotated secret
     *
     * @throws Exception
     */
    @Test
    public void invalidateClientRotatedSecret() throws Exception {
        configureDefaultProfileAndPolicy();

        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients()
                .get(cidConfidential);
        String firstSecret = clientResource.getSecret().getValue();
        String newSecret = clientResource.generateNewSecret().getValue();
        String rotatedSecret = clientResource.getClientRotatedSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));
        assertThat(firstSecret, equalTo(rotatedSecret));
        clientResource.invalidateRotatedSecret();
        try {
            clientResource.getClientRotatedSecret();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(NotFoundException.class)));
        }
    }

    /**
     * When try to create an executor for client secret rotation the configuration must be valid.
     * If the rules expressed in services/src/main/java/org/keycloak/services/clientpolicy/executor/ClientSecretRotationExecutor.Configuration is invalid, then the resource must not be created
     *
     * @throws Exception
     */
    @Test
    public void createExecutorConfigurationWithInvalidValues() throws Exception {
        try {
            configureCustomProfileAndPolicy(60, 61, 30);
        } catch (Exception e) {
            assertThat(e, instanceOf(ClientPolicyException.class));
        }
        // no police must have been created due to the above error
        ClientPoliciesPoliciesResource policiesResource = adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource();
        ClientPoliciesRepresentation policies = policiesResource.getPolicies();
        assertThat(policies.getPolicies(), is(empty()));
    }

    /**
     * When there is a client that has a secret rotated and the policy is disabled, Rotation information must be removed after updating a client
     *
     * @throws Exception
     */
    @Test
    public void removingPolicyMustClearRotationInformationFromClientOnUpdate() throws Exception {
        //create and enable the profile
        configureDefaultProfileAndPolicy();
        //create client
        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
        String firstSecret = clientResource.getSecret().getValue();
        String newSecret = clientResource.generateNewSecret().getValue();
        String rotatedSecret = clientResource.getClientRotatedSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));
        assertThat(firstSecret, equalTo(rotatedSecret));

        //disable the profile
        disableProfile();

        //force a update
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientRepresentation.setDescription("New Description Updated");
        clientResource.update(clientRepresentation);

        //client must not have any information about rotation in it
        clientRepresentation = clientResource.toRepresentation();
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(clientRepresentation);

        assertThat(wrapper.hasRotatedSecret(), is(false));
        assertThat(wrapper.getClientSecretExpirationTime(),is(0));
    }

    /**
     * When there is a client that has a secret rotated and the policy is disabled, Rotation information must be removed after request a new secret
     *
     * @throws Exception
     */
    @Test
    public void removingPolicyMustClearRotationInformationFromClientOnRequestNewSecret() throws Exception {
        //create and enable the profile
        configureDefaultProfileAndPolicy();
        //create client
        String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
        String firstSecret = clientResource.getSecret().getValue();
        String newSecret = clientResource.generateNewSecret().getValue();
        String rotatedSecret = clientResource.getClientRotatedSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));
        assertThat(firstSecret, equalTo(rotatedSecret));

        //disable the profile
        disableProfile();

        //Request a new secret
        newSecret = clientResource.generateNewSecret().getValue();

        //client must not have any information about rotation in it
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(clientRepresentation);
        assertThat(clientResource.getSecret().getValue(),equalTo(newSecret));
        assertThat(wrapper.hasRotatedSecret(), is(false));
        assertThat(wrapper.getClientSecretExpirationTime(),is(0));
    }

    /**
     * -------------------- support methods --------------------
     **/

    private void configureCustomProfileAndPolicy(int secretExpiration, int rotatedExpiration,
                                                 int remainingExpiration) throws Exception {
        ClientProfileBuilder profileBuilder = new ClientProfileBuilder();
        ClientSecretRotationExecutor.Configuration profileConfig = getClientProfileConfiguration(
                secretExpiration, rotatedExpiration, remainingExpiration);

        doConfigProfileAndPolicy(profileBuilder, profileConfig);
    }

    private void configureDefaultProfileAndPolicy() throws Exception {
        // register profiles
        ClientProfileBuilder profileBuilder = new ClientProfileBuilder();
        ClientSecretRotationExecutor.Configuration profileConfig = getClientProfileConfiguration(
                DEFAULT_EXPIRATION_PERIOD, DEFAULT_ROTATED_EXPIRATION_PERIOD,
                DEFAULT_REMAIN_EXPIRATION_PERIOD);

        doConfigProfileAndPolicy(profileBuilder, profileConfig);
    }

    private void disableProfile() throws Exception {
        Configuration config = new Configuration();
        config.setType(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL));
        String json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME,
                                "Policy for Client Secret Rotation",
                                Boolean.FALSE).addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, config)
                        .addProfile(PROFILE_NAME).toRepresentation()).toString();
        updatePolicies(json);
    }

    private void doConfigProfileAndPolicy(ClientProfileBuilder profileBuilder,
                                          ClientSecretRotationExecutor.Configuration profileConfig) throws Exception {
        String json = (new ClientProfilesBuilder()).addProfile(
                profileBuilder.createProfile(PROFILE_NAME, "Enable Client Secret Rotation")
                        .addExecutor(ClientSecretRotationExecutorFactory.PROVIDER_ID, profileConfig)
                        .toRepresentation()).toString();
        updateProfiles(json);

        // register policies
        Configuration config = new Configuration();
        config.setType(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL));
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME,
                                "Policy for Client Secret Rotation",
                                Boolean.TRUE).addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, config)
                        .addProfile(PROFILE_NAME).toRepresentation()).toString();
        updatePolicies(json);
    }

    @NotNull
    private ClientSecretRotationExecutor.Configuration getClientProfileConfiguration(
            int expirationPeriod, int rotatedExpirationPeriod, int remainExpirationPeriod) {
        ClientSecretRotationExecutor.Configuration profileConfig = new ClientSecretRotationExecutor.Configuration();
        profileConfig.setExpirationPeriod(expirationPeriod);
        profileConfig.setRotatedExpirationPeriod(rotatedExpirationPeriod);
        profileConfig.setRemainExpirationPeriod(remainExpirationPeriod);
        return profileConfig;
    }

    protected String createClientByAdmin(String clientId) throws ClientPolicyException {
        ClientRepresentation clientRep = getClientRepresentation(clientId);

        Response resp = adminClient.realm(REALM_NAME).clients().create(clientRep);
        if (resp.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
            String respBody = resp.readEntity(String.class);
            Map<String, String> responseJson = null;
            try {
                responseJson = JsonSerialization.readValue(respBody, Map.class);
            } catch (IOException e) {
                fail();
            }
            throw new ClientPolicyException(responseJson.get(OAuth2Constants.ERROR),
                    responseJson.get(OAuth2Constants.ERROR_DESCRIPTION));
        }
        resp.close();
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        // registered components will be removed automatically when a test method finishes regardless of its success or failure.
        String cId = ApiUtil.getCreatedId(resp);
        testContext.getOrCreateCleanup(REALM_NAME).addClientUuid(cId);
        return cId;
    }

    @NotNull
    private ClientRepresentation getClientRepresentation(String clientId) {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientId);
        clientRep.setName(CLIENT_NAME);
        clientRep.setSecret(DEFAULT_SECRET);
        clientRep.setAttributes(new HashMap<>());
        clientRep.getAttributes()
                .put(ClientSecretConstants.CLIENT_SECRET_CREATION_TIME,
                        String.valueOf(Time.currentTime()));
        clientRep.setProtocol(OIDC);
        clientRep.setBearerOnly(Boolean.FALSE);
        clientRep.setPublicClient(Boolean.FALSE);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        clientRep.setStandardFlowEnabled(Boolean.TRUE);
        clientRep.setImplicitFlowEnabled(Boolean.TRUE);
        clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);

        clientRep.setRedirectUris(Collections.singletonList(
                ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));
        return clientRep;
    }

    protected String generateSuffixedName(String name) {
        return name + "-" + UUID.randomUUID().toString().subSequence(0, 7);
    }

    protected void updateProfiles(String json) throws ClientPolicyException {
        try {
            ClientProfilesRepresentation clientProfiles = JsonSerialization.readValue(json,
                    ClientProfilesRepresentation.class);
            adminClient.realm(REALM_NAME).clientPoliciesProfilesResource()
                    .updateProfiles(clientProfiles);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update profiles failed",
                    e.getResponse().getStatusInfo().toString());
        } catch (Exception e) {
            throw new ClientPolicyException("update profiles failed", e.getMessage());
        }
    }

    protected void updateProfiles(ClientProfilesRepresentation reps) throws ClientPolicyException {
        updateProfiles(convertToProfilesJson(reps));
    }

    protected void revertToBuiltinProfiles() throws ClientPolicyException {
        updateProfiles("{}");
    }

    protected String convertToProfilesJson(ClientProfilesRepresentation reps) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(reps);
        } catch (JsonProcessingException e) {
            fail();
        }
        return json;
    }

    protected String convertToProfileJson(ClientProfileRepresentation rep) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(rep);
        } catch (JsonProcessingException e) {
            fail();
        }
        return json;
    }

    protected ClientProfileRepresentation convertToProfile(String json) {
        ClientProfileRepresentation rep = null;
        try {
            rep = JsonSerialization.readValue(json, ClientProfileRepresentation.class);
        } catch (IOException e) {
            fail();
        }
        return rep;
    }

    protected void revertToBuiltinPolicies() throws ClientPolicyException {
        updatePolicies("{}");
    }

    protected void updatePolicies(String json) throws ClientPolicyException {
        try {
            ClientPoliciesRepresentation clientPolicies = json == null ? null
                    : JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
            adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource()
                    .updatePolicies(clientPolicies);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update policies failed",
                    e.getResponse().getStatusInfo().toString());
        } catch (IOException e) {
            throw new ClientPolicyException("update policies failed", e.getMessage());
        }
    }

    private void successfulLoginAndLogout(String clientId, String clientSecret) {
        AccessTokenResponse res = successfulLogin(clientId, clientSecret);
        oauth.doLogout(res.getRefreshToken());
        events.expectLogout(res.getSessionState()).client(clientId).clearDetails().assertEvent();
    }

    private AccessTokenResponse successfulLogin(String clientId, String clientSecret) {
        oauth.client(clientId, clientSecret);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        return res;
    }
}
