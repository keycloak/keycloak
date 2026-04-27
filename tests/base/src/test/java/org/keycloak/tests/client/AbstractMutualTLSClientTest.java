package org.keycloak.tests.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.tests.utils.PasswordGenerateUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Mutual TLS Client tests.
 */
public abstract class AbstractMutualTLSClientTest {

    private static final String CLIENT_ID = "confidential-x509";
    private static final String DISABLED_CLIENT_ID = "confidential-disabled-x509";
    private static final String EXACT_SUBJECT_DN_CLIENT_ID = "confidential-subjectdn-x509";

    private static final String ISSUER_SUBJECT_DN_CLIENT_ID = "confidential-issuer-subjectdn-x509";
    private static final String OBB_SUBJECT_DN_CLIENT_ID = "obb-subjectdn-x509";
    private static final String USER = "keycloak-user@localhost";
    private static final String PASSWORD = PasswordGenerateUtil.generatePassword();

    public static final String DEFAULT_KEYSTORE_SUBJECT_DN = "EMAILADDRESS=test-user@localhost, CN=test-user@localhost, OU=Keycloak, O=Red Hat, L=Westford, ST=MA, C=US";
    public static final String CA_CERTIFICATE_SUBJECT_DN = "EMAILADDRESS=contact@keycloak.org, CN=Keycloak CA, OU=Keycloak, O=Red Hat, L=Boston, ST=MA, C=US";
    // This is DN of the issuer certificate, which signed certificate corresponding to EXACT_CERTIFICATE_SUBJECT_DN. This issuer certificate is present in the client.jks keystore on the 2nd position
    private static final String ISSUER_CERTIFICATE_SUBJECT_DN = "EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US";

    @InjectRealm(config = MutualTLSClientRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @AfterEach
    public void logout() {
        AccountHelper.logout(managedRealm.admin(), USER);
    }

    public abstract ManagedCertificates getManagedCertificates();

    @Test
    public void testSuccessfulClientInvocationWithProperCertificate() throws Exception {
        //when
        AccessTokenResponse token = loginAndGetAccessTokenResponse(CLIENT_ID, this::newCloseableHttpClient);

        //then
        assertTokenObtained(token);
    }

    @Test
    public void testSuccessfulClientInvocationWithProperCertificateAndSubjectDN() throws Exception {
        //when
        AccessTokenResponse token = loginAndGetAccessTokenResponse(EXACT_SUBJECT_DN_CLIENT_ID, this::newCloseableHttpClient);

        //then
        assertTokenObtained(token);
    }

    @Test
    public void testFailedClientInvocationWithIssuerCertificateAndSubjectDN() throws Exception {
        //when (Certificate with the client's expected subjectDN is available in the certificate chain, but not on the 1st position. Hence authentication should not be successful)
        AccessTokenResponse token = loginAndGetAccessTokenResponse(ISSUER_SUBJECT_DN_CLIENT_ID, this::newCloseableHttpClient);

        //then
        assertTokenNotObtained(token);
    }

    @Test
    public void testFailedClientInvocationWithProperCertificateAndIncorrectCASubjectDN() throws Exception {
        //when
        changeCASubjectDN(ISSUER_SUBJECT_DN_CLIENT_ID, "EMAILADDRESS=contact@other.org,CN=Keycloak CA,OU=Keycloak,O=Red Hat,L=Boston,ST=MA,C=US");
        AccessTokenResponse token = loginAndGetAccessTokenResponse(ISSUER_SUBJECT_DN_CLIENT_ID, this::newCloseableHttpClient);

        //then
        assertTokenNotObtained(token);
    }

    @Test
    public void testSuccessfulClientInvocationWithClientIdInQueryParams() throws Exception {
        //given//when
        AccessTokenResponse token;
        try (CloseableHttpClient client = this.newCloseableHttpClient()) {
            login(CLIENT_ID);
            token = getAccessTokenResponseWithQueryParams(CLIENT_ID, client);
        }

        //then
        assertTokenObtained(token);
    }

    @Test
    public void testFailedClientInvocationWithProperCertificateAndWrongSubjectDN() throws Exception {
        //when
        AccessTokenResponse token = loginAndGetAccessTokenResponse(EXACT_SUBJECT_DN_CLIENT_ID,
                () -> newCloseableHttpClient(getManagedCertificates().createClientSSLContext("keycloak.bcfks", "keycloak-truststore.bcfks", true)));

        //then
        assertTokenNotObtained(token);
    }

    @Test
    public void testFailedClientInvocationWithoutCertificateCertificate() throws Exception {
        //when
        AccessTokenResponse token = loginAndGetAccessTokenResponse(CLIENT_ID,
                () -> newCloseableHttpClient(getManagedCertificates().createClientSSLContext(null, "keycloak-truststore.bcfks", false)));

        //then
        assertTokenNotObtained(token);
    }

    @Test
    public void testFailedClientInvocationWithDisabledClient() throws Exception {
        //given//when
        AccessTokenResponse token;
        try (CloseableHttpClient client = this.newCloseableHttpClient()) {
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
        Supplier<CloseableHttpClient> clientWithProperCertificate = () -> newCloseableHttpClient(
                getManagedCertificates().createClientSSLContext("test-user-obb.bcfks", "keycloak-truststore.bcfks", true));

        // Canonical
        ClientResource client = AdminApiUtil.findClientByClientId(managedRealm.admin(), OBB_SUBJECT_DN_CLIENT_ID);
        ClientRepresentation clientRep = client.toRepresentation();
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        config.setTlsClientAuthSubjectDn(expectedSubjectDN);
        client.update(clientRep);

        AccessTokenResponse token = loginAndGetAccessTokenResponse(OBB_SUBJECT_DN_CLIENT_ID, clientWithProperCertificate);
        if (expectSuccess) {
            assertTokenObtained(token);
        } else {
            assertTokenNotObtained(token);
        }
    }

    private AccessTokenResponse loginAndGetAccessTokenResponse(String clientId, Supplier<CloseableHttpClient> client) throws IOException {
        try (CloseableHttpClient closeableHttpClient = client.get()) {
            login(clientId);
            return getAccessTokenResponse(clientId, closeableHttpClient);
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    private AccessTokenResponse getAccessTokenResponse(String clientId, CloseableHttpClient closeableHttpClient) {
        String code = oauth.parseLoginResponse().getCode();
        // Call protected endpoint with supplied client.
        try {
            oauth.httpClient().set(closeableHttpClient);
            return oauth.client(clientId)
                    .doAccessTokenRequest(code);
        } finally {
            oauth.httpClient().reset();
        }
    }

    private void login(String clientId) {
        // Login with default client, despite what has been supplied into this method.
        oauth.httpClient().reset();
        oauth.client(clientId)
                .doLogin(USER, PASSWORD);
    }

    private void assertTokenObtained(AccessTokenResponse token) {
        Assertions.assertEquals(200, token.getStatusCode());
        Assertions.assertNotNull(token.getAccessToken());
    }

    private void assertTokenNotObtained(AccessTokenResponse token) {
        Assertions.assertEquals(401, token.getStatusCode());
        Assertions.assertNull(token.getAccessToken());
    }

    /*
     * This is a very simplified version of OAuthClient#doAccessTokenRequest.
     * It test a scenario, where we do not follow the spec and specify client_id in Query Params (for in a form).
     */
    private AccessTokenResponse getAccessTokenResponseWithQueryParams(String clientId, CloseableHttpClient client) throws Exception {
        HttpPost post = new HttpPost(oauth.getEndpoints().getToken() + "?client_id=" + clientId);
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, oauth.parseLoginResponse().getCode()));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        return new AccessTokenResponse(client.execute(post));
    }

    private void disableClient(String clientId) {
        ClientRepresentation disabledClientRepresentation = managedRealm.admin().clients().findByClientId(clientId).get(0);
        ClientResource disabledClientResource = managedRealm.admin().clients().get(disabledClientRepresentation.getId());
        disabledClientRepresentation.setEnabled(false);
        disabledClientResource.update(disabledClientRepresentation);
        managedRealm.cleanup().add(r -> {
            disabledClientRepresentation.setEnabled(true);
            disabledClientResource.update(disabledClientRepresentation);
        });
    }

    private void changeCASubjectDN(String clientId, String caSubjectDN) {
        ClientRepresentation clientRep = managedRealm.admin().clients().findByClientId(clientId).get(0);
        ClientResource client = managedRealm.admin().clients().get(clientRep.getId());
        String oldValue = clientRep.getAttributes().get(X509ClientAuthenticator.ATTR_CA_SUBJECT_DN);
        clientRep.getAttributes().put(X509ClientAuthenticator.ATTR_CA_SUBJECT_DN, caSubjectDN);
        client.update(clientRep);
        managedRealm.cleanup().add(r -> {
            clientRep.getAttributes().put(X509ClientAuthenticator.ATTR_CA_SUBJECT_DN, oldValue);
            client.update(clientRep);
        });
    }

    private CloseableHttpClient newCloseableHttpClient() {
        return newCloseableHttpClient(getManagedCertificates().getClientSSLContext());
    }

    private CloseableHttpClient newCloseableHttpClient(SSLContext sslContext) {
        return HttpClientBuilder.create()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build();
    }

    private static class MutualTLSClientRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.clients(ClientBuilder.create(CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(X509ClientAuthenticator.PROVIDER_ID)
                    .redirectUris("*")
                    .attribute(X509ClientAuthenticator.ATTR_SUBJECT_DN, "(.*?)(?:$)")
                    .attribute(X509ClientAuthenticator.ATTR_CA_SUBJECT_DN, CA_CERTIFICATE_SUBJECT_DN));

            realm.clients(ClientBuilder.create(EXACT_SUBJECT_DN_CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(X509ClientAuthenticator.PROVIDER_ID)
                    .redirectUris("*")
                    .attribute(X509ClientAuthenticator.ATTR_SUBJECT_DN, DEFAULT_KEYSTORE_SUBJECT_DN)
                    .attribute(X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON, Boolean.FALSE.toString())
                    .attribute(X509ClientAuthenticator.ATTR_CA_SUBJECT_DN, CA_CERTIFICATE_SUBJECT_DN));

            realm.clients(ClientBuilder.create(ISSUER_SUBJECT_DN_CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(X509ClientAuthenticator.PROVIDER_ID)
                    .redirectUris("*")
                    .attribute(X509ClientAuthenticator.ATTR_SUBJECT_DN, ISSUER_CERTIFICATE_SUBJECT_DN)
                    .attribute(X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON, Boolean.FALSE.toString())
                    .attribute(X509ClientAuthenticator.ATTR_CA_SUBJECT_DN, CA_CERTIFICATE_SUBJECT_DN));

            realm.clients(ClientBuilder.create(DISABLED_CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(X509ClientAuthenticator.PROVIDER_ID)
                    .redirectUris("*")
                    .attribute(X509ClientAuthenticator.ATTR_SUBJECT_DN, "(.*?)(?:$)")
                    .attribute(X509ClientAuthenticator.ATTR_CA_SUBJECT_DN, CA_CERTIFICATE_SUBJECT_DN));

            realm.clients(ClientBuilder.create(OBB_SUBJECT_DN_CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(X509ClientAuthenticator.PROVIDER_ID)
                    .redirectUris("*")
                    .attribute(X509ClientAuthenticator.ATTR_SUBJECT_DN, ISSUER_CERTIFICATE_SUBJECT_DN)
                    .attribute(X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON, Boolean.FALSE.toString())
                    .attribute(X509ClientAuthenticator.ATTR_CA_SUBJECT_DN, CA_CERTIFICATE_SUBJECT_DN));

            realm.users(UserBuilder.create(USER)
                    .name("Keycloak", "User")
                    .email("keycloak-user@localhost")
                    .emailVerified(true)
                    .password(PASSWORD));

            return realm;
        }
    }
}
