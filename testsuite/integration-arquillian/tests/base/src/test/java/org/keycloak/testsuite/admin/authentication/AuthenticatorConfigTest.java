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

package org.keycloak.testsuite.admin.authentication;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.BadRequestException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticatorConfigTest extends AbstractAuthenticationTest {

    private String executionId;

    @Before
    public void beforeConfigTest() {
        AuthenticationFlowRepresentation flowRep = newFlow("firstBrokerLogin2", "firstBrokerLogin2", "basic-flow", true, false);
        createFlow(flowRep);

        HashMap<String, String> params = new HashMap<>();
        params.put("provider", IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID);
        authMgmtResource.addExecution("firstBrokerLogin2", params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionPath("firstBrokerLogin2"), params, ResourceType.AUTH_EXECUTION);

        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions("firstBrokerLogin2");
        AuthenticationExecutionInfoRepresentation exec = findExecutionByProvider(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, executionReps);
        Assert.assertNotNull(exec);
        executionId = exec.getId();
    }

    @Test
    public void testCreateConfigWithReservedChar() {
        AuthenticatorConfigRepresentation cfg = newConfig("f!oo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        Response resp = authMgmtResource.newExecutionConfig(executionId, cfg);
        Assert.assertEquals(400, resp.getStatus());
    }

    @Test
    public void testCreateConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");

        // Attempt to create config for non-existent execution
        Response response = authMgmtResource.newExecutionConfig("exec-id-doesnt-exists", cfg);
        Assert.assertEquals(404, response.getStatus());
        response.close();

        // Create config success
        String cfgId = createConfig(executionId, cfg);

        // Assert found
        AuthenticatorConfigRepresentation cfgRep = authMgmtResource.getAuthenticatorConfig(cfgId);
        assertConfig(cfgRep, cfgId, "foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");

        // Cleanup
        authMgmtResource.removeAuthenticatorConfig(cfgId);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.authExecutionConfigPath(cfgId), ResourceType.AUTHENTICATOR_CONFIG);
    }

    @Test (expected = BadRequestException.class)
    public void testUpdateConfigWithBadChar() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        String cfgId = createConfig(executionId, cfg);
        AuthenticatorConfigRepresentation cfgRep = authMgmtResource.getAuthenticatorConfig(cfgId);
        
        cfgRep.setAlias("Bad@Char");
        authMgmtResource.updateAuthenticatorConfig(cfgRep.getId(), cfgRep);
    }
    
    @Test
    public void testUpdateConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        String cfgId = createConfig(executionId, cfg);
        AuthenticatorConfigRepresentation cfgRep = authMgmtResource.getAuthenticatorConfig(cfgId);

        // Try to update not existent config
        try {
            authMgmtResource.updateAuthenticatorConfig("not-existent", cfgRep);
            Assert.fail("Config didn't found");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Assert nothing changed
        cfgRep = authMgmtResource.getAuthenticatorConfig(cfgId);
        assertConfig(cfgRep, cfgId, "foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");

        // Update success
        cfgRep.setAlias("foo2");
        cfgRep.getConfig().put("configKey2", "configValue2");
        authMgmtResource.updateAuthenticatorConfig(cfgRep.getId(), cfgRep);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authExecutionConfigPath(cfgId), cfgRep, ResourceType.AUTHENTICATOR_CONFIG);

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
        Assert.assertEquals(cfgRep.getId(), execution.getAuthenticationConfig());


        // Test remove not-existent
        try {
            authMgmtResource.removeAuthenticatorConfig("not-existent");
            Assert.fail("Config didn't found");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Test remove our config
        authMgmtResource.removeAuthenticatorConfig(cfgId);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.authExecutionConfigPath(cfgId), ResourceType.AUTHENTICATOR_CONFIG);

        // Assert config not found
        try {
            authMgmtResource.getAuthenticatorConfig(cfgRep.getId());
            Assert.fail("Not expected to find config");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Assert execution doesn't have our config
        execution = findExecutionByProvider(
                IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, authMgmtResource.getExecutions("firstBrokerLogin2"));
        Assert.assertNull(execution.getAuthenticationConfig());
    }

    @Test
    public void testNullsafetyIterationOverProperties() {
        String providerId = "auth-cookie";
        String providerName = "Cookie";
        AuthenticatorConfigInfoRepresentation description = authMgmtResource.getAuthenticatorConfigDescription(providerId);

        Assert.assertEquals(providerName, description.getName());
        Assert.assertTrue(description.getProperties().isEmpty());
    }

    private String createConfig(String executionId, AuthenticatorConfigRepresentation cfg) {
        Response resp = authMgmtResource.newExecutionConfig(executionId, cfg);
        Assert.assertEquals(201, resp.getStatus());
        String cfgId = ApiUtil.getCreatedId(resp);
        Assert.assertNotNull(cfgId);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionConfigPath(executionId), cfg, ResourceType.AUTH_EXECUTION);
        return cfgId;
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
        Assert.assertEquals(id, cfgRep.getId());
        Assert.assertEquals(alias, cfgRep.getAlias());
        Assert.assertMap(cfgRep.getConfig(), fields);
    }
}
