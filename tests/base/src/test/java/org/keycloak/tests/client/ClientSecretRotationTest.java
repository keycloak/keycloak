package org.keycloak.tests.client;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.ClientPoliciesProfilesResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCClientSecretConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutor;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
@KeycloakIntegrationTest(config = ClientSecretRotationTest.ClientSecretRotationServerConfig.class)
public class ClientSecretRotationTest {

    private static final String CLIENT_NAME = "confidential-client";
    private static final String DEFAULT_SECRET = "GFyDEriVTA9nAu92DenBknb5bjR5jdUM";
    private static final String PROFILE_NAME = "ClientSecretRotationProfile";
    private static final String POLICY_NAME = "ClientSecretRotationPolicy";

    private static final String TEST_USER_NAME = "test-user@localhost";
    private static final String TEST_USER_PASSWORD = "password";

    private static final Logger logger = Logger.getLogger(ClientSecretRotationTest.class);

    private static final int DEFAULT_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.HOURS.toSeconds(1)).intValue();
    private static final int DEFAULT_ROTATED_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.MINUTES.toSeconds(10)).intValue();
    private static final int DEFAULT_REMAIN_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.MINUTES.toSeconds(30)).intValue();
    @InjectRealm(config = ClientSecretRotationRealmConfig.class)
    protected ManagedRealm realm;

    @InjectOAuthClient(config = OAuthClientConfig.class, lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectEvents
    Events events;

    @BeforeAll
    public static void beforeAll() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver(Profile.Feature.CLIENT_SECRET_ROTATION.getVersionedKey(), ""));
    }

    /**
     * When create a client even without policy secret rotation enabled the client must have a
     * secret creation time
     */
    @Test
    public void whenCreateClientSecretCreationTimeMustExist() {
        ClientResource clientResource = oauth.clientResource();
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
     */
    @Test
    public void regenerateSecret() {
        ClientResource clientResource = oauth.clientResource();
        String secret = clientResource.getSecret().getValue();
        long secretCreationTime = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation()).getClientSecretCreationTime();
        assertThat(secret, equalTo(DEFAULT_SECRET));
        String newSecret = clientResource.generateNewSecret().getValue();
        assertThat(newSecret, not(equalTo(secret)));
        long updatedSecretCreationTime = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation()).getClientSecretCreationTime();
        assertThat(updatedSecretCreationTime, greaterThanOrEqualTo(secretCreationTime));
    }

    /**
     * When update a client with policy enabled and secret expiration is still valid the rotation
     * must not be performed
     */
    @Test
    public void updateClientWithPolicyAndSecretNotExpired() {

        configureDefaultProfileAndPolicy();

        ClientResource clientResource = oauth.clientResource();
        String secret = clientResource.getSecret().getValue();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        long secretCreationTime = OIDCClientSecretConfigWrapper.fromClientRepresentation(
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
     */
    @Test
    public void regenerateSecretOnCurrentSecretNotExpired() {
        //apply policy
        configureDefaultProfileAndPolicy();

        ClientResource clientResource = oauth.clientResource();
        String firstSecret = clientResource.getSecret().getValue();
        String secondSecret = clientResource.generateNewSecret().getValue();
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());

        assertThat(secondSecret, not(equalTo(firstSecret)));
        assertThat(wrapper.hasRotatedSecret(), is(Boolean.TRUE));
        assertThat(wrapper.getClientRotatedSecret(null), equalTo(firstSecret));
    }

    /**
     * When regenerate secret for a client and the expiration date is reached the policy must force
     * a secret rotation
     */
    @Test
    public void regenerateSecretAfterCurrentSecretExpires() {

        ClientResource clientResource = oauth.clientResource();
        String firstSecret = clientResource.getSecret().getValue();
        String secondSecret = clientResource.generateNewSecret().getValue();
        assertThat(secondSecret, not(equalTo(firstSecret)));
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        assertThat(wrapper.hasRotatedSecret(), is(Boolean.FALSE));

        //apply policy
        configureDefaultProfileAndPolicy();

        //advance 1 hour
        timeOffSet.set(3600);

        String newSecret = clientResource.generateNewSecret().getValue();
        assertThat(newSecret, not(equalTo(secondSecret)));
        wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        assertThat(wrapper.hasRotatedSecret(), is(Boolean.TRUE));
        assertThat(wrapper.getClientRotatedSecret(null), equalTo(secondSecret));
        long rotatedCreationTime = wrapper.getClientSecretCreationTime();
        assertThat(rotatedCreationTime, is(notNullValue()));
        assertThat(rotatedCreationTime, greaterThan(0L));
    }

    /**
     * When update a client with policy enabled and secret expired the secret rotation must be
     * performed
     */
    @Test
    public void updateClientPolicyEnabledSecretExpired() {

        configureDefaultProfileAndPolicy();

        ClientResource clientResource = oauth.clientResource();
        String firstSecret = clientResource.getSecret().getValue();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientRepresentation.setDescription("New Description Updated");
        clientResource.update(clientRepresentation);

        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        long secretCreationTime = wrapper.getClientSecretCreationTime();

        logger.debug("Current time " + Time.toDate(Time.currentTime()));
        //advance 1 hour
        timeOffSet.set(3601);
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
        assertThat(wrapper.getClientRotatedSecret(null), equalTo(firstSecret));
    }

    /**
     * When authenticate with client-id and secret and the policy is not enable the login must be
     * successfully (Keeps flow compatibility without secret rotation)
     */
    @Test
    public void authenticateWithValidClientNoPolicy() {
        successfulLoginAndLogout(CLIENT_NAME, DEFAULT_SECRET);
    }

    /**
     * When the secret rotation policy is active and the client's main secret has not yet expired,
     * the login should be successful.
     */
    @Test
    public void authenticateWithValidClientPolicyEnable() {
        configureDefaultProfileAndPolicy();
        successfulLoginAndLogout(CLIENT_NAME, DEFAULT_SECRET);
    }

    /**
     * When the secret rotation policy is active and the client's main secret has expired, the login
     * should not be successful.
     */
    @Test
    public void authenticateWithInvalidClientPolicyEnable() {
        configureDefaultProfileAndPolicy();

        //The first login will be successful
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.OK.getStatusCode()));
        oauth.doLogout(res.getRefreshToken());

        //advance 1 hour
        timeOffSet.set(3601);

        // the second login must fail
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        code = oauth.parseLoginResponse().getCode();
        res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.UNAUTHORIZED.getStatusCode()));
        AdminApiUtil.findUserByUsernameId(realm.admin(), TEST_USER_NAME).logout();
    }

    /**
     * When a client goes through a secret rotation, the current secret becomes a rotated secret. A
     * login attempt with the new secret and the rotated secret should be successful as long as none
     * of the client's secrets are expired.
     */
    @Test
    public void authenticateWithValidActualAndRotatedSecret() {
        configureDefaultProfileAndPolicy();

        // force client update. First update will not rotate the secret
        ClientResource clientResource = oauth.clientResource();
        String firstSecret = clientResource.getSecret().getValue();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientRepresentation.setDescription("New Description Updated");
        clientResource.update(clientRepresentation);

        //advance 1 hour
        timeOffSet.set(3601);

        // force client update (rotate the secret according to the policy)
        clientRepresentation = clientResource.toRepresentation();
        clientResource.update(clientRepresentation);

        String updatedSecret = clientResource.getSecret().getValue();
        assertThat(clientResource.getSecret().getValue(), not(equalTo(firstSecret)));

        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());

        oauth.client(CLIENT_NAME, updatedSecret);

        //login with new secret
        AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME,
                TEST_USER_PASSWORD);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.OK.getStatusCode()));
        oauth.doLogout(res.getRefreshToken());

        //login with rotated secret
        oauth.client(CLIENT_NAME, firstSecret);
        loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        code = oauth.parseLoginResponse().getCode();
        res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(Status.OK.getStatusCode()));
        oauth.doLogout(res.getRefreshToken());
    }

    /**
     * When a client goes through a secret rotation, the current secret becomes a rotated secret. A
     * login attempt with the rotated secret should not be successful if secret is expired.
     */
    @Test
    public void authenticateWithInValidRotatedSecret() {
        configureDefaultProfileAndPolicy();

        // force client update (rotate the secret according to the policy)
        ClientResource clientResource = oauth.clientResource();
        String firstSecret = clientResource.getSecret().getValue();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientRepresentation.setDescription("New Description Updated");
        clientResource.update(clientRepresentation);

        logger.debug(">>> secret creation time " + Time.toDate(Time.currentTime()));

        timeOffSet.set(3601);
        clientResource.update(clientResource.toRepresentation());

        logger.debug(">>> secret expiration time after first update " + new Date(TimeUnit.SECONDS.toMillis(
                OIDCClientSecretConfigWrapper.fromClientRepresentation(
                                clientResource.toRepresentation())
                        .getClientSecretExpirationTime())) + " | Time: " + Time.toDate(Time.currentTime()));

        // force rotation
        String updatedSecret = clientResource.getSecret().getValue();
        assertThat(updatedSecret, not(equalTo(firstSecret)));
        clientRepresentation = clientResource.toRepresentation();
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientRepresentation);

        logger.debug(
                ">>> secret expiration configured " + new Date(TimeUnit.SECONDS.toMillis(
                        wrapper.getClientSecretExpirationTime()))
                        + " | Time: " + Time.toDate(Time.currentTime()));

        timeOffSet.set(7201);

        logger.debug("client secret:" + updatedSecret + "\nsecret expiration: " + new Date(TimeUnit.SECONDS.toMillis(
                wrapper.getClientSecretExpirationTime())) + "\nrotated secret: "
                + wrapper.getClientRotatedSecret(null) + "\nrotated expiration: " + new Date(TimeUnit.SECONDS.toMillis(
                wrapper.getClientRotatedSecretExpirationTime())) + " | Time: " + Time.toDate(
                Time.currentTime()));
        logger.debug(">>> trying login at time " + Time.toDate(Time.currentTime()));

        oauth.client(CLIENT_NAME, firstSecret);

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
     */
    @Test
    public void authenticateWithRotatedSecretWithZeroExpirationTime() {
        configureCustomProfileAndPolicy(DEFAULT_EXPIRATION_PERIOD, 0, 0);

        ClientResource clientResource = oauth.clientResource();
        clientResource.update(clientResource.toRepresentation());

        //advance 1 hour
        timeOffSet.set(3601);

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

        oauth.client(CLIENT_NAME, firstSecret);

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
     */
    @Test
    public void createClientWithPolicyEnableSecretExpiredTime() {

        configureDefaultProfileAndPolicy();

        ClientResource clientResource = oauth.clientResource();
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientResource.remove();
        realm.admin().clients().create(clientRepresentation);

        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        long clientSecretExpirationTime = wrapper.getClientSecretExpirationTime();
        assertThat(clientSecretExpirationTime, is(not(0L)));
    }

    @Test
    public void secretExpirationWithLargeOffsetDoesNotOverflow() {
        long fiftyYearsInSeconds = TimeUnit.DAYS.toSeconds(365 * 50);
        configureCustomProfileAndPolicy(fiftyYearsInSeconds, TimeUnit.DAYS.toSeconds(2), TimeUnit.DAYS.toSeconds(2));

        ClientResource clientResource = oauth.clientResource();
        clientResource.generateNewSecret();

        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientRepresentation(
                clientResource.toRepresentation());
        long expirationTime = wrapper.getClientSecretExpirationTime();

        assertThat(expirationTime, is(greaterThan((long) Integer.MAX_VALUE)));
    }

    /**
     * After rotate the secret the endpoint must return the rotated secret
     */
    @Test
    public void getClientRotatedSecret() {
        configureDefaultProfileAndPolicy();

        ClientResource clientResource = oauth.clientResource();
        String firstSecret = clientResource.getSecret().getValue();
        assertThrows(NotFoundException.class, () -> clientResource.getClientRotatedSecret());

        String newSecret = clientResource.generateNewSecret().getValue();
        String rotatedSecret = clientResource.getClientRotatedSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));
        assertThat(firstSecret, equalTo(rotatedSecret));
    }

    /**
     * After rotate the secret it must be possible to invalidate the rotated secret
     */
    @Test
    public void invalidateClientRotatedSecret() {
        configureDefaultProfileAndPolicy();

        ClientResource clientResource = oauth.clientResource();
        String firstSecret = clientResource.getSecret().getValue();
        String newSecret = clientResource.generateNewSecret().getValue();
        String rotatedSecret = clientResource.getClientRotatedSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));
        assertThat(firstSecret, equalTo(rotatedSecret));
        clientResource.invalidateRotatedSecret();
        assertThrows(NotFoundException.class, () -> clientResource.getClientRotatedSecret());
    }

    /**
     * When try to create an executor for client secret rotation the configuration must be valid.
     * If the rules expressed in services/src/main/java/org/keycloak/services/clientpolicy/executor/ClientSecretRotationExecutor.Configuration is invalid, then the resource must not be created
     */
    @Test
    public void createExecutorConfigurationWithInvalidValues() {
        BadRequestException bre = assertThrows(BadRequestException.class, () -> doConfigProfile(getClientProfileConfiguration(60, 61, 30)));
        ErrorRepresentation error = bre.getResponse().readEntity(ErrorRepresentation.class);
        assertThat(error.getErrorMessage(), is("proposed client profile contains the executor, which does not have valid provider, or has invalid configuration."));

        // no profile must have been created due to the above error
        ClientPoliciesProfilesResource profileResource = realm.admin().clientPoliciesProfilesResource();
        ClientProfilesRepresentation profiles = profileResource.getProfiles(false);
        assertThat(profiles.getProfiles(), is(empty()));
    }

    @Test
    public void createExecutorConfigurationWithMissingConfigDoesNotCauseNPE() {
        ClientSecretRotationExecutor.Configuration missingExpirationPeriod = getClientProfileConfiguration(60, 30, 20);
        missingExpirationPeriod.setExpirationPeriod(null);
        ClientSecretRotationExecutor.Configuration missingRotatedExpirationPeriod = getClientProfileConfiguration(60, 30, 20);
        missingRotatedExpirationPeriod.setRotatedExpirationPeriod(null);
        ClientSecretRotationExecutor.Configuration missingRemainExpirationPeriod = getClientProfileConfiguration(60, 30, 20);
        missingRemainExpirationPeriod.setRemainExpirationPeriod(null);

        for (ClientSecretRotationExecutor.Configuration config : Arrays.asList(
                missingExpirationPeriod, missingRotatedExpirationPeriod, missingRemainExpirationPeriod)) {
            BadRequestException bre = assertThrows(BadRequestException.class, () -> doConfigProfile(config));
            ErrorRepresentation error = bre.getResponse().readEntity(ErrorRepresentation.class);
            assertThat(error.getErrorMessage(), is("proposed client profile contains the executor, which does not have valid provider, or has invalid configuration."));
        }

        ClientPoliciesProfilesResource profileResource = realm.admin().clientPoliciesProfilesResource();
        ClientProfilesRepresentation profiles = profileResource.getProfiles(false);
        assertThat(profiles.getProfiles(), is(empty()));
    }

    /**
     * When there is a client that has a secret rotated and the policy is disabled, Rotation information must be removed after updating a client
     */
    @Test
    public void removingPolicyMustClearRotationInformationFromClientOnUpdate() {
        //create and enable the profile
        configureDefaultProfileAndPolicy();
        //create client
        ClientResource clientResource = oauth.clientResource();
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
        assertThat(wrapper.getClientSecretExpirationTime(),is(0L));
    }

    /**
     * When there is a client that has a secret rotated and the policy is disabled, Rotation information must be removed after request a new secret
     */
    @Test
    public void removingPolicyMustClearRotationInformationFromClientOnRequestNewSecret() {
        //create and enable the profile
        configureDefaultProfileAndPolicy();
        //create client
        ClientResource clientResource = oauth.clientResource();
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
        assertThat(wrapper.getClientSecretExpirationTime(),is(0L));
    }

    /**
     * -------------------- support methods --------------------
     **/

    private ClientSecretRotationExecutor.Configuration getClientProfileConfiguration(
            long expirationPeriod, long rotatedExpirationPeriod, long remainExpirationPeriod) {
        ClientSecretRotationExecutor.Configuration profileConfig = new ClientSecretRotationExecutor.Configuration();
        profileConfig.setExpirationPeriod(expirationPeriod);
        profileConfig.setRotatedExpirationPeriod(rotatedExpirationPeriod);
        profileConfig.setRemainExpirationPeriod(remainExpirationPeriod);
        return profileConfig;
    }

    private void doConfigProfile(ClientSecretRotationExecutor.Configuration profileConfig) {
        ClientProfileRepresentation clientProfile = ClientProfileBuilder.create()
                .name(PROFILE_NAME)
                .description("Enable Client Secret Rotation")
                .executor(ClientSecretRotationExecutorFactory.PROVIDER_ID, profileConfig)
                .build();
        ClientProfilesRepresentation clientProfiles = new ClientProfilesRepresentation();
        clientProfiles.setProfiles(List.of(clientProfile));
        realm.admin().clientPoliciesProfilesResource().updateProfiles(clientProfiles);
    }

    private void configureDefaultProfileAndPolicy() {
        configureCustomProfileAndPolicy(DEFAULT_EXPIRATION_PERIOD, DEFAULT_ROTATED_EXPIRATION_PERIOD, DEFAULT_REMAIN_EXPIRATION_PERIOD);
    }

    private void configureCustomProfileAndPolicy(long secretExpiration, long rotatedExpiration, long remainingExpiration) {
        realm.updateWithCleanup(r -> {
            r.clientProfile(ClientProfileBuilder.create()
                    .name(PROFILE_NAME)
                    .description("Enable Client Secret Rotation")
                    .executor(ClientSecretRotationExecutorFactory.PROVIDER_ID, getClientProfileConfiguration(
                            secretExpiration, rotatedExpiration, remainingExpiration))
                    .build());

            r.clientPolicy(ClientPolicyBuilder.create()
                    .name(POLICY_NAME)
                    .description("Policy for Client Secret Rotation")
                    .condition(ClientAccessTypeConditionFactory.PROVIDER_ID, ClientPolicyBuilder.clientAccessTypeCondition(
                            false, ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL))
                    .profile(PROFILE_NAME)
                    .build());

            return r;
        });
    }

    private void disableProfile() {
        realm.updateWithCleanup(r -> {
            r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                            .name(POLICY_NAME)
                            .description("Policy for Client Secret Rotation")
                            .condition(ClientAccessTypeConditionFactory.PROVIDER_ID, ClientPolicyBuilder.clientAccessTypeCondition(
                                    false, ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL))
                            .profile(PROFILE_NAME)
                            .enabled(false)
                            .build());

            return r;
        });
    }

    private void successfulLoginAndLogout(String clientId, String clientSecret) {
        AccessTokenResponse res = successfulLogin(clientId, clientSecret);
        oauth.doLogout(res.getRefreshToken());
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT)
                .sessionId(res.getSessionState()).clientId(clientId).withoutDetails(Details.REDIRECT_URI);
    }

    private AccessTokenResponse successfulLogin(String clientId, String clientSecret) {
        oauth.client(clientId, clientSecret);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent).clientId(clientId);
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());
        EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(sessionId)
                .clientId(clientId)
                .details(Details.CODE_ID, codeId)
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID);

        return res;
    }

    static class ClientSecretRotationServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_SECRET_ROTATION);
        }
    }

    static class ClientSecretRotationRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create(TEST_USER_NAME).password(TEST_USER_PASSWORD)
                    .name("Test", "User").email(TEST_USER_NAME).emailVerified(true));
        }
    }

    static class OAuthClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(CLIENT_NAME)
                    .secret(DEFAULT_SECRET)
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .publicClient(false)
                    .serviceAccountsEnabled()
                    .redirectUris("*");
        }
    }
}
