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

package org.keycloak.testsuite.admin;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.services.managers.LDAPConnectionTestManager;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.keycloak.testsuite.util.LDAPRule;

import javax.ws.rs.core.Response;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableVault
@AuthServerContainerExclude(AuthServer.REMOTE)
public class UserFederationLdapConnectionTest extends AbstractAdminTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Test
    public void testLdapConnections1() {
        // Unknown action
        Response response = realm.testLDAPConnection("unknown", "ldap://localhost:10389", "foo", "bar", "false", null);
        assertStatus(response, 400);

        // Bad host
        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_CONNECTION, "ldap://localhostt:10389", "foo", "bar", "false", null);
        assertStatus(response, 400);

        // Connection success
        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_CONNECTION, "ldap://localhost:10389", "foo", "bar", "false", null);
        assertStatus(response, 204);

        // Bad authentication
        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_AUTHENTICATION, "ldap://localhost:10389", "foo", "bar", "false", "10000");
        assertStatus(response, 400);

        // Authentication success
        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_AUTHENTICATION, "ldap://localhost:10389", "uid=admin,ou=system", "secret", "false", null);
        assertStatus(response, 204);

        // Authentication success with bindCredential from Vault
        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_AUTHENTICATION, "ldap://localhost:10389", "uid=admin,ou=system", "${vault.ldap_bindCredential}", "false", null);
        assertStatus(response, 204);
    }

    @Test
    public void testLdapConnectionsSsl() {

        Response response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_CONNECTION, "ldaps://localhost:10636", "foo", "bar", "false", null);
        assertStatus(response, 204);

        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_CONNECTION, "ldaps://localhostt:10636", "foo", "bar", "false", null);
        assertStatus(response, 400);

        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_AUTHENTICATION, "ldaps://localhost:10636", "foo", "bar", "false", null);
        assertStatus(response, 400);

        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_AUTHENTICATION, "ldaps://localhost:10636", "uid=admin,ou=system", "secret", "true", null);
        assertStatus(response, 204);

        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_AUTHENTICATION, "ldaps://localhost:10636", "uid=admin,ou=system", "secret", "true", "10000");
        assertStatus(response, 204);

        // Authentication success with bindCredential from Vault
        response = realm.testLDAPConnection(LDAPConnectionTestManager.TEST_AUTHENTICATION, "ldaps://localhost:10636", "uid=admin,ou=system", "${vault.ldap_bindCredential}", "true", null);
        assertStatus(response, 204);
    }

    private void assertStatus(Response response, int status) {
        Assert.assertEquals(status, response.getStatus());
        response.close();
    }
}
