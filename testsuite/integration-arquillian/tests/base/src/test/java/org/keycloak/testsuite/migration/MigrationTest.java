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

import java.util.HashSet;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.migration.Migration;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
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
    private RealmResource migrationRealm;
    private RealmResource masterRealm;
        
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.info("Adding no test realms for migration test. Test realm should be migrated from previous vesrion.");
    }
    
    @Before
    public void beforeMigrationTest() {
        migrationRealm = adminClient.realms().realm(MIGRATION);
        masterRealm = adminClient.realms().realm(MASTER);
        
        //add migration realm to testRealmReps to make the migration removed after test
        testRealmReps.add(adminClient.realms().realm(MIGRATION).toRepresentation());
    }
    
    @Test
    @Migration(versionFrom = "1.9.8.Final")
    public void migration1_9_8Test() {
        testMigratedData();
        testMigrationTo2_0_0();
        testMigrationTo2_1_0();
        testMigrationTo2_2_0();
        testMigrationTo2_3_0();
        testMigrationTo2_5_0();
    }
    
    @Test
    @Migration(versionFrom = "2.2.1.Final")
    public void migration2_2_1Test() {
        testMigratedData();
        testMigrationTo2_3_0();
        testMigrationTo2_5_0();
    }
    
    private void testMigratedData() {
        //master realm
        assertNames(masterRealm.roles().list(), "offline_access", "uma_authorization", "create-realm", "master-test-realm-role", "admin");
        assertNames(masterRealm.clients().findAll(), "admin-cli", "security-admin-console", "broker", "account", 
                "master-realm", "master-test-client", "Migration-realm");
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
    }
    
    private void testMigrationTo2_5_0() {
        //TODO org.keycloak.migration.migrators.MigrateTo2_5_0
        
        //https://github.com/keycloak/keycloak/pull/3630
        testDuplicateEmailSupport(masterRealm, migrationRealm);
    }
    
    private void testAuthorizationServices(RealmResource... realms) {
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
        for (RealmResource realm : realms) {
            RequiredActionProviderRepresentation otpAction = realm.flows().getRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP.name());

            assertEquals("The name of CONFIGURE_TOTP required action should be 'Configure OTP'.", "Configure OTP", otpAction.getName());
        }
    }
    
    private void testIdentityProviderAuthenticator(RealmResource... realms) {
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
        for (RealmResource realm : realms) {
            RealmRepresentation rep = realm.toRepresentation();
            assertTrue("LoginWithEmailAllowed should be enabled.", rep.isLoginWithEmailAllowed());
            assertFalse("DuplicateEmailsAllowed should be disabled.", rep.isDuplicateEmailsAllowed());
        }
    }
}
