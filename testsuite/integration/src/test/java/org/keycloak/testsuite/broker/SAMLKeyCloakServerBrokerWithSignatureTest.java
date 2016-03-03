/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.processing.web.util.PostBindingUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author pedroigor
 */
public class SAMLKeyCloakServerBrokerWithSignatureTest extends AbstractKeycloakIdentityProviderTest {

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(8082);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-saml-with-signature.json"));
        }

        @Override
        protected String[] getTestRealms() {
            return new String[] { "realm-with-saml-signed-idp" };
        }
    };

    // @Test
    public void testSleep() throws Exception {
        Thread.sleep(100000000);
    }

    @Override
    protected String getProviderId() {
        return "kc-saml-signed-idp";
    }

    @Override
    protected void doAssertFederatedUser(UserModel federatedUser, IdentityProviderModel identityProviderModel, String expectedEmail, boolean isProfileUpdateExpected) {
        if (isProfileUpdateExpected) {
            super.doAssertFederatedUser(federatedUser, identityProviderModel, expectedEmail, isProfileUpdateExpected);
        } else {
            if (expectedEmail == null) {
                // Need to handle differences for various databases (like Oracle)
                assertTrue(federatedUser.getEmail() == null || federatedUser.getEmail().equals(""));
            } else {
                assertEquals(expectedEmail, federatedUser.getEmail());
            }
            assertNull(federatedUser.getFirstName());
            assertNull(federatedUser.getLastName());
        }
    }

    @Override
    protected void doAssertFederatedUserNoEmail(UserModel federatedUser) {
        assertEquals("kc-saml-signed-idp.test-user-noemail", federatedUser.getUsername());
        //assertEquals("", federatedUser.getEmail());
        assertEquals(null, federatedUser.getFirstName());
        assertEquals(null, federatedUser.getLastName());
    }

    @Override
    protected void doAssertTokenRetrieval(String pageSource) {
        try {
            SAML2Request saml2Request = new SAML2Request();
            ResponseType responseType = (ResponseType) saml2Request
                        .getSAML2ObjectFromStream(PostBindingUtil.base64DecodeAsStream(pageSource));

            assertNotNull(responseType);
            assertFalse(responseType.getAssertions().isEmpty());
        } catch (Exception e) {
            fail("Could not parse token.");
        }
    }

    @Override
    @Test
    public void testSuccessfulAuthentication() {
        super.testSuccessfulAuthentication();
    }

    @Test
    public void testTokenStorageAndRetrievalByApplication() {
        super.testTokenStorageAndRetrievalByApplication();
    }

    @Test
    public void testAccountManagementLinkIdentity() {
        super.testAccountManagementLinkIdentity();
    }

    @Test
    public void testSuccessfulAuthenticationUpdateProfileOnMissing_nothingMissing() {
        // skip this test as this provider do not return name and surname so something is missing always
    }
}
