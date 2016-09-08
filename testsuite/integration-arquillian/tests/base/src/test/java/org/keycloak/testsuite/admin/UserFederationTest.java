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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.admin.client.resource.UserFederationProvidersResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.UserFederationProviderFactoryRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserFederationSyncResultRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.authentication.AbstractAuthenticationTest;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.UserFederationProviderBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationTest extends AbstractAdminTest {

    @Test
    public void testProviderFactories() {
        List<UserFederationProviderFactoryRepresentation> providerFactories = userFederation().getProviderFactories();
        Assert.assertNames(providerFactories, "ldap", "kerberos", "dummy", "dummy-configurable");

        // Builtin provider without properties
        UserFederationProviderFactoryRepresentation ldapProvider = userFederation().getProviderFactory("ldap");
        Assert.assertEquals(ldapProvider.getId(), "ldap");
        Assert.assertEquals(0, ldapProvider.getOptions().size());

        // Configurable through the "old-way" options
        UserFederationProviderFactoryRepresentation dummyProvider = userFederation().getProviderFactory("dummy");
        Assert.assertEquals(dummyProvider.getId(), "dummy");
        Assert.assertNames(new LinkedList<>(dummyProvider.getOptions()), "important.config");

        // Configurable through the "new-way" ConfiguredProvider
        UserFederationProviderFactoryRepresentation dummyConfiguredProvider = userFederation().getProviderFactory("dummy-configurable");
        Assert.assertEquals(dummyConfiguredProvider.getId(), "dummy-configurable");
        Assert.assertTrue(dummyConfiguredProvider.getOptions() == null || dummyConfiguredProvider.getOptions().isEmpty());
        Assert.assertEquals("Dummy User Federation Provider Help Text", dummyConfiguredProvider.getHelpText());
        Assert.assertEquals(2, dummyConfiguredProvider.getProperties().size());
        Assert.assertProviderConfigProperty(dummyConfiguredProvider.getProperties().get(0), "prop1", "Prop1", "prop1Default", "Prop1 HelpText", ProviderConfigProperty.STRING_TYPE);
        Assert.assertProviderConfigProperty(dummyConfiguredProvider.getProperties().get(1), "prop2", "Prop2", "true", "Prop2 HelpText", ProviderConfigProperty.BOOLEAN_TYPE);

        try {
            userFederation().getProviderFactory("not-existent");
            Assert.fail("Not expected to find not-existent provider");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

    private UserFederationProvidersResource userFederation() {
        return realm.userFederation();
    }


    @Test
    public void testCreateProvider() {
        // create provider without configuration and displayName
        UserFederationProviderRepresentation dummyRep1 = UserFederationProviderBuilder.create()
                .providerName("dummy")
                .displayName("")
                .priority(2)
                .fullSyncPeriod(1000)
                .changedSyncPeriod(500)
                .lastSync(123)
                .build();

        String id1 = createUserFederationProvider(dummyRep1);

        // create provider with configuration and displayName
        UserFederationProviderRepresentation dummyRep2 = UserFederationProviderBuilder.create()
                .providerName("dummy")
                .displayName("dn1")
                .priority(1)
                .configProperty("prop1", "prop1Val")
                .configProperty("prop2", "true")
                .build();
        String id2 = createUserFederationProvider(dummyRep2);

        // Assert provider instances available
        assertFederationProvider(userFederation().get(id1).toRepresentation(), id1, id1, "dummy", 2, 1000, 500, 123);
        assertFederationProvider(userFederation().get(id2).toRepresentation(), id2, "dn1", "dummy", 1, -1, -1, -1, "prop1", "prop1Val", "prop2", "true");

        // Assert sorted
        List<UserFederationProviderRepresentation> providerInstances = userFederation().getProviderInstances();
        Assert.assertEquals(providerInstances.size(), 2);
        assertFederationProvider(providerInstances.get(0), id2, "dn1", "dummy", 1, -1, -1, -1, "prop1", "prop1Val", "prop2", "true");
        assertFederationProvider(providerInstances.get(1), id1, id1, "dummy", 2, 1000, 500, 123);

        // Remove providers
        removeUserFederationProvider(id1);
        removeUserFederationProvider(id2);
    }


    @Test
    public void testValidateAndCreateLdapProvider() {
        // Invalid filter
        UserFederationProviderRepresentation ldapRep = UserFederationProviderBuilder.create()
                .displayName("ldap1")
                .providerName("ldap")
                .priority(1)
                .configProperty(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "dc=something")
                .build();
        Response resp = userFederation().create(ldapRep);
        Assert.assertEquals(400, resp.getStatus());
        resp.close();

        // Invalid filter
        ldapRep.getConfig().put(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something");
        resp = userFederation().create(ldapRep);
        Assert.assertEquals(400, resp.getStatus());
        resp.close();

        // Invalid filter
        ldapRep.getConfig().put(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "dc=something)");
        resp = userFederation().create(ldapRep);
        Assert.assertEquals(400, resp.getStatus());
        resp.close();

        // Assert nothing created so far
        Assert.assertTrue(userFederation().getProviderInstances().isEmpty());
        assertAdminEvents.assertEmpty();


        // Valid filter. Creation success
        ldapRep.getConfig().put(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something)");
        String id1 = createUserFederationProvider(ldapRep);

        // Missing filter is ok too. Creation success
        UserFederationProviderRepresentation ldapRep2 = UserFederationProviderBuilder.create()
                .displayName("ldap2")
                .providerName("ldap")
                .priority(2)
                .configProperty(LDAPConstants.BIND_DN, "cn=manager")
                .configProperty(LDAPConstants.BIND_CREDENTIAL, "password")
                .build();
        String id2 = createUserFederationProvider(ldapRep2);

        // Assert both providers created
        List<UserFederationProviderRepresentation> providerInstances = userFederation().getProviderInstances();
        Assert.assertEquals(providerInstances.size(), 2);
        assertFederationProvider(providerInstances.get(0), id1, "ldap1", "ldap", 1, -1, -1, -1, LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something)");
        assertFederationProvider(providerInstances.get(1), id2, "ldap2", "ldap", 2, -1, -1, -1, LDAPConstants.BIND_DN, "cn=manager", LDAPConstants.BIND_CREDENTIAL, "password");

        // Cleanup
        removeUserFederationProvider(id1);
        removeUserFederationProvider(id2);
    }


    @Test
    public void testUpdateProvider() {
        UserFederationProviderRepresentation ldapRep = UserFederationProviderBuilder.create()
                .providerName("ldap")
                .priority(2)
                .configProperty(LDAPConstants.BIND_DN, "cn=manager")
                .configProperty(LDAPConstants.BIND_CREDENTIAL, "password")
                .build();
        String id = createUserFederationProvider(ldapRep);
        assertFederationProvider(userFederation().get(id).toRepresentation(), id, id, "ldap", 2, -1, -1, -1, LDAPConstants.BIND_DN, "cn=manager", LDAPConstants.BIND_CREDENTIAL, "password");

        // Assert update with invalid filter should fail
        ldapRep = userFederation().get(id).toRepresentation();
        ldapRep.setDisplayName("");
        ldapRep.getConfig().put(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2");
        ldapRep.getConfig().put(LDAPConstants.BIND_DN, "cn=manager-updated");
        try {
            userFederation().get(id).update(ldapRep);
            Assert.fail("Not expected to successfull update");
        } catch (BadRequestException bre) {
            // Expected
        }

        // Assert nothing was updated
        assertFederationProvider(userFederation().get(id).toRepresentation(), id, id, "ldap", 2, -1, -1, -1, LDAPConstants.BIND_DN, "cn=manager", LDAPConstants.BIND_CREDENTIAL, "password");

        // Change filter to be valid
        ldapRep.getConfig().put(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2)");
        userFederation().get(id).update(ldapRep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.userFederationResourcePath(id), ldapRep, ResourceType.USER_FEDERATION_PROVIDER);

        // Assert updated successfully
        ldapRep = userFederation().get(id).toRepresentation();
        assertFederationProvider(ldapRep, id, id, "ldap", 2, -1, -1, -1, LDAPConstants.BIND_DN, "cn=manager-updated", LDAPConstants.BIND_CREDENTIAL, "password",
                LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2)");

        // Assert update displayName
        ldapRep.setDisplayName("ldap2");
        userFederation().get(id).update(ldapRep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.userFederationResourcePath(id), ldapRep, ResourceType.USER_FEDERATION_PROVIDER);

        assertFederationProvider(userFederation().get(id).toRepresentation(), id, "ldap2", "ldap", 2, -1, -1, -1, LDAPConstants.BIND_DN, "cn=manager-updated", LDAPConstants.BIND_CREDENTIAL, "password",
                LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2)");



        // Cleanup
        removeUserFederationProvider(id);
    }


    @Test
    public void testKerberosAuthenticatorEnabledAutomatically() {
        // Assert kerberos authenticator DISABLED
        AuthenticationExecutionInfoRepresentation kerberosExecution = findKerberosExecution();
        Assert.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // create LDAP provider with kerberos
        UserFederationProviderRepresentation ldapRep = UserFederationProviderBuilder.create()
                .displayName("ldap2")
                .providerName("ldap")
                .priority(2)
                .configProperty(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true")
                .build();
        String id = createUserFederationProvider(ldapRep);

        // Assert kerberos authenticator ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assert.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Switch kerberos authenticator to DISABLED
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.toString());
        realm.flows().updateExecutions("browser", kerberosExecution);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);

        // update LDAP provider with kerberos
        ldapRep = userFederation().get(id).toRepresentation();
        userFederation().get(id).update(ldapRep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.userFederationResourcePath(id), ldapRep, ResourceType.USER_FEDERATION_PROVIDER);

        // Assert kerberos authenticator ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assert.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Cleanup
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.toString());
        realm.flows().updateExecutions("browser", kerberosExecution);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);
        removeUserFederationProvider(id);
    }

    @Test
    public void testKerberosAuthenticatorChangedOnlyIfDisabled() {
        // Change kerberos to REQUIRED
        AuthenticationExecutionInfoRepresentation kerberosExecution = findKerberosExecution();
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString());
        realm.flows().updateExecutions("browser", kerberosExecution);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);

        // create LDAP provider with kerberos
        UserFederationProviderRepresentation ldapRep = UserFederationProviderBuilder.create()
                .displayName("ldap2")
                .providerName("ldap")
                .priority(2)
                .configProperty(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true")
                .build();
        String id = createUserFederationProvider(ldapRep);

        // Assert kerberos authenticator still REQUIRED
        kerberosExecution = findKerberosExecution();
        Assert.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.REQUIRED.toString());

        // update LDAP provider with kerberos
        ldapRep = userFederation().get(id).toRepresentation();
        userFederation().get(id).update(ldapRep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.userFederationResourcePath(id), ldapRep, ResourceType.USER_FEDERATION_PROVIDER);

        // Assert kerberos authenticator still REQUIRED
        kerberosExecution = findKerberosExecution();
        Assert.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.REQUIRED.toString());

        // Cleanup
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.toString());
        realm.flows().updateExecutions("browser", kerberosExecution);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);
        removeUserFederationProvider(id);

    }


    @Test (expected = NotFoundException.class)
    public void testLookupNotExistentProvider() {
        userFederation().get("not-existent").toRepresentation();
    }


    @Test
    public void testSyncFederationProvider() {
        // create provider
        UserFederationProviderRepresentation dummyRep1 = UserFederationProviderBuilder.create()
                .providerName("dummy")
                .build();
        String id1 = createUserFederationProvider(dummyRep1);


        // Sync with unknown action shouldn't pass
        try {
            userFederation().get(id1).syncUsers("unknown");
            Assert.fail("Not expected to sync with unknown action");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Assert sync didn't happen
        Assert.assertEquals(-1, userFederation().get(id1).toRepresentation().getLastSync());

        // Sync and assert it happened
        UserFederationSyncResultRepresentation syncResult = userFederation().get(id1).syncUsers("triggerFullSync");
        Assert.assertEquals("0 imported users, 0 updated users", syncResult.getStatus());

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("action", "triggerFullSync");
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userFederationResourcePath(id1) + "/sync", eventRep, ResourceType.USER_FEDERATION_PROVIDER);

        int fullSyncTime = userFederation().get(id1).toRepresentation().getLastSync();
        Assert.assertTrue(fullSyncTime > 0);

        // Changed sync
        setTimeOffset(50);
        syncResult = userFederation().get(id1).syncUsers("triggerChangedUsersSync");

        eventRep.put("action", "triggerChangedUsersSync");
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userFederationResourcePath(id1) + "/sync", eventRep, ResourceType.USER_FEDERATION_PROVIDER);

        Assert.assertEquals("0 imported users, 0 updated users", syncResult.getStatus());
        int changedSyncTime = userFederation().get(id1).toRepresentation().getLastSync();
        Assert.assertTrue(fullSyncTime + 50 <= changedSyncTime);

        // Cleanup
        resetTimeOffset();
        removeUserFederationProvider(id1);
    }


    private String createUserFederationProvider(UserFederationProviderRepresentation rep) {
        Response resp = userFederation().create(rep);
        Assert.assertEquals(201, resp.getStatus());
        resp.close();
        String federationProviderId = ApiUtil.getCreatedId(resp);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userFederationResourcePath(federationProviderId), rep, ResourceType.USER_FEDERATION_PROVIDER);
        return federationProviderId;
    }

    private void removeUserFederationProvider(String id) {
        userFederation().get(id).remove();
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.userFederationResourcePath(id), ResourceType.USER_FEDERATION_PROVIDER);
    }

    private void assertFederationProvider(UserFederationProviderRepresentation rep, String id, String displayName, String providerName,
                                          int priority, int fullSyncPeriod, int changeSyncPeriod, int lastSync,
                                          String... config) {
        Assert.assertEquals(id, rep.getId());
        Assert.assertEquals(displayName, rep.getDisplayName());
        Assert.assertEquals(providerName, rep.getProviderName());
        Assert.assertEquals(priority, rep.getPriority());
        Assert.assertEquals(fullSyncPeriod, rep.getFullSyncPeriod());
        Assert.assertEquals(changeSyncPeriod, rep.getChangedSyncPeriod());
        Assert.assertEquals(lastSync, rep.getLastSync());

        Assert.assertMap(rep.getConfig(), config);
    }


    private AuthenticationExecutionInfoRepresentation findKerberosExecution() {
        AuthenticationExecutionInfoRepresentation kerberosExecution = null;
        List<AuthenticationExecutionInfoRepresentation> executionReps = realm.flows().getExecutions("browser");
        kerberosExecution = AbstractAuthenticationTest.findExecutionByProvider("auth-spnego", executionReps);

        Assert.assertNotNull(kerberosExecution);
        return kerberosExecution;
    }
}
