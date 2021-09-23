package org.keycloak.testsuite.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.collections.map.UnmodifiableMap;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.OAuthClient;

import com.google.common.base.Charsets;

/**
 * Mutual TLS Client tests.
 */
public class MutualTLSClientTest extends AbstractTestRealmKeycloakTest {

   private static final boolean sslRequired = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

   private static final String CLIENT_ID = "confidential-x509";
   private static final String DISABLED_CLIENT_ID = "confidential-disabled-x509";
   private static final String EXACT_SUBJECT_DN_CLIENT_ID = "confidential-subjectdn-x509";
   private static final String OBB_SUBJECT_DN_CLIENT_ID = "obb-subjectdn-x509";
   private static final String USER = "keycloak-user@localhost";
   private static final String PASSWORD = "password";
   private static final String REALM = "test";
   private static final String EXACT_CERTIFICATE_SUBJECT_DN = "EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US";

   @Override
   public void configureTestRealm(RealmRepresentation testRealm) {
      ClientRepresentation properConfiguration = KeycloakModelUtils.createClient(testRealm, CLIENT_ID);
      properConfiguration.setServiceAccountsEnabled(Boolean.TRUE);
      properConfiguration.setRedirectUris(Arrays.asList("https://localhost:8543/auth/realms/master/app/auth"));
      properConfiguration.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
      properConfiguration.setAttributes(Collections.singletonMap(X509ClientAuthenticator.ATTR_SUBJECT_DN, "(.*?)(?:$)"));

      ClientRepresentation disabledConfiguration = KeycloakModelUtils.createClient(testRealm, DISABLED_CLIENT_ID);
      disabledConfiguration.setServiceAccountsEnabled(Boolean.TRUE);
      disabledConfiguration.setRedirectUris(Arrays.asList("https://localhost:8543/auth/realms/master/app/auth"));
      disabledConfiguration.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
      disabledConfiguration.setAttributes(Collections.singletonMap(X509ClientAuthenticator.ATTR_SUBJECT_DN, "(.*?)(?:$)"));

      ClientRepresentation exactSubjectDNConfiguration = KeycloakModelUtils.createClient(testRealm, EXACT_SUBJECT_DN_CLIENT_ID);
      exactSubjectDNConfiguration.setServiceAccountsEnabled(Boolean.TRUE);
      exactSubjectDNConfiguration.setRedirectUris(Arrays.asList("https://localhost:8543/auth/realms/master/app/auth"));
      exactSubjectDNConfiguration.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
      exactSubjectDNConfiguration.setAttributes(Collections.singletonMap(X509ClientAuthenticator.ATTR_SUBJECT_DN, EXACT_CERTIFICATE_SUBJECT_DN));

      ClientRepresentation obbSubjectDNConfiguration = KeycloakModelUtils.createClient(testRealm, OBB_SUBJECT_DN_CLIENT_ID);
      obbSubjectDNConfiguration.setServiceAccountsEnabled(Boolean.TRUE);
      obbSubjectDNConfiguration.setRedirectUris(Arrays.asList("https://localhost:8543/auth/realms/master/app/auth"));
      obbSubjectDNConfiguration.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
      // ATTR_SUBJECT_DN will be set in the individual tests based on the requested Subject DN Format
   }

   @BeforeClass
   public static void sslRequired() {
      Assume.assumeTrue("\"auth.server.ssl.required\" is required for Mutual TLS tests", sslRequired);
   }

   @Test
   public void testSuccessfulClientInvocationWithProperCertificate() throws Exception {
      //given
      Supplier<CloseableHttpClient> clientWithProperCertificate = MutualTLSUtils::newCloseableHttpClientWithDefaultKeyStoreAndTrustStore;

      //when
      OAuthClient.AccessTokenResponse token = loginAndGetAccessTokenResponse(CLIENT_ID, clientWithProperCertificate);

      //then
      assertTokenObtained(token);
   }

   @Test
   public void testSuccessfulClientInvocationWithProperCertificateAndSubjectDN() throws Exception {
      //given
      Supplier<CloseableHttpClient> clientWithProperCertificate = MutualTLSUtils::newCloseableHttpClientWithDefaultKeyStoreAndTrustStore;

      //when
      OAuthClient.AccessTokenResponse token = loginAndGetAccessTokenResponse(EXACT_SUBJECT_DN_CLIENT_ID, clientWithProperCertificate);

      //then
      assertTokenObtained(token);
   }

   @Test
   public void testSuccessfulClientInvocationWithClientIdInQueryParams() throws Exception {
      //given//when
      OAuthClient.AccessTokenResponse token = null;
      try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
         login(CLIENT_ID);
         token = getAccessTokenResponseWithQueryParams(CLIENT_ID, client);
      }

      //then
      assertTokenObtained(token);
   }

   @Test
   public void testFailedClientInvocationWithProperCertificateAndWrongSubjectDN() throws Exception {
      //given
      Supplier<CloseableHttpClient> clientWithProperCertificate = MutualTLSUtils::newCloseableHttpClientWithOtherKeyStoreAndTrustStore;

      //when
      OAuthClient.AccessTokenResponse token = loginAndGetAccessTokenResponse(EXACT_SUBJECT_DN_CLIENT_ID, clientWithProperCertificate);

      //then
      assertTokenNotObtained(token);
   }

   @Test
   public void testFailedClientInvocationWithoutCertificateCertificate() throws Exception {
      //given
      Supplier<CloseableHttpClient> clientWithoutCertificate = MutualTLSUtils::newCloseableHttpClientWithoutKeyStoreAndTrustStore;

      //when
      OAuthClient.AccessTokenResponse token = loginAndGetAccessTokenResponse(CLIENT_ID, clientWithoutCertificate);

      //then
      assertTokenNotObtained(token);
   }

   @Test
   public void testFailedClientInvocationWithDisabledClient() throws Exception {
      //given//when
      OAuthClient.AccessTokenResponse token = null;
      try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
         login(DISABLED_CLIENT_ID);

         disableClient(DISABLED_CLIENT_ID);

         token = getAccessTokenResponse(DISABLED_CLIENT_ID, client);
      }

      //then
      assertTokenNotObtained(token);
   }

   @Test
   public void testClientInvocationWithOBBClient_rfc2553_resolvedAttributes() throws Exception {
      // Attributes like "JURISDICTIONCOUNTRYNAME", "BUSINESSCATEGORY" and SERIALNUMBER" are resolved (expanded) in the expected Subject DN
      testClientInvocationWithOBBClient("CN=Foo,JURISDICTIONCOUNTRYNAME=BR,BUSINESSCATEGORY=Business Entity,SERIALNUMBER=2009,OU=My Org Unit,O=Red Hat,L=Boston,ST=MA,C=US",
              true);
   }

   @Test
   public void testClientInvocationWithOBBClient_rfc2553_unresolvedAttributes() throws Exception {
      // Format like this is used by OpenBanking Brasil testsuite. Attributes like "JURISDICTIONCOUNTRYNAME", "BUSINESSCATEGORY" and SERIALNUMBER" are NOT resolved (expanded) in the expected Subject DN
      testClientInvocationWithOBBClient("CN=Foo,1.3.6.1.4.1.311.60.2.1.3=#13024252,2.5.4.15=#130f427573696e65737320456e74697479,2.5.4.5=#130432303039,OU=My Org Unit,O=Red Hat,L=Boston,ST=MA,C=US",
              true);
   }

   @Test
   public void testClientInvocationWithOBBClient_rfc2553_invalidSubjectDN() throws Exception {
      // Test that authentication fails when certificate does not contain expected DN
      testClientInvocationWithOBBClient("CN=Foo,1.3.6.1.4.1.311.60.2.1.3=#13024252,2.5.4.15=#130f427573696e65737320456e74697479,2.5.4.5=#130e3037323337333733303030313230,OU=My Org Unit,O=Red Hat,L=Boston,ST=MA,C=US",
              false);
   }

   @Test
   public void testClientInvocationWithOBBClient_rfc1779() throws Exception {
      testClientInvocationWithOBBClient("CN=Foo, JURISDICTIONCOUNTRYNAME=BR, BUSINESSCATEGORY=Business Entity, SERIALNUMBER=2009, OU=My Org Unit, O=Red Hat, L=Boston, ST=MA, C=US",
              true);
   }

   private void testClientInvocationWithOBBClient(String expectedSubjectDN, boolean expectSuccess) throws Exception {
      //given
      Supplier<CloseableHttpClient> clientWithProperCertificate = MutualTLSUtils::newCloseableHttpClientWithOBBKeyStoreAndTrustStore;

      // Canonical
      ClientResource client = ApiUtil.findClientByClientId(testRealm(), OBB_SUBJECT_DN_CLIENT_ID);
      ClientRepresentation clientRep = client.toRepresentation();
      OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
      config.setAllowRegexPatternComparison(false);
      config.setTlsClientAuthSubjectDn(expectedSubjectDN);
      client.update(clientRep);

      OAuthClient.AccessTokenResponse token = loginAndGetAccessTokenResponse(OBB_SUBJECT_DN_CLIENT_ID, clientWithProperCertificate);
      if (expectSuccess) {
         assertTokenObtained(token);
      } else {
         assertTokenNotObtained(token);
      }
   }

   private OAuthClient.AccessTokenResponse loginAndGetAccessTokenResponse(String clientId, Supplier<CloseableHttpClient> client) throws IOException{
      try (CloseableHttpClient closeableHttpClient = client.get()) {
         login(clientId);
         return getAccessTokenResponse(clientId, closeableHttpClient);
      }  catch (IOException ioe) {
         throw ioe;
      }
   }

   private OAuthClient.AccessTokenResponse getAccessTokenResponse(String clientId, CloseableHttpClient closeableHttpClient) {
      String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
      // Call protected endpoint with supplied client.
      return oauth
            .httpClient(() -> closeableHttpClient)
            .clientId(clientId)
            .doAccessTokenRequest(code, null, closeableHttpClient);
   }

   private void login(String clientId) {
      // Login with default client, despite what has been supplied into this method.
      oauth
            .httpClient(OAuthClient::newCloseableHttpClient)
            .clientId(clientId)
            .doLogin(USER, PASSWORD);
   }

   private void assertTokenObtained(OAuthClient.AccessTokenResponse token) {
      Assert.assertEquals(200, token.getStatusCode());
      Assert.assertNotNull(token.getAccessToken());
   }

   private void assertTokenNotObtained(OAuthClient.AccessTokenResponse token) {
      Assert.assertEquals(400, token.getStatusCode());
      Assert.assertNull(token.getAccessToken());
   }

   /*
    * This is a very simplified version of OAuthClient#doAccessTokenRequest.
    * It test a scenario, where we do not follow the spec and specify client_id in Query Params (for in a form).
    */
   private OAuthClient.AccessTokenResponse getAccessTokenResponseWithQueryParams(String clientId, CloseableHttpClient client) throws Exception {
      OAuthClient.AccessTokenResponse token;// This is a very simplified version of
      HttpPost post = new HttpPost(oauth.getAccessTokenUrl() + "?client_id=" + clientId);
      List<NameValuePair> parameters = new LinkedList<>();
      parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
      parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, oauth.getCurrentQuery().get(OAuth2Constants.CODE)));
      parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
      UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, Charsets.UTF_8);
      post.setEntity(formEntity);

      return new OAuthClient.AccessTokenResponse(client.execute(post));
   }

   private void disableClient(String clientId) {
      ClientRepresentation disabledClientRepresentation = adminClient.realm(REALM).clients().findByClientId(clientId).get(0);
      ClientResource disabledClientResource = adminClient.realms().realm(REALM).clients().get(disabledClientRepresentation.getId());
      disabledClientRepresentation.setEnabled(false);
      disabledClientResource.update(disabledClientRepresentation);
   }
}
