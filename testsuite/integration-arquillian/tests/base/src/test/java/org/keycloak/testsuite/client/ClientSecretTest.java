package org.keycloak.testsuite.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientSecretConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCClientConfigWrapper;
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
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.OAuthClient.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.ClockUtil;

/**
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
public class ClientSecretTest extends AbstractKeycloakTest {

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

  private static final Logger logger = Logger.getLogger(ClientSecretTest.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final int DEFAULT_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.HOURS.toSeconds(1)).intValue();
  private static final int DEFAULT_ROTATED_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.MINUTES.toSeconds(10)).intValue();
  private static final int DEFAULT_REMAIN_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.MINUTES.toSeconds(30)).intValue();

  @Rule
  public AssertEvents events = new AssertEvents(this);

  @After
  public void after() throws Exception {
    revertToBuiltinProfiles();
    revertToBuiltinPolicies();
    ClockUtil.resetClock();
  }

  @Override
  public void addTestRealms(List<RealmRepresentation> testRealms) {
    RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"),
        RealmRepresentation.class);

    List<UserRepresentation> users = realm.getUsers();

    UserRepresentation user = UserBuilder.create().enabled(Boolean.TRUE).username(ADMIN_USER_NAME)
        .password(USER_PASSWORD)
        .addRoles(new String[]{AdminRoles.MANAGE_CLIENTS})
        .build();
    users.add(user);

    UserRepresentation commonUser = UserBuilder.create()
        .id(COMMON_USER_ID)
        .enabled(Boolean.TRUE)
        .username(COMMON_USER_NAME)
        .email(COMMON_USER_NAME + "@localhost")
        .password(USER_PASSWORD)
        .build();
    users.add(commonUser);

    realm.setUsers(users);
    testRealms.add(realm);
  }

  @Test
  public void whenCreateClientSecretCreationTimeMustExist() throws Exception {

    String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);

    OIDCClientConfigWrapper wrapper = OIDCClientConfigWrapper.fromClientRepresentation(
        clientResource.toRepresentation());
    assertThat(wrapper.getClientSecretCreationTime(), is(notNullValue()));
    String secret = clientResource.getSecret().getValue();
    assertThat(secret, is(notNullValue()));
    assertThat(secret, equalTo(DEFAULT_SECRET));
  }

  /**
   * When regenerate a client secret the creation time attribute must be updated, when the rotate
   * secret policy is not enable
   *
   * @throws Exception
   */
  @Test
  public void regenerateSecret() throws Exception {
    String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
    String secret = clientResource.getSecret().getValue();
    int secretCreationTime = OIDCClientConfigWrapper.fromClientRepresentation(
        clientResource.toRepresentation()).getClientSecretCreationTime();
    assertThat(secret, equalTo(DEFAULT_SECRET));
    String newSecret = clientResource.generateNewSecret().getValue();
    assertThat(newSecret, not(equalTo(secret)));
    int updatedSecretCreationTime = OIDCClientConfigWrapper.fromClientRepresentation(
        clientResource.toRepresentation()).getClientSecretCreationTime();
    assertThat(updatedSecretCreationTime, greaterThanOrEqualTo(secretCreationTime));
  }

  /**
   * When update a client with policy enabled and secret expiration is still valid the rotation must
   * not be performed
   *
   * @throws Exception
   */
  @Test
  public void updateClientWithPolicyAndSecretNotExpired() throws Exception {

    configureDefaultProfileAndPolicy();

    String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
    String secret = clientResource.getSecret().getValue();
    ClientRepresentation clientRepresentation = clientResource.toRepresentation();
    int secretCreationTime = OIDCClientConfigWrapper.fromClientRepresentation(clientRepresentation)
        .getClientSecretCreationTime();
    clientRepresentation.setDescription("New Description Updated");
    clientResource.update(clientRepresentation);
    assertThat(clientResource.getSecret().getValue(), equalTo(secret));
    assertThat(OIDCClientConfigWrapper.fromClientRepresentation(clientResource.toRepresentation())
        .getClientSecretCreationTime(), equalTo(secretCreationTime));
  }

  /**
   * When regenerate secret for a client and the expiration date is reached the policy must force a
   * secret rotation
   *
   * @throws Exception
   */
  @Test
  public void regenerateSecretAfterCurrentSecretExpires() throws Exception {

    String cidConfidential = createClientByAdmin(DEFAULT_CLIENT_ID);
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
    String firstSecret = clientResource.getSecret().getValue();
    String secondSecret = clientResource.generateNewSecret().getValue();
    assertThat(secondSecret, not(equalTo(firstSecret)));
    OIDCClientConfigWrapper wrapper = OIDCClientConfigWrapper.fromClientRepresentation(
        clientResource.toRepresentation());
    assertThat(wrapper.hasRotatedSecret(), is(Boolean.FALSE));

    //apply policy
    configureDefaultProfileAndPolicy();

    //advance 1 hour
    ClockUtil.plusHours(1);

    String newSecret = clientResource.generateNewSecret().getValue();
    assertThat(newSecret, not(equalTo(secondSecret)));
    wrapper = OIDCClientConfigWrapper.fromClientRepresentation(clientResource.toRepresentation());
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
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
    String firstSecret = clientResource.getSecret().getValue();
    ClientRepresentation clientRepresentation = clientResource.toRepresentation();
    OIDCClientConfigWrapper wrapper = OIDCClientConfigWrapper.fromClientRepresentation(
        clientRepresentation);
    int secretCreationTime = wrapper.getClientSecretCreationTime();
    clientRepresentation.setDescription("New Description Updated");

    //advance 1 hour
    ClockUtil.plusHours(1);

    clientResource.update(clientRepresentation);
    assertThat(clientResource.getSecret().getValue(), not(equalTo(firstSecret)));

    wrapper = OIDCClientConfigWrapper.fromClientRepresentation(clientResource.toRepresentation());
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
   * When the secret rotation policy is active and the client's main secret has not yet expired, the
   * login should be successful.
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

    //advance 1 hour
    ClockUtil.plusHours(1);

    oauth.clientId(clientId);
    AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME,
        TEST_USER_PASSWORD);
    String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
    OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, DEFAULT_SECRET);
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

    //advance 1 hour
    ClockUtil.plusHours(1);

    // force client update (rotate the secret according to the policy)
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
    String firstSecret = clientResource.getSecret().getValue();
    ClientRepresentation clientRepresentation = clientResource.toRepresentation();
    clientRepresentation.setDescription("New Description Updated");

    clientResource.update(clientRepresentation);
    String updatedSecret = clientResource.getSecret().getValue();
    assertThat(clientResource.getSecret().getValue(), not(equalTo(firstSecret)));

    OIDCClientConfigWrapper wrapper = OIDCClientConfigWrapper.fromClientRepresentation(
        clientResource.toRepresentation());

    oauth.clientId(clientId);

    //login with new secret
    AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
    String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
    OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, updatedSecret);
    assertThat(res.getStatusCode(), equalTo(Status.OK.getStatusCode()));
    oauth.doLogout(res.getRefreshToken(), updatedSecret);

    //login with rotated secret
    loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
    code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
    res = oauth.doAccessTokenRequest(code, firstSecret);
    assertThat(res.getStatusCode(), equalTo(Status.OK.getStatusCode()));
    oauth.doLogout(res.getRefreshToken(), firstSecret);

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

    //advance 1 hour
    ClockUtil.plusHours(1);

    // force client update (rotate the secret according to the policy)
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
    String firstSecret = clientResource.getSecret().getValue();
    ClientRepresentation clientRepresentation = clientResource.toRepresentation();
    clientRepresentation.setDescription("New Description Updated");

    clientResource.update(clientRepresentation);
    String updatedSecret = clientResource.getSecret().getValue();
    assertThat(updatedSecret, not(equalTo(firstSecret)));

    OIDCClientConfigWrapper wrapper = OIDCClientConfigWrapper.fromClientRepresentation(
        clientResource.toRepresentation());

    oauth.clientId(clientId);

    //advance 1 hour
    ClockUtil.plusHours(1);

    // try to login with rotated secret (must fail)
    oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
    String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
    OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, firstSecret);
    assertThat(res.getStatusCode(), equalTo(Status.UNAUTHORIZED.getStatusCode()));
    oauth.doLogout(res.getRefreshToken(), firstSecret);

  }

  /**
   * When a client goes through a secret rotation and the configuration for rotated secret is zero then the rotated secret is automatically invalidated, therefore the rotated secret is not valid for a successful login
   *
   * @throws Exception
   */
  @Test
  public void authenticateWithRotatedSecretWithZeroExpirationTime() throws Exception {
    configureCustomProfileAndPolicy(DEFAULT_EXPIRATION_PERIOD,0,0);
    String clientId = generateSuffixedName(CLIENT_NAME);
    String cidConfidential = createClientByAdmin(clientId);

    //advance 1 hour
    ClockUtil.plusHours(1);

    // force client update (rotate the secret according to the policy)
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
    String firstSecret = clientResource.getSecret().getValue();
    ClientRepresentation clientRepresentation = clientResource.toRepresentation();
    clientRepresentation.setDescription("New Description Updated");

    clientResource.update(clientRepresentation);
    String updatedSecret = clientResource.getSecret().getValue();
    //confirms rotation
    assertThat(updatedSecret, not(equalTo(firstSecret)));

    OIDCClientConfigWrapper wrapper = OIDCClientConfigWrapper.fromClientRepresentation(clientResource.toRepresentation());
    assertThat(wrapper.hasRotatedSecret(),is(Boolean.FALSE));

    // try to login with rotated secret (must fail)
    oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
    String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
    OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, firstSecret);
    assertThat(res.getStatusCode(), equalTo(Status.UNAUTHORIZED.getStatusCode()));
    oauth.doLogout(res.getRefreshToken(), firstSecret);

  }


  /** -------------------- support methods -------------------- **/

  private void configureCustomProfileAndPolicy(int secretExpiration,int rotatedExpiration,int remainingExpiration) throws Exception{
    ClientProfileBuilder profileBuilder = new ClientProfileBuilder();
    ClientSecretRotationExecutor.Configuration profileConfig = getClientProfileConfiguration(
        secretExpiration, rotatedExpiration,
        remainingExpiration);

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
        (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Policy for Client Secret Rotation",
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
    ClientRepresentation clientRep = new ClientRepresentation();
    clientRep.setClientId(clientId);
    clientRep.setName(CLIENT_NAME);
    clientRep.setSecret(DEFAULT_SECRET);
    clientRep.setAttributes(new HashMap<>());
    clientRep.getAttributes().put(ClientSecretConfig.CLIENT_SECRET_CREATION_TIME, String.valueOf(
        ClockUtil.currentTimeInSeconds()));
    clientRep.setProtocol(OIDC);
    clientRep.setBearerOnly(Boolean.FALSE);
    clientRep.setPublicClient(Boolean.FALSE);
    clientRep.setServiceAccountsEnabled(Boolean.TRUE);
    clientRep.setStandardFlowEnabled(Boolean.TRUE);
    clientRep.setImplicitFlowEnabled(Boolean.TRUE);
    clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);

    clientRep.setRedirectUris(Collections.singletonList(
        ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));

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

  protected String generateSuffixedName(String name) {
    return name + "-" + UUID.randomUUID().toString().subSequence(0, 7);
  }

  protected void updateProfiles(String json) throws ClientPolicyException {
    try {
      ClientProfilesRepresentation clientProfiles = JsonSerialization.readValue(json,
          ClientProfilesRepresentation.class);
      adminClient.realm(REALM_NAME).clientPoliciesProfilesResource().updateProfiles(clientProfiles);
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
      adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource().updatePolicies(clientPolicies);
    } catch (BadRequestException e) {
      throw new ClientPolicyException("update policies failed",
          e.getResponse().getStatusInfo().toString());
    } catch (IOException e) {
      throw new ClientPolicyException("update policies failed", e.getMessage());
    }
  }

  private void successfulLoginAndLogout(String clientId, String clientSecret) {
    OAuthClient.AccessTokenResponse res = successfulLogin(clientId, clientSecret);
    oauth.doLogout(res.getRefreshToken(), clientSecret);
    events.expectLogout(res.getSessionState()).client(clientId).clearDetails().assertEvent();
  }

  private OAuthClient.AccessTokenResponse successfulLogin(String clientId, String clientSecret) {
    oauth.clientId(clientId);
    oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

    EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
    String sessionId = loginEvent.getSessionId();
    String codeId = loginEvent.getDetails().get(Details.CODE_ID);
    String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
    OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
    assertEquals(200, res.getStatusCode());
    events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

    return res;
  }
}
