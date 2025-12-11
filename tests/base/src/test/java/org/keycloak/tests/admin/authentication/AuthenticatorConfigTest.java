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

package org.keycloak.tests.admin.authentication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpDetectExistingBrokerUserAuthenticatorFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class AuthenticatorConfigTest extends AbstractAuthenticationTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private String flowId;
    private String executionId;
    private String executionId2;

    @BeforeEach
    public void beforeConfigTest() {
        AuthenticationFlowRepresentation flowRep = newFlow("firstBrokerLogin2", "firstBrokerLogin2", "basic-flow", true, false);
        flowId = createFlow(flowRep);

        HashMap<String, Object> params = new HashMap<>();
        params.put("provider", IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID);
        authMgmtResource.addExecution("firstBrokerLogin2", params);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authAddExecutionPath("firstBrokerLogin2"), params, ResourceType.AUTH_EXECUTION);
        params.put("provider", IdpDetectExistingBrokerUserAuthenticatorFactory.PROVIDER_ID);
        authMgmtResource.addExecution("firstBrokerLogin2", params);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authAddExecutionPath("firstBrokerLogin2"), params, ResourceType.AUTH_EXECUTION);

        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions("firstBrokerLogin2");
        AuthenticationExecutionInfoRepresentation exec = findExecutionByProvider(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, executionReps);
        Assertions.assertNotNull(exec);
        executionId = exec.getId();
        exec = findExecutionByProvider(IdpDetectExistingBrokerUserAuthenticatorFactory.PROVIDER_ID, executionReps);
        Assertions.assertNotNull(exec);
        executionId2 = exec.getId();
    }

    @Test
    public void testCreateConfigWithReservedChar() {
        AuthenticatorConfigRepresentation cfg = newConfig("f!oo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        Response resp = authMgmtResource.newExecutionConfig(executionId, cfg);
        Assertions.assertEquals(400, resp.getStatus());
    }

    @Test
    public void testCreateConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");

        // Attempt to create config for non-existent execution
        try (Response response = authMgmtResource.newExecutionConfig("exec-id-doesnt-exists", cfg)) {
            Assertions.assertEquals(404, response.getStatus());
        }

        // Create config success
        String cfgId = createConfig(executionId, cfg);

        // Assert found
        AuthenticatorConfigRepresentation cfgRep = authMgmtResource.getAuthenticatorConfig(cfgId);
        assertConfig(cfgRep, cfgId, "foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");

        // Cleanup
        authMgmtResource.removeAuthenticatorConfig(cfgId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authExecutionConfigPath(cfgId), ResourceType.AUTHENTICATOR_CONFIG);
    }

    @Test
    public void testUpdateConfigWithBadChar() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        String cfgId = createConfig(executionId, cfg);
        AuthenticatorConfigRepresentation cfgRep = authMgmtResource.getAuthenticatorConfig(cfgId);
        
        cfgRep.setAlias("Bad@Char");
        Assertions.assertThrows(BadRequestException.class, () -> authMgmtResource.updateAuthenticatorConfig(cfgRep.getId(), cfgRep));
    }
    
    @Test
    public void testUpdateConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        String cfgId = createConfig(executionId, cfg);
        final AuthenticatorConfigRepresentation cfgRepNonExistent = authMgmtResource.getAuthenticatorConfig(cfgId);

        // Try to update not existent config
        NotFoundException nfe = Assertions.assertThrows(NotFoundException.class, () -> authMgmtResource.updateAuthenticatorConfig("not-existent", cfgRepNonExistent));
        Assertions.assertEquals(404, nfe.getResponse().getStatus());

        // Assert nothing changed
        AuthenticatorConfigRepresentation cfgRep = authMgmtResource.getAuthenticatorConfig(cfgId);
        assertConfig(cfgRep, cfgId, "foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");

        // Update success
        cfgRep.setAlias("foo2");
        cfgRep.getConfig().put("configKey2", "configValue2");
        authMgmtResource.updateAuthenticatorConfig(cfgRep.getId(), cfgRep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authExecutionConfigPath(cfgId), cfgRep, ResourceType.AUTHENTICATOR_CONFIG);

        // Assert updated
        cfgRep = authMgmtResource.getAuthenticatorConfig(cfgRep.getId());
        assertConfig(cfgRep, cfgId, "foo2",
                IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true",
                "configKey2", "configValue2");
    }


    @Test
    public void testRemoveConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        String cfgId = createConfig(executionId, cfg);
        AuthenticatorConfigRepresentation cfgRep = authMgmtResource.getAuthenticatorConfig(cfgId);

        // Assert execution has our config
        AuthenticationExecutionInfoRepresentation execution = findExecutionByProvider(
                IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, authMgmtResource.getExecutions("firstBrokerLogin2"));
        Assertions.assertEquals(cfgRep.getId(), execution.getAuthenticationConfig());

        // Test remove not-existent
        NotFoundException nfe = Assertions.assertThrows(NotFoundException.class, () -> authMgmtResource.removeAuthenticatorConfig("not-existent"));
        Assertions.assertEquals(404, nfe.getResponse().getStatus());

        // Test remove our config
        authMgmtResource.removeAuthenticatorConfig(cfgId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authExecutionConfigPath(cfgId), ResourceType.AUTHENTICATOR_CONFIG);

        // Assert config not found
        nfe = Assertions.assertThrows(NotFoundException.class, () -> authMgmtResource.getAuthenticatorConfig(cfgRep.getId()));
        Assertions.assertEquals(404, nfe.getResponse().getStatus());

        // Assert execution doesn't have our config
        execution = findExecutionByProvider(
                IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, authMgmtResource.getExecutions("firstBrokerLogin2"));
        Assertions.assertNull(execution.getAuthenticationConfig());
    }

    @Test
    public void testNullsafetyIterationOverProperties() {
        String providerId = "auth-cookie";
        String providerName = "Cookie";
        AuthenticatorConfigInfoRepresentation description = authMgmtResource.getAuthenticatorConfigDescription(providerId);

        Assertions.assertEquals(providerName, description.getName());
        Assertions.assertTrue(description.getProperties().isEmpty());
    }

    @Test
    public void testDuplicateAuthenticatorConfigAlias() {
        // create a config for step1
        AuthenticatorConfigRepresentation config1 = new AuthenticatorConfigRepresentation();
        config1.setAlias("test-config-1");
        config1.setConfig(Map.of("key", "value"));
        String config1Id = createConfig(executionId, config1);

        // create the same config name for step2, should fail
        try (Response response = authMgmtResource.newExecutionConfig(executionId2, config1)) {
            Assertions.assertEquals(409, response.getStatus());
        }

        // create a config for step2
        AuthenticatorConfigRepresentation config2 = new AuthenticatorConfigRepresentation();
        config2.setAlias("test-config-2");
        config2.setConfig(Map.of("key", "value"));
        String config2Id = createConfig(executionId, config2);

        // create a new config for step1, config1 should be removed
        AuthenticatorConfigRepresentation config3 = new AuthenticatorConfigRepresentation();
        config3.setAlias("test-config-1-modified");
        config3.setConfig(Map.of("key", "value"));
        String tmpConfig3Id = createConfig(executionId, config3);
        NotFoundException nfe = Assertions.assertThrows(NotFoundException.class, () -> authMgmtResource.getAuthenticatorConfig(config1Id));
        Assertions.assertEquals(404, nfe.getResponse().getStatus());

        // create a new config with thew same name but that overwrites the previous one
        String config3Id = createConfig(executionId, config3);
        nfe = Assertions.assertThrows(NotFoundException.class, () -> authMgmtResource.getAuthenticatorConfig(tmpConfig3Id));
        Assertions.assertEquals(404, nfe.getResponse().getStatus());

        // delete execution step1, config3 should be removed
        authMgmtResource.removeExecution(executionId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authExecutionPath(executionId), ResourceType.AUTH_EXECUTION);
        nfe = Assertions.assertThrows(NotFoundException.class, () -> authMgmtResource.getAuthenticatorConfig(config3Id));
        Assertions.assertEquals(404, nfe.getResponse().getStatus());
    }

    @Test
    public void testMissingConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        final String cfgId = createConfig(executionId, cfg);
        final String realmId = testRealmId;
        AuthenticatorConfigRepresentation cfgRep = authMgmtResource.getAuthenticatorConfig(cfgId);
        Assertions.assertNotNull(cfgRep);

        runOnServer.run(session -> {
            // emulating a broken config id, remove the config but do not remove the link in the authenticator
            RealmModel realm = session.realms().getRealm(realmId);
            AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(cfgId);
            realm.removeAuthenticatorConfig(config);
        });

        // check the flow can be read and execution has no config
        AuthenticationFlowRepresentation flow = authMgmtResource.getFlow(flowId);
        AuthenticationExecutionExportRepresentation execExport = flow.getAuthenticationExecutions().stream()
                .filter(ae -> IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID.equals(ae.getAuthenticator()))
                .findAny()
                .orElse(null);
        Assertions.assertNotNull(execExport);
        Assertions.assertNull(execExport.getAuthenticatorConfig());

        // check the execution can be read with no configuration assigned
        AuthenticationExecutionInfoRepresentation execInfo = findExecutionByProvider(
                IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, authMgmtResource.getExecutions("firstBrokerLogin2"));
        Assertions.assertNull(execInfo.getAuthenticationConfig());
    }

    private String createConfig(String executionId, AuthenticatorConfigRepresentation cfg) {
        try (Response resp = authMgmtResource.newExecutionConfig(executionId, cfg)) {
            Assertions.assertEquals(201, resp.getStatus());
            String cfgId = ApiUtil.getCreatedId(resp);
            Assertions.assertNotNull(cfgId);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authAddExecutionConfigPath(executionId), cfg, ResourceType.AUTHENTICATOR_CONFIG);
            return cfgId;
        }
    }

    private AuthenticatorConfigRepresentation newConfig(String alias, String cfgKey, String cfgValue) {
        AuthenticatorConfigRepresentation cfg = new AuthenticatorConfigRepresentation();
        cfg.setAlias(alias);
        Map<String, String> cfgMap = new HashMap<>();
        cfgMap.put(cfgKey, cfgValue);
        cfg.setConfig(cfgMap);
        return cfg;
    }

    private void assertConfig(AuthenticatorConfigRepresentation cfgRep, String id, String alias, String... fields) {
        Assertions.assertEquals(id, cfgRep.getId());
        Assertions.assertEquals(alias, cfgRep.getAlias());
        Assert.assertMap(cfgRep.getConfig(), fields);
    }
}
