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
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.models.ClientSecretConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCClientConfigWrapper;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeCondition.Configuration;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutor;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.ClockUtil;

/**
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
public class ClientSecretTest extends AbstractKeycloakTest {

  private static final String OIDC = "openid-connect";
  private static final String CLIENT_ID = KeycloakModelUtils.generateId();
  private static final String REALM_NAME = "test";
  private static final String CLIENT_NAME = "confidential-client";
  private static final String DEFAULT_SECRET = "GFyDEriVTA9nAu92DenBknb5bjR5jdUM";
  private static final String PROFILE_NAME = "ClientSecretRotationProfile";
  private static final String POLICY_NAME = "ClientSecretRotationPolicy";
  private static final Logger logger = Logger.getLogger(ClientSecretTest.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final int EXPIRATION_PERIOD = Long.valueOf(TimeUnit.HOURS.toSeconds(1)).intValue();
  private static final int ROTATED_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.MINUTES.toSeconds(10))
      .intValue();
  private static final int REMAIN_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.MINUTES.toSeconds(30))
      .intValue();

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
    testRealms.add(realm);
  }

  @Test
  public void whenCreateClientSecretCreationTimeMustExist() throws Exception {

    String cidConfidential = createClientByAdmin();
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
    String cidConfidential = createClientByAdmin();
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

    String cidConfidential = createClientByAdmin();
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

    configureDefaultProfileAndPolicy();
    String cidConfidential = createClientByAdmin();
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
    String firstSecret = clientResource.getSecret().getValue();
    String secondSecret = clientResource.generateNewSecret().getValue();
    assertThat(secondSecret, not(equalTo(firstSecret)));
    OIDCClientConfigWrapper wrapper = OIDCClientConfigWrapper.fromClientRepresentation(
        clientResource.toRepresentation());
    assertThat(wrapper.hasRotatedSecret(), is(Boolean.FALSE));

    //advance 1 hour
    ClockUtil.plusHours(1);

    String newSecret = clientResource.generateNewSecret().getValue();
    assertThat(newSecret, not(equalTo(secondSecret)));
    wrapper = OIDCClientConfigWrapper.fromClientRepresentation(clientResource.toRepresentation());
    assertThat(wrapper.hasRotatedSecret(), is(Boolean.TRUE));
    assertThat(wrapper.getClientSecretRotated(), equalTo(secondSecret));
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

    String cidConfidential = createClientByAdmin();
    ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
    String firstSecret = clientResource.getSecret().getValue();
    ClientRepresentation clientRepresentation = clientResource.toRepresentation();
    OIDCClientConfigWrapper wrapper = OIDCClientConfigWrapper.fromClientRepresentation(clientRepresentation);
    int secretCreationTime = wrapper.getClientSecretCreationTime();
    clientRepresentation.setDescription("New Description Updated");

    //advance 1 hour
    ClockUtil.plusHours(1);

    clientResource.update(clientRepresentation);
    assertThat(clientResource.getSecret().getValue(), not(equalTo(firstSecret)));

    wrapper = OIDCClientConfigWrapper.fromClientRepresentation(clientResource.toRepresentation());
    assertThat(wrapper.getClientSecretCreationTime(), not(equalTo(secretCreationTime)));
    assertThat(wrapper.hasRotatedSecret(), is(Boolean.TRUE));
    assertThat(wrapper.getClientSecretRotated(), equalTo(firstSecret));
  }

  /** support methods **/
  private void configureDefaultProfileAndPolicy() throws Exception {
    // register profiles
    ClientProfileBuilder profileBuilder = new ClientProfileBuilder();
    ClientSecretRotationExecutor.Configuration profileConfig = new ClientSecretRotationExecutor.Configuration();
    profileConfig.setExpirationPeriod(EXPIRATION_PERIOD);
    profileConfig.setRotatedExpirationPeriod(ROTATED_EXPIRATION_PERIOD);
    profileConfig.setRemainExpirationPeriod(REMAIN_EXPIRATION_PERIOD);
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

  protected String createClientByAdmin() throws ClientPolicyException {
    ClientRepresentation clientRep = new ClientRepresentation();
    clientRep.setClientId(CLIENT_ID);
    clientRep.setName(CLIENT_NAME);
    clientRep.setSecret(DEFAULT_SECRET);
    clientRep.setAttributes(new HashMap<>());
    clientRep.getAttributes().put(ClientSecretConfig.CLIENT_SECRET_CREATION_TIME,String.valueOf(
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

  // TODO: Possibly change this to accept ClientProfilesRepresentation instead of String to have more type-safety.
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

  // TODO: Possibly change this to accept ClientPoliciesRepresentation instead of String to have more type-safety.
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
}
