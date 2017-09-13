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
package org.keycloak.testsuite.migration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.component.PrioritizedComponentModel;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.DeploymentTargetModifier;
import org.keycloak.testsuite.arquillian.migration.Migration;
import org.keycloak.testsuite.runonserver.RunHelpers;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.OAuthClient;

import javax.ws.rs.NotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_LINKS;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
import static org.keycloak.testsuite.Assert.assertEquals;
import static org.keycloak.testsuite.Assert.assertFalse;
import static org.keycloak.testsuite.Assert.assertNames;
import static org.keycloak.testsuite.Assert.assertTrue;
import static org.keycloak.testsuite.Assert.fail;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class MigrationTest extends AbstractKeycloakTest {

    public static final String MIGRATION = "Migration";
    public static final String MIGRATION2 = "Migration2";

    private RealmResource migrationRealm;
    private RealmResource migrationRealm2;
    private RealmResource migrationRealm3;
    private RealmResource masterRealm;

    @Deployment
    @TargetsContainer(DeploymentTargetModifier.AUTH_SERVER_CURRENT)
    public static WebArchive deploy() {
        return RunOnServerDeployment.create();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.info("Adding no test realms for migration test. Test realm should be migrated from previous vesrion.");
    }
    @Before
    public void beforeMigrationTest() {
        migrationRealm = adminClient.realms().realm(MIGRATION);
        migrationRealm2 = adminClient.realms().realm(MIGRATION2);
        migrationRealm3 = adminClient.realms().realm("authorization");
        masterRealm = adminClient.realms().realm(MASTER);
        //add migration realms to testRealmReps to make them removed after test
        addTestRealmToTestRealmReps(migrationRealm);
        addTestRealmToTestRealmReps(migrationRealm2);
        addTestRealmToTestRealmReps(migrationRealm3);
    }
    private void addTestRealmToTestRealmReps(RealmResource realm) {
        try {
            testRealmReps.add(realm.toRepresentation());
        } catch (NotFoundException ex) {
        }
    }
    @Test
    @Migration(versionFrom = "2.5.5.Final")
    public void migration2_5_5Test() {
        testMigratedData();
        testMigrationTo3_0_0();
        testMigrationTo3_3_0();
    }
    @Test
    @Migration(versionFrom = "1.9.8.Final")
    public void migration1_9_8Test() throws Exception {
        testMigratedData();
        testMigrationTo2_0_0();
        testMigrationTo2_1_0();
        testMigrationTo2_2_0();
        testMigrationTo2_3_0();
        testMigrationTo2_5_0();
        testMigrationTo2_5_1();
        testMigrationTo3_0_0();
        testMigrationTo3_2_0();
        testMigrationTo3_3_0();
    }
    @Test
    @Migration(versionFrom = "2.2.1.Final")
    public void migrationInAuthorizationServicesTest() {
        testDroolsToRulesPolicyTypeMigration();
    }

    private void testMigratedData() {
        log.info("testing migrated data");
        //master realm
        assertNames(masterRealm.roles().list(), "offline_access", "uma_authorization", "create-realm", "master-test-realm-role", "admin");
        assertNames(masterRealm.clients().findAll(), "admin-cli", "security-admin-console", "broker", "account", 
                "master-realm", "master-test-client", "Migration-realm", "Migration2-realm");
        String id = masterRealm.clients().findByClientId("master-test-client").get(0).getId();
        assertNames(masterRealm.clients().get(id).roles().list(), "master-test-client-role");
        assertNames(masterRealm.users().search("", 0, 5), "admin", "master-test-user");
        assertNames(masterRealm.groups().groups(), "master-test-group");
        //migrationRealm
        assertNames(migrationRealm.roles().list(), "offline_access", "uma_authorization", "migration-test-realm-role");
        assertNames(migrationRealm.clients().findAll(), "account", "admin-cli", "broker", "migration-test-client", "realm-management", "security-admin-console");
        String id2 = migrationRealm.clients().findByClientId("migration-test-client").get(0).getId();
        assertNames(migrationRealm.clients().get(id2).roles().list(), "migration-test-client-role");
        assertNames(migrationRealm.users().search("", 0, 5), "migration-test-user");
        assertNames(migrationRealm.groups().groups(), "migration-test-group");
    }
    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_0_0
     */
    private void testMigrationTo2_0_0() {
        testAuthorizationServices(masterRealm, migrationRealm);
    }
    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_1_0
     */
    private void testMigrationTo2_1_0() {
        testNameOfOTPRequiredAction(masterRealm, migrationRealm);
    }
    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_2_0
     */
    private void testMigrationTo2_2_0() {
        testIdentityProviderAuthenticator(masterRealm, migrationRealm);
        //MigrateTo2_2_0#migrateRolePolicies is not relevant any more
    }
    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_3_0
     */
    private void testMigrationTo2_3_0() {
        testUpdateProtocolMappers(masterRealm, migrationRealm);
        testExtractRealmKeys(masterRealm, migrationRealm);
    }
    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_5_0
     */
    private void testMigrationTo2_5_0() {
        testLdapKerberosMigration_2_5_0();
        //https://github.com/keycloak/keycloak/pull/3630
        testDuplicateEmailSupport(masterRealm, migrationRealm);
    }

    private void testMigrationTo2_5_1() throws Exception {
        testOfflineTokenLogin();
    }
    /**
     * @see org.keycloak.migration.migrators.MigrateTo3_0_0
     */
    private void testMigrationTo3_0_0() {
        testRoleManageAccountLinks(masterRealm, migrationRealm);
    }

    private void testMigrationTo3_2_0() {
        assertNull(masterRealm.toRepresentation().getPasswordPolicy());
        assertNull(migrationRealm.toRepresentation().getPasswordPolicy());

        testDockerAuthenticationFlow(masterRealm, migrationRealm);
    }

    private void testMigrationTo3_3_0() {
        Map<String, String> securityHeaders = masterRealm.toRepresentation().getBrowserSecurityHeaders();
        if (securityHeaders != null) {
            assertEquals("frame-src 'self'; frame-ancestors 'self'; object-src 'none';",
                    securityHeaders.get("contentSecurityPolicy"));
        } else {
            fail("Browser security headers not found");
        }
    }

    private void testDockerAuthenticationFlow(RealmResource... realms) {
        for (RealmResource realm : realms) {
            AuthenticationFlowRepresentation flow = null;
            for (AuthenticationFlowRepresentation f : realm.flows().getFlows()) {
                if (DefaultAuthenticationFlows.DOCKER_AUTH.equals(f.getAlias())) {
                    flow = f;
                }
            }
            assertNotNull(flow);
        }
    }

    private void testRoleManageAccountLinks(RealmResource... realms) {
        log.info("testing role manage account links");
        for (RealmResource realm : realms) {
            List<ClientRepresentation> clients = realm.clients().findByClientId(ACCOUNT_MANAGEMENT_CLIENT_ID);
            if (!clients.isEmpty()) {
                String accountClientId = clients.get(0).getId();
                ClientResource accountClient = realm.clients().get(accountClientId);
                accountClient.roles().get(MANAGE_ACCOUNT_LINKS).toRepresentation(); //the role should be presented, it'll throw javax.ws.rs.NotFoundException in case the role is not found

                Set<RoleRepresentation> roleComposites = accountClient.roles().get(MANAGE_ACCOUNT).getRoleComposites();
                boolean success = false;
                for (RoleRepresentation roleComposite : roleComposites) {
                    if (roleComposite.getName().equals(MANAGE_ACCOUNT_LINKS)) {
                        success = true;
                    }
                }
                if (!success) {
                    fail("'manage-account' role of client 'account' should have composite role 'manage-account-links'.");
                }
            }
        }
    }
    private void testExtractRealmKeys(RealmResource masterRealm, RealmResource migrationRealm) {
        log.info("testing extract realm keys");
        String expectedMasterRealmKey = "MIIEowIBAAKCAQEAiU54OXoCbHy0L0gHn1yasctcnKHRU1pHFIJnWvaI7rClJydet9dDJaiYXOxMKseiBm3eYznfN3cPyU8udYmRnMuKjiocZ77LT2IEttAjXb6Ggazx7loriFHRy0IOJeX4KxXhAPWmxqa3mkFNfLBEvFqVaBgUDHQ60cmnPvNSHYudBTW9K80s8nvmP2pso7HTwWJ1+Xatj1Ey/gTmB3CXlyqBegGWC9TeuErEYpYhdh+11TVWasgMBZyUCtL3NRPaBuhaPg1LpW8lWGk05nS+YM6dvTk3Mppv+z2RygEpxyO09oT3b4G+Zfwit1STqn0AvDTGzINdoKcNtFScV0j8TwIDAQABAoIBAHcbPKsPLZ8SJfOF1iblW8OzFulAbaaSf2pJHIMJrQrw7LKkMkPjVXoLX+/rgr7xYZmWIP2OLBWfEHCeYTzQUyHiZpSf7vgHx7Fa45/5uVQOe/ttHIiYa37bCtP4vvEdJkOpvP7qGPvljwsebqsk9Ns28LfVez66bHOjK5Mt2yOIulbTeEs7ch//h39YwKJv96vc+CHbV2O6qoOxZessO6y+287cOBvbFXmS2GaGle5Nx/EwncBNS4b7czoetmm70+9ht3yX+kxaP311YUT31KQjuaJt275kOiKsrXr27PvgO++bsIyGuSzqyS7G7fmxF2zUyphEqEpalyDGMKMnrAECgYEA1fCgFox03rPDjm0MhW/ThoS2Ld27sbWQ6reS+PBMdUTJZVZIU1D2//h6VXDnlddhk6avKjA4smdy1aDKzmjz3pt9AKn+kgkXqtTC2fD3wp+fC9hND0z+rQPGe/Gk7ZUnTdsqnfyowxr+woIgzdnRukOUrG+xQiP3RUUT7tt6NQECgYEApEz2xvgqMm+9/f/YxjLdsFUfLqc4WlafB863stYEVqlCYy5ujyo0VQ0ahKSKJkLDnf52+aMUqPOpwaGePpu3O6VkvpcKfPY2MUlZW7/6Sa9et9hxNkdTS7Gui2d1ELpaCBe1Bc62sk8EA01iHXE1PpvyUqDWrhNh+NrDICA9oU8CgYBgGDYACtTP11TmW2r9YK5VRLUDww30k4ZlN1GnyV++aMhBYVEZQ0u+y+A/EnijIFwu0vbo70H4OGknNZMCxbeMbLDoJHM5KyZbUDe5ZvgSjloFGwH59m6KTiDQOUkIgi9mVCQ/VGaFRFHcElEjxUvj60kTbxPijn8ZuR5r8l9hAQKBgQCQ9jL5pHWeoIayN20smi6M6N2lTPbkhe60dcgQatHTIG2pkosLl8IqlHAkPgSB84AiwyR351JQKwRJCm7TcJI/dxMnMZ6YWKfB3qSP1hdfsfJRJQ/mQxIUBAYrizF3e+P5peka4aLCOgMhYsJBlePThMZN7wja99EGPwXQL4IQ8wKBgB8Nis1lQK6Z30GCp9u4dYleGfEP71Lwqvk/eJb89/uz0fjF9CTpJMULFc+nA5u4yHP3LFnRg3zCU6aEwfwUyk4GH9lWGV/qIAisQtgrCEraVe4qxz0DVE59C7qjO26IhU2U66TEzPAqvQ3zqey+woDn/cz/JMWK1vpcSk+TKn3K";
        String expectedMigrationRealmKey = "MIIEpAIBAAKCAQEApt6gCllWkVTZ7fy/oRIx6Bxjt9x3eKKyKGFXvN4iaafrNqpYU9lcqPngWJ9DyXGqUf8RpjPaQWiLWLxjw3xGBqLk2E1/Frb9e/dy8rj//fHGq6bujN1iguzyFwxPGT5Asd7jflRI3qU04M8JE52PArqPhGL2Fn+FiSK5SWRIGm+hVL7Ck/E/tVxM25sFG1/UTQqvrROm4q76TmP8FsyZaTLVf7cCwW2QPIX0N5HTVb3QbBb5KIsk4kKmk/g7uUxS9r42tu533LISzRr5CTyWZAL2XFRuF2RrKdE8gwqkEubw6sDmB2mE0EoPdY1DUhBQgVP/5rwJrCtTsUBR2xdEYQIDAQABAoIBAFbbsNBSOlZBpYJUOmcb8nBQPrOYhXN8tGGCccn0klMOvcdhmcJjdPDbyCQ5Gm7DxJUTwNsTSHsdcNMKlJ9Pk5+msJnKlOl87KrXXbTsCQvlCrWUmb0nCzz9GvJWTOHl3oT3cND0DE4gDksqWR4luCgCdevCGzgQvrBoK6wBD+r578uEW3iw10hnJ0+wnGiw8IvPzE1a9xbY4HD8/QrYdaLxuLb/aC1PDuzrz0cOjnvPkrws5JrbUSnbFygJiOv1z4l2Q00uGIxlHtXdwQBnTZZjVi4vOec2BYSHffgwDYEZIglw1mnrV7y0N1nnPbtJK/cegIkXoBQHXm8Q99TrWMUCgYEA9au86qcwrXZZg5H4BpR5cpy0MSkcKDbA1aRL1cAyTCqJxsczlAtLhFADF+NhnlXj4y7gwDEYWrz064nF73I+ZGicvCiyOy+tCTugTyTGS+XR948ElDMS6PCUUXsotS3dKa0b3c9wd2mxeddTjq/ArfgEVZJ6fE1KtjLt9dtfA+8CgYEAreK3JsvjR5b/Xct28TghYUU7Qnasombb/shqqy8FOMjYUr5OUm/OjNIgoCqhOlE8oQDJ4dOZofNSa7tL+oM8Gmbal+E3fRzxnx/9/EC4QV6sVaPLTIyk7EPfKTcZuzH7+BNZtAziTxJw9d6YJQRbkpg92EZIEoR8iDj2Xs5xrK8CgYEAwMVWwwYX8zT3vn7ukTM2LRH7bsvkVUXJgJqgCwT6Mrv6SmkK9vL5+cPS+Y6pjdW1sRGauBSOGL1Grf/4ug/6F03jFt4UJM8fRyxreU7Q7sNSQ6AMpsGA6BnHODycz7ZCYa59PErG5FyiL4of/cm5Nolz1TXQOPNpWZiTEqVlZC8CgYA4YPbjVF4nuxSnU64H/hwMjsbtAM9uhI016cN0J3W4+J3zDhMU9X1x+Tts0wWdg/N1fGz4lIQOl3cUyRCUc/KL2OdtMS+tmDHbVyMho9ZaE5kq10W2Vy+uDz+O/HeSU12QDK4cC8Vgv+jyPy7zaZtLR6NduUPrBRvfiyCOkr8WrwKBgQCY0h4RCdNFhr0KKLLmJipAtV8wBCGcg1jY1KoWKQswbcykfBKwHbF6EooVqkRW0ITjWB7ZZCf8TnSUxe0NXCUAkVBrhzS4DScgtoSZYOOUaSHgOxpfwgnQ3oYotKi98Yg3IsaLs1j4RuPG5Sp1z6o+ELP1uvr8azyn9YlLa+523Q==";

        List<ComponentRepresentation> components = masterRealm.components().query(MASTER, KeyProvider.class.getName());
        assertEquals(2, components.size());

        components = masterRealm.components().query(MASTER, KeyProvider.class.getName(), "rsa");
        assertEquals(1, components.size());

        ComponentRepresentation component = testingClient.server(MASTER).fetch(RunHelpers.internalComponent(components.get(0).getId()));
        assertEquals(expectedMasterRealmKey, component.getConfig().getFirst("privateKey"));

        components = masterRealm.components().query(MASTER, KeyProvider.class.getName(), "hmac-generated");
        assertEquals(1, components.size());

        components = migrationRealm.components().query(MIGRATION, KeyProvider.class.getName());
        assertEquals(2, components.size());

        components = migrationRealm.components().query(MIGRATION, KeyProvider.class.getName(), "rsa");
        assertEquals(1, components.size());

        component = testingClient.server(MIGRATION).fetch(RunHelpers.internalComponent(components.get(0).getId()));
        assertEquals(expectedMigrationRealmKey, component.getConfig().getFirst("privateKey"));

        components = migrationRealm.components().query(MIGRATION, KeyProvider.class.getName(), "hmac-generated");
        assertEquals(1, components.size());
    }

    private void testLdapKerberosMigration_2_5_0() {
        log.info("testing ldap kerberos migration");
        RealmRepresentation realmRep = migrationRealm2.toRepresentation();
        List<ComponentRepresentation> components = migrationRealm2.components().query(realmRep.getId(), UserStorageProvider.class.getName());
        assertEquals(2, components.size());
        boolean testedLdap = false;
        boolean testedKerberos = false;

        for (ComponentRepresentation component : components) {
            if (component.getName().equals("ldap-provider")) {
                assertEquals("2", component.getConfig().getFirst(PrioritizedComponentModel.PRIORITY));
                assertEquals("READ_ONLY", component.getConfig().getFirst(LDAPConstants.EDIT_MODE));
                assertEquals("true", component.getConfig().getFirst(LDAPConstants.SYNC_REGISTRATIONS));
                assertEquals(LDAPConstants.VENDOR_RHDS, component.getConfig().getFirst(LDAPConstants.VENDOR));
                assertEquals("uid", component.getConfig().getFirst(LDAPConstants.USERNAME_LDAP_ATTRIBUTE));
                assertEquals("uid", component.getConfig().getFirst(LDAPConstants.RDN_LDAP_ATTRIBUTE));
                assertEquals("nsuniqueid", component.getConfig().getFirst(LDAPConstants.UUID_LDAP_ATTRIBUTE));
                assertEquals("inetOrgPerson, organizationalPerson", component.getConfig().getFirst(LDAPConstants.USER_OBJECT_CLASSES));
                assertEquals("http://localhost", component.getConfig().getFirst(LDAPConstants.CONNECTION_URL));
                assertEquals("dn", component.getConfig().getFirst(LDAPConstants.USERS_DN));
                assertEquals(LDAPConstants.AUTH_TYPE_NONE, component.getConfig().getFirst(LDAPConstants.AUTH_TYPE));
                assertEquals("true", component.getConfig().getFirst(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION));
                assertEquals("realm", component.getConfig().getFirst(KerberosConstants.KERBEROS_REALM));
                assertEquals("principal", component.getConfig().getFirst(KerberosConstants.SERVER_PRINCIPAL));
                assertEquals("keytab", component.getConfig().getFirst(KerberosConstants.KEYTAB));
                testedLdap = true;
            } else if (component.getName().equals("kerberos-provider")) {
                assertEquals("3", component.getConfig().getFirst(PrioritizedComponentModel.PRIORITY));
                assertEquals("realm", component.getConfig().getFirst(KerberosConstants.KERBEROS_REALM));
                assertEquals("principal", component.getConfig().getFirst(KerberosConstants.SERVER_PRINCIPAL));
                assertEquals("keytab", component.getConfig().getFirst(KerberosConstants.KEYTAB));
            }
        }
    }

    private void testDroolsToRulesPolicyTypeMigration() {
        log.info("testing drools to rules in authorization services");
        List<ClientRepresentation> client = migrationRealm3.clients().findByClientId("photoz-restful-api");

        assertEquals(1, client.size());

        ClientRepresentation representation = client.get(0);

        List<PolicyRepresentation> policies = migrationRealm3.clients().get(representation.getId()).authorization().policies().policies();

        List<PolicyRepresentation> migratedRulesPolicies = policies.stream().filter(policyRepresentation -> "rules".equals(policyRepresentation.getType())).collect(Collectors.toList());

        assertEquals(1, migratedRulesPolicies.size());
    }
    private void testAuthorizationServices(RealmResource... realms) {
        log.info("testing authorization services");
        for (RealmResource realm : realms) {
            //test setup of authorization services
            for (String roleName : Constants.AUTHZ_DEFAULT_AUTHORIZATION_ROLES) {
                RoleResource role = realm.roles().get(roleName); //throws javax.ws.rs.NotFoundException if not found

                assertFalse("Role's scopeParamRequired should be false.", role.toRepresentation().isScopeParamRequired());
                assertFalse("Role shouldn't be composite should be false.", role.toRepresentation().isComposite());

                assertTrue("role should be added to default roles for new users", realm.toRepresentation().getDefaultRoles().contains(roleName));
            }
            //test admin roles - master admin client
            List<ClientRepresentation> clients = realm.clients().findByClientId(realm.toRepresentation().getRealm() + "-realm");
            if (!clients.isEmpty()) {
                ClientResource masterAdminClient = realm.clients().get(clients.get(0).getId());
                masterAdminClient.roles().get(AdminRoles.VIEW_AUTHORIZATION).toRepresentation();
                masterAdminClient.roles().get(AdminRoles.MANAGE_AUTHORIZATION).toRepresentation();
                //test admin roles - admin role composite
                Set<String> roleNames = new HashSet<>();
                for (RoleRepresentation role : realm.roles().get(AdminRoles.ADMIN).getRoleComposites()) {
                    roleNames.add(role.getName());
                }
                assertTrue(AdminRoles.VIEW_AUTHORIZATION + " should be composite role of " + AdminRoles.ADMIN, roleNames.contains(AdminRoles.VIEW_AUTHORIZATION));
                assertTrue(AdminRoles.MANAGE_AUTHORIZATION + " should be composite role of " + AdminRoles.ADMIN, roleNames.contains(AdminRoles.MANAGE_AUTHORIZATION));
            }
        }
    }
    private void testNameOfOTPRequiredAction(RealmResource... realms) {
        log.info("testing OTP Required Action");
        for (RealmResource realm : realms) {
            RequiredActionProviderRepresentation otpAction = realm.flows().getRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP.name());

            assertEquals("The name of CONFIGURE_TOTP required action should be 'Configure OTP'.", "Configure OTP", otpAction.getName());
        }
    }
    private void testIdentityProviderAuthenticator(RealmResource... realms) {
        log.info("testing identity provider authenticator");
        for (RealmResource realm : realms) {
            boolean success = false;
            for (AuthenticationFlowRepresentation flow : realm.flows().getFlows()) {
                if (flow.getAlias().equals(DefaultAuthenticationFlows.BROWSER_FLOW)) {
                    for (AuthenticationExecutionExportRepresentation execution : flow.getAuthenticationExecutions()) {
                        if ("identity-provider-redirector".equals(execution.getAuthenticator())) {
                            assertEquals("Requirement should be ALTERNATIVE.", AuthenticationExecutionModel.Requirement.ALTERNATIVE.name(), execution.getRequirement());
                            assertTrue("Priority should be 25.", execution.getPriority() == 25);
                            success = true;
                        }
                    }
                }
            } 
            if (!success) {
                fail("BROWSER_FLOW should contain execution: 'identity-provider-redirector' authenticator.");
            }
        }
    }
    private void testUpdateProtocolMappers(RealmResource... realms) {
        log.info("testing updated protocol mappers");
        for (RealmResource realm : realms) {
            for (ClientRepresentation client : realm.clients().findAll()) {
                for (ProtocolMapperRepresentation protocolMapper : client.getProtocolMappers()) {
                    testUpdateProtocolMapper(protocolMapper);
                }
            }
            for (ClientTemplateRepresentation clientTemlate : realm.clientTemplates().findAll()) {
                for (ProtocolMapperRepresentation protocolMapper : clientTemlate.getProtocolMappers()) {
                    testUpdateProtocolMapper(protocolMapper);
                }
            }
        }
    }
    private void testUpdateProtocolMapper(ProtocolMapperRepresentation protocolMapper) {
        if (protocolMapper.getConfig().get("id.token.claim") != null) {
            assertEquals("ProtocolMapper's config should contain key 'userinfo.token.claim'.", 
                    protocolMapper.getConfig().get("id.token.claim"), protocolMapper.getConfig().get("userinfo.token.claim"));
        }
    }
    private void testDuplicateEmailSupport(RealmResource... realms) {
        log.info("testing duplicate email");
        for (RealmResource realm : realms) {
            RealmRepresentation rep = realm.toRepresentation();
            assertTrue("LoginWithEmailAllowed should be enabled.", rep.isLoginWithEmailAllowed());
            assertFalse("DuplicateEmailsAllowed should be disabled.", rep.isDuplicateEmailsAllowed());
        }
    }

    private void testOfflineTokenLogin() throws Exception {
        if (isImportMigrationMode()) {
            log.info("Skip offline token login test in the 'import' migrationMode");
        } else {
            log.info("test login with old offline token");
            String oldOfflineToken = suiteContext.getMigrationContext().loadOfflineToken();
            Assert.assertNotNull(oldOfflineToken);

            oauth.realm(MIGRATION);
            oauth.clientId("migration-test-client");
            OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(oldOfflineToken, "b2c07929-69e3-44c6-8d7f-76939000b3e4");
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            assertEquals("migration-test-user", accessToken.getPreferredUsername());
        }
    }

    private String getMigrationMode() {
        return System.getProperty("migration.mode");
    }

    private boolean isImportMigrationMode() {
        String mode = getMigrationMode();
        return "import".equals(mode);
    }
}
