/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.oauth;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ImpersonationConstants;
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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.BasicAuthHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
public class ClientTokenExchangeSAML2Test extends AbstractKeycloakTest {

    private static final String SAML_SIGNED_TARGET = "http://localhost:8080/saml-signed-assertion/";
    private static final String SAML_ENCRYPTED_TARGET = "http://localhost:8080/saml-encrypted-assertion/";
    private static final String SAML_SIGNED_AND_ENCRYPTED_TARGET = "http://localhost:8080/saml-signed-and-encrypted-assertion/";
    private static final String SAML_UNSIGNED_AND_UNENCRYPTED_TARGET = "http://localhost:8080/saml-unsigned-and-unencrypted-assertion/";

    private static final String REALM_PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    private static final String REALM_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    private static final String ENCRYPTION_CERTIFICATE = "MIIB1DCCAT0CBgFJGVacCDANBgkqhkiG9w0BAQsFADAwMS4wLAYDVQQDEyVodHRwOi8vbG9jYWxob3N0OjgwODAvc2FsZXMtcG9zdC1lbmMvMB4XDTE0MTAxNjE0MjA0NloXDTI0MTAxNjE0MjIyNlowMDEuMCwGA1UEAxMlaHR0cDovL2xvY2FsaG9zdDo4MDgwL3NhbGVzLXBvc3QtZW5jLzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA2+5MCT5BnVN+IYnKZcH6ev1pjXGi4feE0nOycq/VJ3aeaZMi4G9AxOxCBPupErOC7Kgm/Bw5AdJyw+Q12wSRXfJ9FhqCrLXpb7YOhbVSTJ8De5O8mW35DxAlh/cxe9FXjqPb286wKTUZ3LfGYR+X235UQeCTAPS/Ufi21EXaEikCAwEAATANBgkqhkiG9w0BAQsFAAOBgQBMrfGD9QFfx5v7ld/OAto5rjkTe3R1Qei8XRXfcs83vLaqEzjEtTuLGrJEi55kXuJgBpVmQpnwCCkkjSy0JxbqLDdVi9arfWUxEGmOr01ZHycELhDNaQcFqVMPr5kRHIHgktT8hK2IgCvd3Fy9/JCgUgCPxKfhwecyEOKxUc857g==";
    private static final String ENCRYPTION_PRIVATE_KEY = "MIICXQIBAAKBgQDb7kwJPkGdU34hicplwfp6/WmNcaLh94TSc7Jyr9Undp5pkyLgb0DE7EIE+6kSs4LsqCb8HDkB0nLD5DXbBJFd8n0WGoKstelvtg6FtVJMnwN7k7yZbfkPECWH9zF70VeOo9vbzrApNRnct8ZhH5fbflRB4JMA9L9R+LbURdoSKQIDAQABAoGBANtbZG9bruoSGp2s5zhzLzd4hczT6Jfk3o9hYjzNb5Z60ymN3Z1omXtQAdEiiNHkRdNxK+EM7TcKBfmoJqcaeTkW8cksVEAW23ip8W9/XsLqmbU2mRrJiKa+KQNDSHqJi1VGyimi4DDApcaqRZcaKDFXg2KDr/Qt5JFD/o9IIIPZAkEA+ZENdBIlpbUfkJh6Ln+bUTss/FZ1FsrcPZWu13rChRMrsmXsfzu9kZUWdUeQ2Dj5AoW2Q7L/cqdGXS7Mm5XhcwJBAOGZq9axJY5YhKrsksvYRLhQbStmGu5LG75suF+rc/44sFq+aQM7+oeRr4VY88Mvz7mk4esdfnk7ae+cCazqJvMCQQCx1L1cZw3yfRSn6S6u8XjQMjWE/WpjulujeoRiwPPY9WcesOgLZZtYIH8nRL6ehEJTnMnahbLmlPFbttxPRUanAkA11MtSIVcKzkhp2KV2ipZrPJWwI18NuVJXb+3WtjypTrGWFZVNNkSjkLnHIeCYlJIGhDd8OL9zAiBXEm6kmgLNAkBWAg0tK2hCjvzsaA505gWQb4X56uKWdb0IzN+fOLB3Qt7+fLqbVQNQoNGzqey6B4MoS1fUKAStqdGTFYPG/+9t";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealmRep.setPrivateKey(REALM_PRIVATE_KEY);
        testRealmRep.setPublicKey(REALM_PUBLIC_KEY);
        testRealmRep.setAccessCodeLifespan(60); // Used as default assertion lifespan
        testRealms.add(testRealmRep);
    }

    public static void setupRealm(KeycloakSession session) {
        addTargetClients(session);
        addDirectExchanger(session);

        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel exampleRole = realm.getRole("example");

        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        RoleModel impersonateRole = management.getRealmManagementClient().getRole(ImpersonationConstants.IMPERSONATION_ROLE);

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
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(clientRep, server);
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
        Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(clientImpersonateRep, server);
        management.users().setPermissionsEnabled(true);
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        UserModel user = session.users().addUser(realm, "user");
        user.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password("password"));
        user.grantRole(exampleRole);
        user.grantRole(impersonateRole);

        UserModel bad = session.users().addUser(realm, "bad-impersonator");
        bad.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, bad, UserCredentialModel.password("password"));
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
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        Map<String, String> params = new HashMap<>();
        params.put(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE);

        {
            response = oauth.doTokenExchange(TEST, accessToken, SAML_SIGNED_TARGET, "client-exchanger", "secret", params);

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), "UTF-8");

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Verify assertion
            Element assertionElement = DocumentUtil.getDocument(assertionXML).getDocumentElement();
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString(REALM_PUBLIC_KEY)));

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
            response = oauth.doTokenExchange(TEST, accessToken, SAML_SIGNED_TARGET, "legal", "secret", params);

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), "UTF-8");

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Verify assertion
            Element assertionElement = DocumentUtil.getDocument(assertionXML).getDocumentElement();
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString(REALM_PUBLIC_KEY)));

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
            response = oauth.doTokenExchange(TEST, accessToken, SAML_SIGNED_TARGET, "illegal", "secret", params);
            Assert.assertEquals(403, response.getStatusCode());
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeToSAML2EncryptedAssertion() throws Exception {
        testingClient.server().run(ClientTokenExchangeSAML2Test::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        Map<String, String> params = new HashMap<>();
        params.put(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE);

        {
            response = oauth.doTokenExchange(TEST, accessToken, SAML_ENCRYPTED_TARGET, "client-exchanger", "secret", params);

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), "UTF-8");

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Decrypt assertion
            Document assertionDoc = DocumentUtil.getDocument(assertionXML);
            Element assertionElement = XMLEncryptionUtil.decryptElementInDocument(assertionDoc, privateKeyFromString(ENCRYPTION_PRIVATE_KEY));
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
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        Map<String, String> params = new HashMap<>();
        params.put(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE);

        {
            response = oauth.doTokenExchange(TEST, accessToken, SAML_SIGNED_AND_ENCRYPTED_TARGET, "client-exchanger", "secret", params);

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), "UTF-8");

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Verify assertion
            Document assertionDoc = DocumentUtil.getDocument(assertionXML);
            Element assertionElement = XMLEncryptionUtil.decryptElementInDocument(assertionDoc, privateKeyFromString(ENCRYPTION_PRIVATE_KEY));
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString(REALM_PUBLIC_KEY)));

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
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        Map<String, String> params = new HashMap<>();
        params.put(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE);

        {
            response = oauth.doTokenExchange(TEST, accessToken, SAML_UNSIGNED_AND_UNENCRYPTED_TARGET, "client-exchanger", "secret", params);

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), "UTF-8");

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
        oauth.clientId("client-exchanger");

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        Map<String, String> params = new HashMap<>();
        params.put(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE);

        // client-exchanger can impersonate from token "user" to user "impersonated-user" and to "target" client
        {
            params.put(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user");
            response = oauth.doTokenExchange(TEST, accessToken, SAML_SIGNED_TARGET, "client-exchanger", "secret", params);

            String exchangedTokenString = response.getAccessToken();
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), "UTF-8");

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, response.getIssuedTokenType());

            // Verify assertion
            Element assertionElement = DocumentUtil.getDocument(assertionXML).getDocumentElement();
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString(REALM_PUBLIC_KEY)));

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
        oauth.clientId("client-exchanger");

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "bad-impersonator", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "bad-impersonator");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        Map<String, String> params = new HashMap<>();
        params.put(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE);

        // test that user does not have impersonator permission
        {
            params.put(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user");
            response = oauth.doTokenExchange(TEST, accessToken, SAML_SIGNED_TARGET, "client-exchanger", "secret", params);
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
            String assertionXML = new String(Base64Url.decode(exchangedTokenString), "UTF-8");

            // Verify issued_token_type
            Assert.assertEquals(OAuth2Constants.SAML2_TOKEN_TYPE, accessTokenResponse.getOtherClaims().get(OAuth2Constants.ISSUED_TOKEN_TYPE));

            // Verify assertion
            Element assertionElement = DocumentUtil.getDocument(assertionXML).getDocumentElement();
            Assert.assertTrue(AssertionUtil.isSignedElement(assertionElement));
            AssertionType assertion = (AssertionType) SAMLParser.getInstance().parse(assertionElement);
            Assert.assertTrue(AssertionUtil.isSignatureValid(assertionElement, publicKeyFromString(REALM_PUBLIC_KEY)));

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
        Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(clientImpersonateRep, server);
        management.users().setPermissionsEnabled(true);
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        UserModel impersonatedUser = session.users().addUser(realm, "impersonated-user");
        impersonatedUser.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, impersonatedUser, UserCredentialModel.password("password"));
        impersonatedUser.grantRole(exampleRole);
    }

    private PublicKey publicKeyFromString(String publicKey) {
        return org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(publicKey);
    }

    private PrivateKey privateKeyFromString(String privateKey) {
        return org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(privateKey);
    }
}
