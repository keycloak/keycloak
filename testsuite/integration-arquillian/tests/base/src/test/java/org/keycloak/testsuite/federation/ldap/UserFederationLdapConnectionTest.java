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

package org.keycloak.testsuite.federation.ldap;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation;
import org.keycloak.representations.idm.TestLdapConnectionRepresentation;
import org.keycloak.services.managers.LDAPServerCapabilitiesManager;
import org.keycloak.storage.ldap.idm.store.ldap.extended.PasswordModifyRequest;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.keycloak.testsuite.util.LDAPRule;

import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableVault
public class UserFederationLdapConnectionTest extends AbstractAdminTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Test
    public void testLdapConnections() {
        // Unknown action
        Response response = realm.testLDAPConnection(new TestLdapConnectionRepresentation("unknown", "ldap://localhost:10389", "foo", "bar", "false", null));
        assertStatus(response, 400);

        // Bad host
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_CONNECTION, "ldap://localhostt:10389", "foo", "bar", "false", null));
        assertStatus(response, 400);

        // Connection success
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_CONNECTION, "ldap://localhost:10389", null, null, "false", null, "false", LDAPConstants.AUTH_TYPE_NONE));
        assertStatus(response, 204);

        // Connection success with invalid credentials
        String ldapModelId = testingClient.testing().ldap(REALM_NAME).createLDAPProvider(ldapRule.getConfig(), false);
        getCleanup().addCleanup(() -> {
            adminClient.realm(REALM_NAME).components().removeComponent(ldapModelId);;
        });
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_CONNECTION, "ldap://localhost:10389", "invalid-db", ComponentRepresentation.SECRET_VALUE, "false", null, "false", LDAPConstants.AUTH_TYPE_SIMPLE, ldapModelId));
        assertStatus(response, 204);

        // Bad authentication
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhost:10389", "foo", "bar", "false", "10000"));
        assertStatus(response, 400);

        // Authentication success
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhost:10389", "uid=admin,ou=system", "secret", "false", null));
        assertStatus(response, 204);

        // Authentication success with bindCredential from Vault
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhost:10389", "uid=admin,ou=system", "${vault.ldap_bindCredential}", "false", null));
        assertStatus(response, 204);

        // Authentication error for anonymous bind (default ldap rule does not allow anonymous binings)
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhost:10389", null, null, "false", null, "false", LDAPConstants.AUTH_TYPE_NONE));
        assertStatus(response, 400);

        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhost:10389", "uid=admin,ou=system", "${vault.ldap_bindCredential}", "false", null));
        assertStatus(response, 204);

        // Deprecated form based
        response = realm.testLDAPConnection(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhost:10389", "uid=admin,ou=system", "${vault.ldap_bindCredential}", "false", null);
        assertStatus(response, 204);

    }

    @Test
    public void testLdapConnectionsSsl() {

        Response response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_CONNECTION, "ldaps://localhost:10636", null, null, "false", null, null, LDAPConstants.AUTH_TYPE_NONE));
        assertStatus(response, 204);

        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_CONNECTION, "ldaps://localhostt:10636", "foo", "bar", "false", null));
        assertStatus(response, 400);

        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_CONNECTION, "ldaps://localhost:10389", null, null, "false", null));
        assertStatus(response, 400);

        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_CONNECTION, "ldaps://localhost:10389", null, null, "false", "5000"));
        assertStatus(response, 400);

        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_CONNECTION, "ldaps://localhost:10389", null, null, "false", "0"));
        assertStatus(response, 400);

        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldaps://localhost:10636", "foo", "bar", "false", null));
        assertStatus(response, 400);

        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldaps://localhost:10636", "uid=admin,ou=system", "secret", "true", null));
        assertStatus(response, 204);

        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldaps://localhost:10636", "uid=admin,ou=system", "secret", "true", "10000"));
        assertStatus(response, 204);

        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldaps://localhost:10636", null, null, "false", null, "false", LDAPConstants.AUTH_TYPE_NONE));
        assertStatus(response, 400);

        // Authentication success with bindCredential from Vault
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldaps://localhost:10636", "uid=admin,ou=system", "${vault.ldap_bindCredential}", "true", null));
        assertStatus(response, 204);
    }

    @Test
    public void testLdapConnectionMoreServers() {

        // Both servers work
        Response response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhost:10389 ldaps://localhost:10636", "uid=admin,ou=system", "secret", "true", null));
        assertStatus(response, 204);

        // Only 1st server works
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhost:10389 ldap://localhostt:10389", "uid=admin,ou=system", "secret", "true", null));
        assertStatus(response, 204);

        // Only 1st server works - variant with connectionTimeout (important to test as com.sun.jndi.ldap.Connection.createSocket implementation differs based on whether connectionTimeout is used)
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhost:10389 ldap://localhostt:10389", "uid=admin,ou=system", "secret", "true", "10000"));
        assertStatus(response, 204);

        // Only 2nd server works
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhostt:10389 ldaps://localhost:10636", "uid=admin,ou=system", "secret", "true", null));
        assertStatus(response, 204);

        // Only 2nd server works - variant with connectionTimeout
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhostt:10389 ldaps://localhost:10636", "uid=admin,ou=system", "secret", "true", "10000"));
        assertStatus(response, 204);

        // None of servers work
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION, "ldap://localhostt:10389 ldaps://localhostt:10636", "uid=admin,ou=system", "secret", "true", null));
        assertStatus(response, 400);

        // create LDAP component model using ldap
        Map<String, String> cfg = ldapRule.getConfig();
        cfg.put(LDAPConstants.CONNECTION_URL, "ldap://invalid:10389 ldap://localhost:10389");
        cfg.put(LDAPConstants.CONNECTION_TIMEOUT, "1000");
        String ldapModelId = testingClient.testing().ldap(REALM_NAME).createLDAPProvider(cfg, false);

        // Only 2nd server works with stored LDAP federation provider
        response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION,
                cfg.get(LDAPConstants.CONNECTION_URL), cfg.get(LDAPConstants.BIND_DN), ComponentRepresentation.SECRET_VALUE,
                cfg.get(LDAPConstants.USE_TRUSTSTORE_SPI), cfg.get(LDAPConstants.CONNECTION_TIMEOUT),cfg.get(LDAPConstants.START_TLS),
                cfg.get(LDAPConstants.AUTH_TYPE), ldapModelId));
        assertStatus(response, 204);
    }

    @Test
    public void testLdapConnectionComponentAlreadyCreated() {
        // create ldap component model using ldaps
        Map<String, String> cfg = ldapRule.getConfig();
        cfg.put(LDAPConstants.CONNECTION_URL, "ldaps://localhost:10636");
        cfg.put(LDAPConstants.START_TLS, "false");
        cfg.put(LDAPConstants.USE_TRUSTSTORE_SPI, "true");
        String ldapModelId = testingClient.testing().ldap(REALM_NAME).createLDAPProvider(cfg, false);
        try {
            // test passing everything with password included
            Response response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION,
                    cfg.get(LDAPConstants.CONNECTION_URL), cfg.get(LDAPConstants.BIND_DN), cfg.get(LDAPConstants.BIND_CREDENTIAL),
                    cfg.get(LDAPConstants.USE_TRUSTSTORE_SPI), cfg.get(LDAPConstants.CONNECTION_TIMEOUT),
                    cfg.get(LDAPConstants.START_TLS), cfg.get(LDAPConstants.AUTH_TYPE), ldapModelId));
            assertStatus(response, 204);

            // test passing the secret but not changing anything
            response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION,
                    cfg.get(LDAPConstants.CONNECTION_URL), cfg.get(LDAPConstants.BIND_DN), ComponentRepresentation.SECRET_VALUE,
                    cfg.get(LDAPConstants.USE_TRUSTSTORE_SPI), cfg.get(LDAPConstants.CONNECTION_TIMEOUT),
                    cfg.get(LDAPConstants.START_TLS), cfg.get(LDAPConstants.AUTH_TYPE), ldapModelId));
            assertStatus(response, 204);

            // test passing the secret and changing the connection timeout which is allowed
            response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION,
                    cfg.get(LDAPConstants.CONNECTION_URL), cfg.get(LDAPConstants.BIND_DN), ComponentRepresentation.SECRET_VALUE,
                    cfg.get(LDAPConstants.USE_TRUSTSTORE_SPI), "1000",
                    cfg.get(LDAPConstants.START_TLS), cfg.get(LDAPConstants.AUTH_TYPE), ldapModelId));
            assertStatus(response, 204);

            // test passing the secret but modifying the connection URL to plain ldap (different URL)
            response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION,
                    "ldap://localhost:10389", cfg.get(LDAPConstants.BIND_DN), ComponentRepresentation.SECRET_VALUE,
                    cfg.get(LDAPConstants.USE_TRUSTSTORE_SPI), cfg.get(LDAPConstants.CONNECTION_TIMEOUT),
                    cfg.get(LDAPConstants.START_TLS), cfg.get(LDAPConstants.AUTH_TYPE), ldapModelId));
            assertStatus(response, 400);

            // test passing the secret but modifying the user DN
            response = realm.testLDAPConnection(new TestLdapConnectionRepresentation(LDAPServerCapabilitiesManager.TEST_AUTHENTICATION,
                    cfg.get(LDAPConstants.CONNECTION_URL), "uid=anotheradmin,ou=people,dc=keycloak,dc=org", ComponentRepresentation.SECRET_VALUE,
                    cfg.get(LDAPConstants.USE_TRUSTSTORE_SPI), cfg.get(LDAPConstants.CONNECTION_TIMEOUT),
                    cfg.get(LDAPConstants.START_TLS), cfg.get(LDAPConstants.AUTH_TYPE), ldapModelId));
            assertStatus(response, 400);
        } finally {
            adminClient.realm(REALM_NAME).components().removeComponent(ldapModelId);
        }
    }

    @Test
    public void testLdapCapabilities() {

        // Query the rootDSE success
        TestLdapConnectionRepresentation config = new TestLdapConnectionRepresentation(
            LDAPServerCapabilitiesManager.QUERY_SERVER_CAPABILITIES, "ldap://localhost:10389", "uid=admin,ou=system", "secret",
            "false", null, "false", LDAPConstants.AUTH_TYPE_SIMPLE);

        List<LDAPCapabilityRepresentation> ldapCapabilities = realm.ldapServerCapabilities(config);
        assertThat(ldapCapabilities, Matchers.hasItem(new LDAPCapabilityRepresentation(PasswordModifyRequest.PASSWORD_MODIFY_OID, LDAPCapabilityRepresentation.CapabilityType.EXTENSION)));

        // Query the rootDSE failure
        try {
            config = new TestLdapConnectionRepresentation(
                    LDAPServerCapabilitiesManager.QUERY_SERVER_CAPABILITIES, "ldap://localhost:10389", "foo", "bar",
                    "false", null, "false", LDAPConstants.AUTH_TYPE_SIMPLE);
            realm.ldapServerCapabilities(config);

            Assert.fail("It wasn't expected to successfully sent the request for query capabilities");
        } catch (BadRequestException bre) {
            // Expected
        }
    }

    private void assertStatus(Response response, int status) {
        Assert.assertEquals(status, response.getStatus());
        response.close();
    }
}
