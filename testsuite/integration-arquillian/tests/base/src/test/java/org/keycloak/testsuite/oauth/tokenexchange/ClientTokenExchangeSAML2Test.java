/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.oauth.tokenexchange;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.Algorithm;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.BasicAuthHelper;

import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
@DisableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2, skipRestart = true)
public class ClientTokenExchangeSAML2Test extends AbstractKeycloakTest {

    private static final String SAML_SIGNED_TARGET = "http://localhost:8080/saml-signed-assertion/";
    private static final String SAML_ENCRYPTED_TARGET = "http://localhost:8080/saml-encrypted-assertion/";
    private static final String SAML_SIGNED_AND_ENCRYPTED_TARGET = "http://localhost:8080/saml-signed-and-encrypted-assertion/";
    private static final String SAML_UNSIGNED_AND_UNENCRYPTED_TARGET = "http://localhost:8080/saml-unsigned-and-unencrypted-assertion/";

    private static final String ENCRYPTION_CERTIFICATE = "MIIDBjCCAe6gAwIBAgIJANPu/mvxOREdMA0GCSqGSIb3DQEBCwUAMDAxLjAsBgNVBAMTJWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9zYWxlcy1wb3N0LWVuYy8wIBcNMjQwNjIxMTkzMTE3WhgPMjEyNDA1MjgxOTMxMTdaMDAxLjAsBgNVBAMTJWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9zYWxlcy1wb3N0LWVuYy8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDE5iKDNNW5XxHAF0ITErZcHDYZI68z7u68n7o4dsiywkfOWf7jVnw7PJVnMeDEtLWtTO6f0tRTqJ4OV5HYdJ9+mhPJtn+2UuvrepyYa2IsC1eFPH98ZEtYapsE6ObvhKBQMcu5G/tQrxkCFY2ssDa99unwBH5STLyX78UvqKiYnkPCvIhkiPIHy8ab7DQowc+EE9XhlE3b63A65rp4G9R87rwgJX5VTM3h81WcDuWLPOg7YRYLZoorWz2p38/qL9gXY5NxIRK16EHGfw2W1dPrX3GyMOJbXVyrBNZ6m5IL9Wn7lBEJ/Dl7ZFMFB5W36QkJ+3aaNLT/Tu/Gz+7f24inAgMBAAGjITAfMB0GA1UdDgQWBBSk7RegFbEBruVbt/VFl2gZhZ2IpDANBgkqhkiG9w0BAQsFAAOCAQEAGyH1sXVU3HDMhCzP2k5fsJBGA+1iKLMsyyiGcaD/22anQ1uVU7iWPZH8mSJGWqkvo/4oFb7RjB2KzO/50wP0q/P/tymGsYoznt+MEJKKxYEqAYmIns7SKRIgv3xEfF8yQy2jOuULC9FTq/Pb3gd9Om40jmeJtYccDSICjEC+A2fcGe56ScuRRLt+3WFyIZUFH7Y9FYZQ3EYQ88UZg//5F1ddAzGtdMSeTanMxLKow7rUIm/+Sx6cd+Vkwo/SYdk4hsD8xZCYx8Ln4i3NKh+SzyvbYykyWVI2fwjplqvM5Md/M+SNvPtU9tkOCUxQqVfz/bwtTiqfjdSaUJlasgGByg==";
    private static final String ENCRYPTION_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDE5iKDNNW5XxHAF0ITErZcHDYZI68z7u68n7o4dsiywkfOWf7jVnw7PJVnMeDEtLWtTO6f0tRTqJ4OV5HYdJ9+mhPJtn+2UuvrepyYa2IsC1eFPH98ZEtYapsE6ObvhKBQMcu5G/tQrxkCFY2ssDa99unwBH5STLyX78UvqKiYnkPCvIhkiPIHy8ab7DQowc+EE9XhlE3b63A65rp4G9R87rwgJX5VTM3h81WcDuWLPOg7YRYLZoorWz2p38/qL9gXY5NxIRK16EHGfw2W1dPrX3GyMOJbXVyrBNZ6m5IL9Wn7lBEJ/Dl7ZFMFB5W36QkJ+3aaNLT/Tu/Gz+7f24inAgMBAAECggEATiW0zvR6Ww9jgST6AY3suNQtmH60O915/X07sMtcTq6TR1AqvNoHho8+EO4X8ppyfOzKzL4lrWqACNsytIFdCCdo8ScwuxFgN167pjcAiNCblPL0+k7oJJhzHFi/x5KQ+iM5Yye68EP+nfgl+cMahvznzm5KIKn6NCdi0M6U07VRuPIep0v5geqwLOYRWMm8guis5V1p6tpPm6ejplea0QaNpkGxpNuzE2GDJotPRja1TNZUBDV0cKPVY+00BOeuqbiM90V+uk+zRMb9UeeRsuufx2fnLythff19NTgnukgzxWPfU9sSzHen1If1Ul5Xmv3VRG6XhwvOWsLm1TqVuQKBgQD4YgOkRMtpm6BFhOp6pjBcy/H1hN54cMqcTHtpL4w9X7bW+LoN9alfxZiHIRS8+HNATpRtjyKoo5yOQ09NH12/4lFpEIPdkQPzJQIb+kh//QMqqtGcRblCitNObHnlz/HhYDJ3C0nA9frfXhkv3doBAKEELytceGbS1fJ2PcIi2wKBgQDK7+9AmuWXe1qtDt/21j5ymsqhDFjuriPdT6LNvE9ep36h+XRHLe7XEKCKqyOsfYJvK7QI8QQbvB8Jto3pxJf41kBJxmzI9n4SnBKKhInoIICRXXQN4tTDoXVXQGun0idvyhrNEVL3ryW3XPX/UJHFy/Hfjab0sYJm6F50WcQtJQKBgGojUBURBK8zPnCWlLAmdgIhcFqPFZX39MyHbjELjWzoirQgAzlV4bO4Ny5/N2Js9KrlKU4L3S6dA5hTMP7uyVvmtQ0lboPupPZwuQ8Fi5eNoZ3I8ttJfBnwQs1/UzOeAWlidw4ht7mKI1Lx3edzcOX+w8+K7IeON7oejIZ0a5IDAoGAXDrpmIoNWGg2kLpW7V73aKyS9NigvnEkWZus2SYBSHqFIeY2g3cLunCTFhKrluQ/2HibTQkEnfpEfOyb2KeBjhUJiL4GiNsF9z05a/zKlFXZOLepW/pASlzh8HKVuuLXC4Zl4ddCxtCyKoC0SIH8jlGfLsO5IjJemph2/RgjAYUCgYEAkE98bIHsK9jPbt+wnPPs6kyDGHy1JrG9yBlcHOPxsnpxWLFXuxU+9D0qkpbfA28D4jAgehpePzlNPXkF4uIlgarYRDIKss/dX6QQXmmBKjY8UEu+doZYpJGO9SnSuUyih6eRlC/7x9zER/uPjJYia055u2VB0GqO51PKAgq/tqc=";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealmRep.setAccessCodeLifespan(60); // Used as default assertion lifespan
        testRealms.add(testRealmRep);
    }

    public static void setupRealm(KeycloakSession session) {
        addTargetClients(session);
        addDirectExchanger(session);

        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel exampleRole = realm.getRole("example");

        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        RoleModel impersonateRole = management.getRealmPermissionsClient().getRole(AdminRoles.IMPERSONATION);

        ClientModel clientExchanger = realm.addClient("client-exchanger");
        clientExchanger.setClientId("client-exchanger");
        clientExchanger.setPublicClient(false);
        clientExchanger.setDirectAccessGrantsEnabled(true);
        clientExchanger.setEnabled(true);
        clientExchanger.setSecret("secret");
        clientExchanger.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientExchanger.setFullScopeAllowed(false);
        clientExchanger.addScopeMapping(impersonateRole);
        clientExchanger.addProtocolMapper(UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_ID));
        clientExchanger.addProtocolMapper(UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_USERNAME));

        ClientModel illegal = realm.addClient("illegal");
        illegal.setClientId("illegal");
        illegal.setPublicClient(false);
        illegal.setDirectAccessGrantsEnabled(true);
        illegal.setEnabled(true);
        illegal.setSecret("secret");
        illegal.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        illegal.setFullScopeAllowed(false);

        ClientModel legal = realm.addClient("legal");
        legal.setClientId("legal");
        legal.setPublicClient(false);
        legal.setDirectAccessGrantsEnabled(true);
        legal.setEnabled(true);
        legal.setSecret("secret");
        legal.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        legal.setFullScopeAllowed(false);

        ClientModel directLegal = realm.addClient("direct-legal");
        directLegal.setClientId("direct-legal");
        directLegal.setPublicClient(false);
        directLegal.setDirectAccessGrantsEnabled(true);
        directLegal.setEnabled(true);
        directLegal.setSecret("secret");
        directLegal.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directLegal.setFullScopeAllowed(false);

        ClientModel directPublic = realm.addClient("direct-public");
        directPublic.setClientId("direct-public");
        directPublic.setPublicClient(true);
        directPublic.setDirectAccessGrantsEnabled(true);
        directPublic.setEnabled(true);
        directPublic.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directPublic.setFullScopeAllowed(false);

        ClientModel directNoSecret = realm.addClient("direct-no-secret");
        directNoSecret.setClientId("direct-no-secret");
        directNoSecret.setPublicClient(false);
        directNoSecret.setDirectAccessGrantsEnabled(true);
        directNoSecret.setEnabled(true);
        directNoSecret.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directNoSecret.setFullScopeAllowed(false);

        // permission for client to client exchange to "target" client
        ClientPolicyRepresentation clientRep = new ClientPolicyRepresentation();
        clientRep.setName("to");
        clientRep.addClient(clientExchanger.getId());
        clientRep.addClient(legal.getId());
        clientRep.addClient(directLegal.getId());

        ClientModel samlSignedTarget = realm.getClientByClientId(SAML_SIGNED_TARGET);
        ClientModel samlEncryptedTarget = realm.getClientByClientId(SAML_ENCRYPTED_TARGET);
        ClientModel samlSignedAndEncryptedTarget = realm.getClientByClientId(SAML_SIGNED_AND_ENCRYPTED_TARGET);
        ClientModel samlUnsignedAndUnencryptedTarget = realm.getClientByClientId(SAML_UNSIGNED_AND_UNENCRYPTED_TARGET);
        assertNotNull(samlSignedTarget);
        assertNotNull(samlEncryptedTarget);
        assertNotNull(samlSignedAndEncryptedTarget);
        assertNotNull(samlUnsignedAndUnencryptedTarget);

        ResourceServer server = management.realmResourceServer();
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientRep);
        management.clients().exchangeToPermission(samlSignedTarget).addAssociatedPolicy(clientPolicy);
        management.clients().exchangeToPermission(samlEncryptedTarget).addAssociatedPolicy(clientPolicy);
        management.clients().exchangeToPermission(samlSignedAndEncryptedTarget).addAssociatedPolicy(clientPolicy);
        management.clients().exchangeToPermission(samlUnsignedAndUnencryptedTarget).addAssociatedPolicy(clientPolicy);

        // permission for user impersonation for a client

        ClientPolicyRepresentation clientImpersonateRep = new ClientPolicyRepresentation();
        clientImpersonateRep.setName("clientImpersonators");
        clientImpersonateRep.addClient(directLegal.getId());
        clientImpersonateRep.addClient(directPublic.getId());
        clientImpersonateRep.addClient(directNoSecret.getId());
        server = management.realmResourceServer();
        Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientImpersonateRep);
        management.users().setPermissionsEnabled(true);
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        UserModel user = session.users().addUser(realm, "user");
        user.setEnabled(true);
        user.credentialManager().updateCredential(UserCredentialModel.password("password"));
        user.grantRole(exampleRole);
        user.grantRole(impersonateRole);

        UserModel bad = session.users().addUser(realm, "bad-impersonator");
        bad.setEnabled(true);
        bad.credentialManager().updateCredential(UserCredentialModel.password("password"));
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeToSAML2SignedAssertion() throws Exception {
        testingClient.server().run(ClientTokenExchangeSAML2Test::setupRealm);

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");
        org.keycloak.testsuite.util.oauth.AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        {
            response = oauth.tokenExchangeRequest(accessToken).audience(SAML_SIGNED_TARGET).requestedTokenType(OAuth2Constants.SAML2_TOKEN_TYPE).send();

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), StandardCharsets.UTF_8);

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Verify assertion
            Element assertionElement = DocumentUtil.getDocument(assertionXML).getDocumentElement();
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString()));

            // Expires
            Assert.assertEquals(60, response.getExpiresIn());

            // Audience
            AudienceRestrictionType aud = (AudienceRestrictionType) assertion.getConditions().getConditions().get(0);
            Assert.assertEquals(SAML_SIGNED_TARGET, aud.getAudience().get(0).toString());

            // NameID
            Assert.assertEquals("user", ((NameIDType) assertion.getSubject().getSubType().getBaseID()).getValue());

            // Role mapping
            List<String> roles = AssertionUtil.getRoles(assertion, null);
            Assert.assertTrue(roles.contains("example"));
        }

        {
            oauth.client("legal", "secret");
            response = oauth.tokenExchangeRequest(accessToken).audience(SAML_SIGNED_TARGET).requestedTokenType(OAuth2Constants.SAML2_TOKEN_TYPE).send();

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), StandardCharsets.UTF_8);

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Verify assertion
            Element assertionElement = DocumentUtil.getDocument(assertionXML).getDocumentElement();
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString()));

            // Audience
            AudienceRestrictionType aud = (AudienceRestrictionType) assertion.getConditions().getConditions().get(0);
            Assert.assertEquals(SAML_SIGNED_TARGET, aud.getAudience().get(0).toString());

            // NameID
            Assert.assertEquals("user", ((NameIDType) assertion.getSubject().getSubType().getBaseID()).getValue());

            // Role mapping
            List<String> roles = AssertionUtil.getRoles(assertion, null);
            Assert.assertTrue(roles.contains("example"));
        }
        {
            oauth.client("illegal", "secret");
            response = oauth.tokenExchangeRequest(accessToken).audience(SAML_SIGNED_TARGET).requestedTokenType(OAuth2Constants.SAML2_TOKEN_TYPE).send();
            Assert.assertEquals(403, response.getStatusCode());
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeToSAML2EncryptedAssertion() throws Exception {
        testingClient.server().run(ClientTokenExchangeSAML2Test::setupRealm);

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");
        org.keycloak.testsuite.util.oauth.AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        {
            response = oauth.tokenExchangeRequest(accessToken).audience(SAML_ENCRYPTED_TARGET).requestedTokenType(OAuth2Constants.SAML2_TOKEN_TYPE).send();

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), StandardCharsets.UTF_8);

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Decrypt assertion
            Document assertionDoc = DocumentUtil.getDocument(assertionXML);
            Element assertionElement = XMLEncryptionUtil.decryptElementInDocument(assertionDoc, data -> Collections.singletonList(privateKeyFromString(ENCRYPTION_PRIVATE_KEY)));
            Assert.assertFalse(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);

            // Expires
            Assert.assertEquals(30, response.getExpiresIn());

            // Audience
            AudienceRestrictionType aud = (AudienceRestrictionType) assertion.getConditions().getConditions().get(0);
            Assert.assertEquals(SAML_ENCRYPTED_TARGET, aud.getAudience().get(0).toString());

            // NameID
            Assert.assertEquals("user", ((NameIDType) assertion.getSubject().getSubType().getBaseID()).getValue());

            // Role mapping
            List<String> roles = AssertionUtil.getRoles(assertion, null);
            Assert.assertTrue(roles.contains("example"));
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeToSAML2SignedAndEncryptedAssertion() throws Exception {
        testingClient.server().run(ClientTokenExchangeSAML2Test::setupRealm);

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");
        org.keycloak.testsuite.util.oauth.AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        {
            response = oauth.tokenExchangeRequest(accessToken).audience(SAML_SIGNED_AND_ENCRYPTED_TARGET).requestedTokenType(OAuth2Constants.SAML2_TOKEN_TYPE).send();

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), StandardCharsets.UTF_8);

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Verify assertion
            Document assertionDoc = DocumentUtil.getDocument(assertionXML);
            Element assertionElement = XMLEncryptionUtil.decryptElementInDocument(assertionDoc, data -> Collections.singletonList(privateKeyFromString(ENCRYPTION_PRIVATE_KEY)));
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString()));

            // Audience
            AudienceRestrictionType aud = (AudienceRestrictionType) assertion.getConditions().getConditions().get(0);
            Assert.assertEquals(SAML_SIGNED_AND_ENCRYPTED_TARGET, aud.getAudience().get(0).toString());

            // NameID
            Assert.assertEquals("user", ((NameIDType) assertion.getSubject().getSubType().getBaseID()).getValue());

            // Role mapping
            List<String> roles = AssertionUtil.getRoles(assertion, null);
            Assert.assertTrue(roles.contains("example"));
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeToSAML2UnsignedAndUnencryptedAssertion() throws Exception {
        testingClient.server().run(ClientTokenExchangeSAML2Test::setupRealm);

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");
        org.keycloak.testsuite.util.oauth.AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        {
            response = oauth.tokenExchangeRequest(accessToken).audience(SAML_UNSIGNED_AND_UNENCRYPTED_TARGET).requestedTokenType(OAuth2Constants.SAML2_TOKEN_TYPE).send();

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), StandardCharsets.UTF_8);

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Verify assertion
            Document assertionDoc = DocumentUtil.getDocument(assertionXML);
            Assert.assertFalse(AssertionUtil.isSignedElement(assertionDoc.getDocumentElement()));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionDoc);

            // Audience
            AudienceRestrictionType aud = (AudienceRestrictionType) assertion.getConditions().getConditions().get(0);
            Assert.assertEquals(SAML_UNSIGNED_AND_UNENCRYPTED_TARGET, aud.getAudience().get(0).toString());

            // NameID
            Assert.assertEquals("user", ((NameIDType) assertion.getSubject().getSubType().getBaseID()).getValue());

            // Role mapping
            List<String> roles = AssertionUtil.getRoles(assertion, null);
            Assert.assertTrue(roles.contains("example"));
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testImpersonation() throws Exception {
        testingClient.server().run(ClientTokenExchangeSAML2Test::setupRealm);

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");

        org.keycloak.testsuite.util.oauth.AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));


        // client-exchanger can impersonate from token "user" to user "impersonated-user" and to "target" client
        {
            response = oauth.tokenExchangeRequest(accessToken).audience(SAML_SIGNED_TARGET).requestedTokenType(OAuth2Constants.SAML2_TOKEN_TYPE).requestedSubject("impersonated-user").send();

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), StandardCharsets.UTF_8);

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Verify assertion
            Element assertionElement = DocumentUtil.getDocument(assertionXML).getDocumentElement();
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString()));

            // Audience
            AudienceRestrictionType aud = (AudienceRestrictionType) assertion.getConditions().getConditions().get(0);
            Assert.assertEquals(SAML_SIGNED_TARGET, aud.getAudience().get(0).toString());

            // NameID
            Assert.assertEquals("impersonated-user", ((NameIDType) assertion.getSubject().getSubType().getBaseID()).getValue());

            // Role mapping
            List<String> roles = AssertionUtil.getRoles(assertion, null);
            Assert.assertTrue(roles.contains("example"));
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testBadImpersonator() throws Exception {
        testingClient.server().run(ClientTokenExchangeSAML2Test::setupRealm);

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");

        org.keycloak.testsuite.util.oauth.AccessTokenResponse response = oauth.doPasswordGrantRequest("bad-impersonator", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "bad-impersonator");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        // test that user does not have impersonator permission
        {
            response = oauth.tokenExchangeRequest(accessToken).audience(SAML_SIGNED_TARGET).requestedTokenType(OAuth2Constants.SAML2_TOKEN_TYPE).requestedSubject("impersonated-user").send();
            Assert.assertEquals(403, response.getStatusCode());
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testDirectImpersonation() throws Exception {
        testingClient.server().run(ClientTokenExchangeSAML2Test::setupRealm);
        Client httpClient = AdminClientUtil.createResteasyClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        // direct-legal can impersonate from token "user" to user "impersonated-user" and to "target" client
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-legal", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, SAML_SIGNED_TARGET)
                    ));
            Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String exchangedTokenString = accessTokenResponse.getToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), StandardCharsets.UTF_8);

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, accessTokenResponse.getOtherClaims().get(OAuth2Constants.ISSUED_TOKEN_TYPE));

            // Verify assertion
            Element assertionElement = DocumentUtil.getDocument(assertionXML).getDocumentElement();
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString()));

            // Audience
            AudienceRestrictionType aud = (AudienceRestrictionType) assertion.getConditions().getConditions().get(0);
            Assert.assertEquals(SAML_SIGNED_TARGET, aud.getAudience().get(0).toString());

            // NameID
            Assert.assertEquals("impersonated-user", ((NameIDType) assertion.getSubject().getSubType().getBaseID()).getValue());

            // Role mapping
            List<String> roles = AssertionUtil.getRoles(assertion, null);
            Assert.assertTrue(roles.contains("example"));
        }

        // direct-public fails impersonation
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-public", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, SAML_SIGNED_TARGET)
                    ));
            Assert.assertEquals(403, response.getStatus());
            response.close();
        }

        // direct-no-secret fails impersonation
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-no-secret", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, SAML_SIGNED_TARGET)
                    ));
            Assert.assertTrue(response.getStatus() >= 400);
            response.close();
        }
    }

    private static void addTargetClients(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);

        // Create SAML 2.0 target clients
        ClientModel samlSignedTarget = realm.addClient(SAML_SIGNED_TARGET);
        samlSignedTarget.setClientId(SAML_SIGNED_TARGET);
        samlSignedTarget.setEnabled(true);
        samlSignedTarget.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        samlSignedTarget.setFullScopeAllowed(true);
        samlSignedTarget.setAttribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true");
        samlSignedTarget.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                SAML_SIGNED_TARGET + "endpoint");
        samlSignedTarget.setAttribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username");
        samlSignedTarget.setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true");
        samlSignedTarget.setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        samlSignedTarget.setAttribute(SamlConfigAttributes.SAML_ENCRYPT, "false");

        ClientModel samlEncryptedTarget = realm.addClient(SAML_ENCRYPTED_TARGET);
        samlEncryptedTarget.setClientId(SAML_ENCRYPTED_TARGET);
        samlEncryptedTarget.setEnabled(true);
        samlEncryptedTarget.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        samlEncryptedTarget.setFullScopeAllowed(true);
        samlEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true");
        samlEncryptedTarget.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                SAML_ENCRYPTED_TARGET + "endpoint");
        samlEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username");
        samlEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        samlEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        samlEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_ENCRYPT, "true");
        samlEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, ENCRYPTION_CERTIFICATE);
        samlEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_ASSERTION_LIFESPAN, "30");

        ClientModel samlSignedAndEncryptedTarget = realm.addClient(SAML_SIGNED_AND_ENCRYPTED_TARGET);
        samlSignedAndEncryptedTarget.setClientId(SAML_SIGNED_AND_ENCRYPTED_TARGET);
        samlSignedAndEncryptedTarget.setEnabled(true);
        samlSignedAndEncryptedTarget.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        samlSignedAndEncryptedTarget.setFullScopeAllowed(true);
        samlSignedAndEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true");
        samlSignedAndEncryptedTarget.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                SAML_SIGNED_AND_ENCRYPTED_TARGET + "endpoint");
        samlSignedAndEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username");
        samlSignedAndEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true");
        samlSignedAndEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        samlSignedAndEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_ENCRYPT, "true");
        samlSignedAndEncryptedTarget.setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, ENCRYPTION_CERTIFICATE);

        ClientModel samlUnsignedAndUnencryptedTarget = realm.addClient(SAML_UNSIGNED_AND_UNENCRYPTED_TARGET);
        samlUnsignedAndUnencryptedTarget.setClientId(SAML_UNSIGNED_AND_UNENCRYPTED_TARGET);
        samlUnsignedAndUnencryptedTarget.setEnabled(true);
        samlUnsignedAndUnencryptedTarget.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        samlUnsignedAndUnencryptedTarget.setFullScopeAllowed(true);
        samlUnsignedAndUnencryptedTarget.setAttribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true");
        samlUnsignedAndUnencryptedTarget.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                SAML_UNSIGNED_AND_UNENCRYPTED_TARGET + "endpoint");
        samlUnsignedAndUnencryptedTarget.setAttribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username");
        samlUnsignedAndUnencryptedTarget.setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        samlUnsignedAndUnencryptedTarget.setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        samlUnsignedAndUnencryptedTarget.setAttribute(SamlConfigAttributes.SAML_ENCRYPT, "false");
    }

    private static void addDirectExchanger(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel exampleRole = realm.addRole("example");
        AdminPermissionManagement management = AdminPermissions.management(session, realm);

        ClientModel directExchanger = realm.addClient("direct-exchanger");
        directExchanger.setName("direct-exchanger");
        directExchanger.setClientId("direct-exchanger");
        directExchanger.setPublicClient(false);
        directExchanger.setDirectAccessGrantsEnabled(true);
        directExchanger.setEnabled(true);
        directExchanger.setSecret("secret");
        directExchanger.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directExchanger.setFullScopeAllowed(false);

        // permission for client to client exchange to "target" client
        management.clients().setPermissionsEnabled(realm.getClientByClientId(SAML_SIGNED_TARGET), true);
        management.clients().setPermissionsEnabled(realm.getClientByClientId(SAML_ENCRYPTED_TARGET), true);
        management.clients().setPermissionsEnabled(realm.getClientByClientId(SAML_SIGNED_AND_ENCRYPTED_TARGET), true);
        management.clients().setPermissionsEnabled(realm.getClientByClientId(SAML_UNSIGNED_AND_UNENCRYPTED_TARGET), true);

        ClientPolicyRepresentation clientImpersonateRep = new ClientPolicyRepresentation();
        clientImpersonateRep.setName("clientImpersonatorsDirect");
        clientImpersonateRep.addClient(directExchanger.getId());

        ResourceServer server = management.realmResourceServer();
        Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientImpersonateRep);
        management.users().setPermissionsEnabled(true);
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        UserModel impersonatedUser = session.users().addUser(realm, "impersonated-user");
        impersonatedUser.setEnabled(true);
        impersonatedUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
        impersonatedUser.grantRole(exampleRole);
    }

    private PublicKey publicKeyFromString() {
        KeysMetadataRepresentation.KeyMetadataRepresentation keyRep = KeyUtils.findActiveSigningKey(adminClient.realm(TEST), Algorithm.RS256);
        return org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(keyRep.getPublicKey());
    }

    private PrivateKey privateKeyFromString(String privateKey) {
        return org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(privateKey);
    }
}
