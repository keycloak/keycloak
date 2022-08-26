/*
 *  Copyright 2021 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.oauth;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.*;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.models.Constants;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Tests for the JWT bearer assertion grant type flow (RFC 7523).
 */
@EnableFeature(value = Profile.Feature.ASSERTION_GRANT, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
public class AssertionGrantTest extends AbstractTestRealmKeycloakTest {
    // config
    private static String TEST_CLIENT_ID = "test-client";
    private static String TEST_CLIENT_SECRET = "test-client-secret";
    private static String TEST_USER = "test";
    private static String TEST_ISSUER = "testissuer";
    private static String TEST_AUDIENCE = "testaudience";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {}

    /**
     * Test to ensure the grant fails if the assertion grant feature has not been enabled. HTTP status 400
     * should be returned if the feature is not enabled in keycloak.
     */
    @Test
    @UncaughtServerErrorExpected
    @DisableFeature(value = Profile.Feature.ASSERTION_GRANT, skipRestart = true)
    public void checkFeatureDisabled() {
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, true, false);
        Assert.assertEquals(400, sendAssertionGrant(null).getStatus());
    }

    /**
     * Test to ensure the grant fails if the assertion grant flow has not been enabled on the client. HTTP status 403
     * forbidden is returned if the client does not have the grant enabled.
     */
    @Test
    public void checkGrantNotEnabled() {
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, true, false);
        Assert.assertEquals(403, sendAssertionGrant(null).getStatus());
    }

    /**
     * Test to ensure the grant fails if the client is not authenticated. HTTP status 403 is returned if the client is
     * public (assertion grant should only work with authenticated clients).
     */
    @Test
    public void checkPublicClient() {
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, true, true);
        Assert.assertEquals(403, sendAssertionGrant(null).getStatus());
    }

    /**
     * Test to ensure the grant fails if the request does not include an assertion JWT. HTTP status 400 should be returned
     * if no assertion is provided in the request.
     */
    @Test
    public void checkNoAssertion() {
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, false, true);
        Assert.assertEquals(400, sendAssertionGrant(null).getStatus());
    }

    /**
     * Test to ensure the grant fails if the provided assertion cannot be validated by the configured trusted issuers
     * private keys. HTTP status 403 should be returned if an invalid assertion was received.
     */
    @Test
    public void checkInvalidSignature() throws CertificateEncodingException {
        // Sending assertion grant with invalid assertion should return status code 400 bad request
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, false, true);
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "test-cert");
        setAssertionGrantConfig(
                TEST_CLIENT_ID,
                createAssertionGrantConfig(cert, TEST_ISSUER, TEST_AUDIENCE)
        );

        Assert.assertEquals(403, sendAssertionGrant(new HashMap<>(){{
            put("sub", TEST_USER);
            put("iss", TEST_ISSUER);
            put("aud", TEST_AUDIENCE);
        }}).getStatus());
    }

    /**
     * Test to ensure the grant fails if the audience in the assertion does not match the value in the trusted issuer
     * configuration. HTTP status 403 should be returned if an invalid assertion was received.
     */
    @Test
    public void checkInvalidAudience() {
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, false, true);
        setAssertionGrantConfig(TEST_CLIENT_ID,
                createAssertionGrantConfig(getRealmSigningCert(), TEST_ISSUER, TEST_AUDIENCE)
        );
        Assert.assertEquals(403, sendAssertionGrant(new HashMap<>(){{
            put("sub", TEST_USER);
            put("iss", TEST_ISSUER);
            put("aud", "BAD AUDIENCE");
        }}).getStatus());
    }

    /**
     * Test to ensure the grant fails if the issuer in the assertion does not match the value in the trusted issuer
     * configuration. HTTP status 403 should be returned if the issuer is invalid.
     */
    @Test
    public void checkInvalidIssuer() {
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, false, true);
        setAssertionGrantConfig(TEST_CLIENT_ID,
                createAssertionGrantConfig(getRealmSigningCert(), TEST_ISSUER, TEST_AUDIENCE)
        );

        Assert.assertEquals(403, sendAssertionGrant(new HashMap<>(){{
            put("sub", TEST_USER);
            put("iss", "BAD_ISSUER");
            put("aud", TEST_AUDIENCE);
        }}).getStatus());
    }

    /**
     * Test to ensure the grant fails if the user specified in the assertion does not exist. HTTP status 400 should be
     * returned if the user does not exist.
     */
    @Test
    public void checkInvalidUser(){
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, false, true);
        setAssertionGrantConfig(TEST_CLIENT_ID,
                createAssertionGrantConfig(getRealmSigningCert(), TEST_ISSUER, TEST_AUDIENCE)
        );

        Assert.assertEquals(400, sendAssertionGrant(new HashMap<>(){{
            put("sub", TEST_USER);
            put("iss", TEST_ISSUER);
            put("aud", TEST_AUDIENCE);
        }}).getStatus());
    }

    /**
     * Test to ensure the grant fails if the client is not authorized to impersonate the user specified in the assertion.
     * HTTP status 403 should be returned if the client is not authorized to impersonate the user.
     */
    @Test
    public void checkImpersonateNotAllowed(){
        addUser(TEST_USER);
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, false, true);
        setAssertionGrantConfig(TEST_CLIENT_ID,
                createAssertionGrantConfig(getRealmSigningCert(), TEST_ISSUER, TEST_AUDIENCE)
        );

        Assert.assertEquals(403, sendAssertionGrant(new HashMap<>(){{
            put("sub", TEST_USER);
            put("iss", TEST_ISSUER);
            put("aud", TEST_AUDIENCE);
        }}).getStatus());
    }

    /**
     * Test to ensure a valid assertion will properly authenticate the specified user and return a valid access token.
     *
     * @throws JWSInputException
     */
    @Test
    public void checkSuccess() throws JWSInputException {
        addUser(TEST_USER);
        addClient(TEST_CLIENT_ID, TEST_CLIENT_SECRET, false, true);
        setAssertionGrantConfig(TEST_CLIENT_ID,
                createAssertionGrantConfig(getRealmSigningCert(), TEST_ISSUER, TEST_AUDIENCE)
        );
        testingClient.server().run(session -> enableUserImpersonation(session, TEST_CLIENT_ID));

        // send assertion grant request
        Response response = sendAssertionGrant(new HashMap<>(){{
            put("sub", TEST_USER);
            put("iss", TEST_ISSUER);
            put("aud", TEST_AUDIENCE);
        }});

        // parse response
        AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
        int status = response.getStatus();

        // check response code
        Assert.assertEquals(200, status);

        // check access token
        Assert.assertNotNull(tokenResponse);
        Assert.assertNotNull(tokenResponse.getToken());
        AccessToken token = (new JWSInput(tokenResponse.getToken())).readJsonContent(AccessToken.class);
        Assert.assertNotNull(token);

        // check access token subject
        Assert.assertEquals(TEST_USER, token.getPreferredUsername());
    }

    /**
     * Helper function to add a user to Keycloak with the given username.
     *
     * @param username The username for the user to create
     */
    private void addUser(String username){
        testRealm().users().create(UserBuilder.create().username(username).enabled(true).build());
    }

    /**
     * Helper function to add a client to Keycloak with the given client ID.
     *
     * @param clientId The client ID to create the client with
     * @param clientSecret The client secret to set on the client
     * @param isPublicClient Set the client as a public client. If set, clientSecret can be null.
     * @param assertionGrantEnabled Enable the assertion grant flow on the client.
     */
    private void addClient(String clientId, String clientSecret, boolean isPublicClient, boolean assertionGrantEnabled) {
        // create the client
        ClientBuilder builder = ClientBuilder.create().clientId(clientId).name(clientId).enabled(true).attribute(Constants.OAUTH_ASSERTION_GRANT_ENABLED, Boolean.toString(assertionGrantEnabled)).protocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        if (clientSecret != null) {
            builder.secret(clientSecret);
        }

        ClientRepresentation rep = builder.build();
        rep.setPublicClient(isPublicClient);

        testRealm().clients().create(rep);
    }

    /**
     * Helper function to set the cross domain trust settings for the realm.
     * @param config The cross domain trust configurations to set on the realm
     */
    private void setRealmCrossDomainTrust(List<CrossDomainTrust> config){
        RealmRepresentation realm = testRealm().toRepresentation();
        Map<String, String> attrs = realm.getAttributes();
        attrs.put(CrossDomainTrust.REALM_CROSS_DOMAIN_TRUST_ATTRIBUTE, serialize(config));
        realm.setAttributes(attrs);
        testRealm().update(realm);
    }

    /**
     * Helper function to set the specified cross-domain trust ids on the specified client
     * @param clientId the client to configure
     * @param trustIds the cross-domain trust ids
     */
    private void setClientCrossDomainTrust(String clientId, Set<String> trustIds){
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).stream().findFirst().orElseThrow();
        Map<String, String> attrs = client.getAttributes();
        attrs.put(Constants.OAUTH_ASSERTION_GRANT_TRUSTED_ISSUER, serialize(trustIds));
        client.setAttributes(attrs);
        testRealm().clients().get(client.getId()).update(client);
    }

    /**
     * Helper function to set the client assertion grant config for the realm and link them to the specified client.
     * @param clientId The client to set the config on
     * @param config The list of cross domain trust configs to set
     */
    private void setAssertionGrantConfig(String clientId, List<CrossDomainTrust> config) {
        setRealmCrossDomainTrust(config);
        setClientCrossDomainTrust(clientId, config.stream().map(CrossDomainTrust::getIssuer).collect(Collectors.toSet()));
    }

    /**
     * Send an assertion grant request with the supplied claims.
     *
     * @param claims Map<String, String> containing the assertion JWT claims to sign
     * @return The response from the token endpoint
     */
    private Response sendAssertionGrant(Map<String, String> claims){
        ResteasyClient httpClient = AdminClientUtil.createResteasyClient();
        ResteasyWebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST_REALM_NAME)
                .path("protocol/openid-connect/token");

        // create assertion grant request body
        Form form = new Form()
                .param(OAuth2Constants.CLIENT_ID, TEST_CLIENT_ID)
                .param(OAuth2Constants.CLIENT_SECRET, TEST_CLIENT_SECRET)
                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.JWT_BEARER_GRANT_TYPE);

        // populate assertion parameter
        if (claims != null){
            form.param(OAuth2Constants.ASSERTION, generateAssertion(claims));
        }

        return exchangeUrl.request()
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .post(Entity.form(form));
    }

    /**
     * Generate a signed assertion with the supplied claims. Claims are serialized to support usage in the
     * testingClient.server().fetch lambda function.
     *
     * @param claims Map<String, String> containing the assertion JWT claims to sign
     * @return The signed assertion JWT
     */
    private String generateAssertion(Map<String, String> claims) {
        String serializedClaims = serialize(claims);

        String assertion = testingClient.server().fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST_REALM_NAME);

            KeyWrapper signingKey = session.keys().getKeysStream(realm)
                    .filter(key -> key.getUse().equals(KeyUse.SIG) && key.getAlgorithm().equals(Algorithm.RS256) )
                    .findFirst().orElseThrow();

            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, Algorithm.RS256);
            SignatureSignerContext signer = signatureProvider.signer(signingKey);

            // generate JWT
            JsonWebToken jwt = new JsonWebToken();
            for (Map.Entry<String, String> claim : deserialize(serializedClaims).entrySet()) {
                jwt.setOtherClaims(claim.getKey(), claim.getValue());
            }

            // sign JWT with signing key
            return (new JWSBuilder()).type("JWT").jsonContent(jwt).sign(signer);
        }, String.class);

        return assertion;

    }

    /**
     * Helper function to enable user impersonation for the given client.
     *
     * @param session The active keycloak session object
     * @param clientId The client ID to add to the user impersonation policy
     */
    private static void enableUserImpersonation(KeycloakSession session, String clientId){
        RealmModel realm = session.realms().getRealmByName(TEST_REALM_NAME);
        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        management.users().setPermissionsEnabled(true);

        // fetch client
        ClientModel client = realm.getClientByClientId(clientId);

        // allow client to impersonate user
        ClientPolicyRepresentation clientImpersonateRep = new ClientPolicyRepresentation();
        clientImpersonateRep.setName("clientImpersonators");
        clientImpersonateRep.addClient(client.getId());
        ResourceServer server = management.realmResourceServer();
        Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientImpersonateRep);

        // add impersonation policy to server
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
    }

    /**
     * Helper function to fetch the RSA public signing key and encode it as base64.
     *
     * @return The base64 encoded RSA public signing key
     */
    private String getRealmSigningCert(){
        return testRealm().keys().getKeyMetadata().getKeys().stream().filter(k -> k.getUse().equals(KeyUse.SIG) && k.getAlgorithm().equals(Algorithm.RS256)).findFirst().orElseThrow().getCertificate();
    }

    /**
     * Helper function to generate the client assertion grant trusted issuer config.
     *
     * @param cert The trusted X509 certificate
     * @param issuer The issuer that the assertion should be accepted from
     * @param audience The audience that will be validated on the assertion
     * @return The assertion grant config map that can be serialized and set as an attribute on the client.
     */
    private List<CrossDomainTrust> createAssertionGrantConfig(X509Certificate cert, String issuer, String audience) throws CertificateEncodingException {
        return createAssertionGrantConfig(new String(Base64.getEncoder().encode(cert.getEncoded()), StandardCharsets.UTF_8), issuer, audience);
    }

    /**
     * Helper function to generate the client assertion grant trusted issuer config.
     * @param encodedCertificate The encoded X509 certificate to trust
     * @param issuer The assertion grant issuer to trust
     * @param audience The target audience expected in the assertion
     * @return The assertion grant config map that can be serialized and set as an attribute on the client.
     */
    private List<CrossDomainTrust> createAssertionGrantConfig(String encodedCertificate, String issuer, String audience){
        return Collections.singletonList(new CrossDomainTrust() {{
            setCertificate(encodedCertificate);
            setIssuer(issuer);
            setAudience(audience);
        }});
    }

    /**
     * Helper function to serialize Java objects to json.
     *
     * @param map the object to serialize
     * @return the serialized object
     */
    private static String serialize(Object map) {
        try {
            return JsonSerialization.writeValueAsString(map);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper function to deserialize a Map<String, String> JSON object.
     *
     * @param json The JSON string to deserialize
     * @return The deserialized JSON string
     */
    private static Map<String, String> deserialize(String json) {
        try {
            return JsonSerialization.readValue(json, new TypeReference<HashMap<String, String>>() {
            });
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}
