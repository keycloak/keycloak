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

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.challenge.NoCookieFlowRedirectAuthenticatorFactory;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.AssertAdminEvents;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ExecutionTest extends AbstractAuthenticationTest {

    // KEYCLOAK-7975
    @Test
    public void testUpdateAuthenticatorConfig() {
        // copy built-in flow so we get a new editable flow
        HashMap<String, String> params = new HashMap<>();
        params.put("newName", "new-browser-flow");
        Response response = authMgmtResource.copy("browser", params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
        try {
            Assert.assertEquals("Copy flow", 201, response.getStatus());
        } finally {
            response.close();
        }

        // create Conditional OTP Form execution
        params.put("provider", "auth-conditional-otp-form");
        authMgmtResource.addExecution("new-browser-flow", params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionPath("new-browser-flow"), params, ResourceType.AUTH_EXECUTION);

        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions("new-browser-flow");
        AuthenticationExecutionInfoRepresentation exec = findExecutionByProvider("auth-conditional-otp-form", executionReps);

        // create authenticator config for the execution
        Map<String, String> config = new HashMap<>();
        config.put("defaultOtpOutcome", "skip");
        config.put("otpControlAttribute", "test");
        config.put("forceOtpForHeaderPattern", "");
        config.put("forceOtpRole", "");
        config.put("noOtpRequiredForHeaderPattern", "");
        config.put("skipOtpRole", "");

        AuthenticatorConfigRepresentation authConfigRep = new AuthenticatorConfigRepresentation();
        authConfigRep.setAlias("conditional-otp-form-config-alias");
        authConfigRep.setConfig(config);
        response = authMgmtResource.newExecutionConfig(exec.getId(), authConfigRep);

        try {
            authConfigRep.setId(ApiUtil.getCreatedId(response));
        } finally {
            response.close();
        }

        // try to update the config adn check
        config.put("otpControlAttribute", "test-updated");
        authConfigRep.setConfig(config);
        authMgmtResource.updateAuthenticatorConfig(authConfigRep.getId(), authConfigRep);

        AuthenticatorConfigRepresentation updated = authMgmtResource.getAuthenticatorConfig(authConfigRep.getId());

        Assert.assertThat(updated.getConfig().values(), hasItems("test-updated", "skip"));
    }

    @Test
    public void testAddRemoveExecution() {

        // try add execution to built-in flow
        HashMap<String, String> params = new HashMap<>();
        params.put("provider", "idp-review-profile");
        try {
            authMgmtResource.addExecution("browser", params);
            Assert.fail("add execution to built-in flow should fail");
        } catch (BadRequestException expected) {
            // Expected
        }

        // try add execution to not-existent flow
        try {
            authMgmtResource.addExecution("not-existent", params);
            Assert.fail("add execution to not-existent flow should fail");
        } catch (BadRequestException expected) {
            // Expected
        }

        // copy built-in flow so we get a new editable flow
        params.put("newName", "Copy-of-browser");
        Response response = authMgmtResource.copy("browser", params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
        try {
            Assert.assertEquals("Copy flow", 201, response.getStatus());
        } finally {
            response.close();
        }

        // add execution using inexistent provider
        params.put("provider", "test-execution");
        try {
            authMgmtResource.addExecution("CopyOfBrowser", params);
            Assert.fail("add execution with inexistent provider should fail");
        } catch(BadRequestException expected) {
            // Expected
        }

        // add execution - should succeed
        params.put("provider", "idp-review-profile");
        authMgmtResource.addExecution("Copy-of-browser", params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionPath("Copy-of-browser"), params, ResourceType.AUTH_EXECUTION);

        // check execution was added
        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions("Copy-of-browser");
        AuthenticationExecutionInfoRepresentation exec = findExecutionByProvider("idp-review-profile", executionReps);
        Assert.assertNotNull("idp-review-profile added", exec);

        // we'll need auth-cookie later
        AuthenticationExecutionInfoRepresentation authCookieExec = findExecutionByProvider("auth-cookie", executionReps);

        compareExecution(newExecInfo("Review Profile", "idp-review-profile", true, 0, 4, DISABLED, null, new String[]{REQUIRED, ALTERNATIVE,DISABLED}), exec);

        // remove execution
        authMgmtResource.removeExecution(exec.getId());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.authExecutionPath(exec.getId()), ResourceType.AUTH_EXECUTION);

        // check execution was removed
        executionReps = authMgmtResource.getExecutions("Copy-of-browser");
        exec = findExecutionByProvider("idp-review-profile", executionReps);
        Assert.assertNull("idp-review-profile removed", exec);

        // now add the execution again using a different method and representation

        // delete auth-cookie
        authMgmtResource.removeExecution(authCookieExec.getId());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.authExecutionPath(authCookieExec.getId()), ResourceType.AUTH_EXECUTION);

        AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
        rep.setPriority(10);
        rep.setAuthenticator("auth-cookie");
        rep.setRequirement(CONDITIONAL);

        // Should fail - missing parent flow
        response = authMgmtResource.addExecution(rep);
        try {
            Assert.assertEquals("added execution missing parent flow", 400, response.getStatus());
        } finally {
            response.close();
        }

        // Should fail - not existent parent flow
        rep.setParentFlow("not-existent-id");
        response = authMgmtResource.addExecution(rep);
        try {
            Assert.assertEquals("added execution missing parent flow", 400, response.getStatus());
        } finally {
            response.close();
        }

        // Should fail - add execution to builtin flow
        AuthenticationFlowRepresentation browserFlow = findFlowByAlias("browser", authMgmtResource.getFlows());
        rep.setParentFlow(browserFlow.getId());
        response = authMgmtResource.addExecution(rep);
        try {
            Assert.assertEquals("added execution to builtin flow", 400, response.getStatus());
        } finally {
            response.close();
        }

        // get Copy-of-browser flow id, and set it on execution
        List<AuthenticationFlowRepresentation> flows = authMgmtResource.getFlows();
        AuthenticationFlowRepresentation flow = findFlowByAlias("Copy-of-browser", flows);
        rep.setParentFlow(flow.getId());

        // add execution - should succeed
        response = authMgmtResource.addExecution(rep);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AssertAdminEvents.isExpectedPrefixFollowedByUuid(AdminEventPaths.authMgmtBasePath() + "/executions"), rep, ResourceType.AUTH_EXECUTION);
        try {
            Assert.assertEquals("added execution", 201, response.getStatus());
        } finally {
            response.close();
        }

        // check execution was added
        List<AuthenticationExecutionInfoRepresentation> executions = authMgmtResource.getExecutions("Copy-of-browser");
        exec = findExecutionByProvider("auth-cookie", executions);
        Assert.assertNotNull("auth-cookie added", exec);

        // Note: there is no checking in addExecution if requirement is one of requirementChoices
        // Thus we can have OPTIONAL which is neither ALTERNATIVE, nor DISABLED
        compareExecution(newExecInfo("Cookie", "auth-cookie", false, 0, 3, CONDITIONAL, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}), exec);
    }

    @Test
    public void testUpdateExecution() {

        // get current auth-cookie execution
        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions("browser");
        AuthenticationExecutionInfoRepresentation exec = findExecutionByProvider("auth-cookie", executionReps);

        Assert.assertEquals("auth-cookie set to ALTERNATIVE", ALTERNATIVE, exec.getRequirement());

        // switch from DISABLED to ALTERNATIVE
        exec.setRequirement(DISABLED);
        authMgmtResource.updateExecutions("browser", exec);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), exec, ResourceType.AUTH_EXECUTION);

        // make sure the change is visible
        executionReps = authMgmtResource.getExecutions("browser");

        // get current auth-cookie execution
        AuthenticationExecutionInfoRepresentation exec2 = findExecutionByProvider("auth-cookie", executionReps);
        compareExecution(exec, exec2);
    }

    @Test
    public void testClientFlowExecutions() {
        // Create client flow
        AuthenticationFlowRepresentation clientFlow = newFlow("new-client-flow", "desc", AuthenticationFlow.CLIENT_FLOW, true, false);
        createFlow(clientFlow);

        // Add execution to it
        Map<String, String> executionData = new HashMap<>();
        executionData.put("provider", ClientIdAndSecretAuthenticator.PROVIDER_ID);
        authMgmtResource.addExecution("new-client-flow", executionData);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionPath("new-client-flow"), executionData, ResourceType.AUTH_EXECUTION);

        // Check executions of not-existent flow - SHOULD FAIL
        try {
            authMgmtResource.getExecutions("not-existent");
            Assert.fail("Not expected to find executions");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Check existent executions
        List<AuthenticationExecutionInfoRepresentation> executions = authMgmtResource.getExecutions("new-client-flow");
        AuthenticationExecutionInfoRepresentation executionRep = findExecutionByProvider(ClientIdAndSecretAuthenticator.PROVIDER_ID, executions);
        Assert.assertNotNull(executionRep);

        // Update execution with not-existent flow - SHOULD FAIL
        try {
            authMgmtResource.updateExecutions("not-existent", executionRep);
            Assert.fail("Not expected to update execution with not-existent flow");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Update execution with not-existent ID - SHOULD FAIL
        try {
            AuthenticationExecutionInfoRepresentation executionRep2 = new AuthenticationExecutionInfoRepresentation();
            executionRep2.setId("not-existent");
            authMgmtResource.updateExecutions("new-client-flow", executionRep2);
            Assert.fail("Not expected to update not-existent execution");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Update success
        executionRep.setRequirement(ALTERNATIVE);
        authMgmtResource.updateExecutions("new-client-flow", executionRep);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("new-client-flow"), executionRep, ResourceType.AUTH_EXECUTION);

        // Check updated
        executionRep = findExecutionByProvider(ClientIdAndSecretAuthenticator.PROVIDER_ID, authMgmtResource.getExecutions("new-client-flow"));
        Assert.assertEquals(ALTERNATIVE, executionRep.getRequirement());

        // Remove execution with not-existent ID
        try {
            authMgmtResource.removeExecution("not-existent");
            Assert.fail("Didn't expect to find execution");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Successfuly remove execution and flow
        authMgmtResource.removeExecution(executionRep.getId());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.authExecutionPath(executionRep.getId()), ResourceType.AUTH_EXECUTION);

        AuthenticationFlowRepresentation rep = findFlowByAlias("new-client-flow", authMgmtResource.getFlows());
        authMgmtResource.deleteFlow(rep.getId());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.authFlowPath(rep.getId()), ResourceType.AUTH_FLOW);
    }

    @Test
    @EnableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true, onlyForProduct = true)
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testRequirementsInExecution() {
        HashMap<String, String> params = new HashMap<>();
        String newBrowserFlow = "new-exec-flow";

        params.put("newName", newBrowserFlow);
        try (Response response = authMgmtResource.copy("browser", params)) {
            assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
            Assert.assertEquals("Copy flow", 201, response.getStatus());
        }

        addExecutionCheckReq(newBrowserFlow, UsernameFormFactory.PROVIDER_ID, params, REQUIRED);
        addExecutionCheckReq(newBrowserFlow, WebAuthnAuthenticatorFactory.PROVIDER_ID, params, DISABLED);
        addExecutionCheckReq(newBrowserFlow, NoCookieFlowRedirectAuthenticatorFactory.PROVIDER_ID, params, REQUIRED);

        AuthenticationFlowRepresentation rep = findFlowByAlias(newBrowserFlow, authMgmtResource.getFlows());
        Assert.assertNotNull(rep);
        authMgmtResource.deleteFlow(rep.getId());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.authFlowPath(rep.getId()), ResourceType.AUTH_FLOW);
    }

    private void addExecutionCheckReq(String flow, String providerID, HashMap<String, String> params, String expectedRequirement) {
        params.put("provider", providerID);
        authMgmtResource.addExecution(flow, params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionPath(flow), params, ResourceType.AUTH_EXECUTION);

        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions(flow);
        AuthenticationExecutionInfoRepresentation exec = findExecutionByProvider(providerID, executionReps);

        Assert.assertNotNull(exec);
        Assert.assertEquals(expectedRequirement, exec.getRequirement());

        authMgmtResource.removeExecution(exec.getId());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.authExecutionPath(exec.getId()), ResourceType.AUTH_EXECUTION);
    }
}
