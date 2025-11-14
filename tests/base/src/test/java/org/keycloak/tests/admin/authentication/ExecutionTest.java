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

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public class ExecutionTest extends AbstractAuthenticationTest {

    // KEYCLOAK-7975
    @Test
    public void testUpdateAuthenticatorConfig() {
        // copy built-in flow so we get a new editable flow
        HashMap<String, Object> params = new HashMap<>();
        params.put("newName", "new-browser-flow");
        Response response = authMgmtResource.copy("browser", params);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
        try {
            Assertions.assertEquals(201, response.getStatus(), "Copy flow");
        } finally {
            response.close();
        }

        // create Conditional OTP Form execution
        params.put("provider", "auth-conditional-otp-form");
        authMgmtResource.addExecution("new-browser-flow", params);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authAddExecutionPath("new-browser-flow"), params, ResourceType.AUTH_EXECUTION);

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

        assertThat(updated.getConfig().values(), hasItems("test-updated", "skip"));
    }

    @Test
    public void testAddRemoveExecution() {

        // try add execution to built-in flow
        HashMap<String, Object> params = new HashMap<>();
        params.put("provider", "idp-review-profile");
        try {
            authMgmtResource.addExecution("browser", params);
            Assertions.fail("add execution to built-in flow should fail");
        } catch (BadRequestException expected) {
            // Expected
        }

        // try add execution to not-existent flow
        try {
            authMgmtResource.addExecution("not-existent", params);
            Assertions.fail("add execution to not-existent flow should fail");
        } catch (BadRequestException expected) {
            // Expected
        }

        // copy built-in flow so we get a new editable flow
        params.put("newName", "Copy-of-browser");
        Response response = authMgmtResource.copy("browser", params);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
        try {
            Assertions.assertEquals(201, response.getStatus(), "Copy flow");
        } finally {
            response.close();
        }

        // add execution using inexistent provider
        params.put("provider", "test-execution");
        try {
            authMgmtResource.addExecution("CopyOfBrowser", params);
            Assertions.fail("add execution with inexistent provider should fail");
        } catch(BadRequestException expected) {
            // Expected
        }

        // add execution - should succeed
        params.put("provider", "idp-review-profile");
        authMgmtResource.addExecution("Copy-of-browser", params);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authAddExecutionPath("Copy-of-browser"), params, ResourceType.AUTH_EXECUTION);

        // check execution was added
        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions("Copy-of-browser");
        AuthenticationExecutionInfoRepresentation exec = findExecutionByProvider("idp-review-profile", executionReps);
        Assertions.assertNotNull(exec, "idp-review-profile added");

        // we'll need auth-cookie later
        AuthenticationExecutionInfoRepresentation authCookieExec = findExecutionByProvider("auth-cookie", executionReps);

        AuthenticationExecutionInfoRepresentation previousExecution = findPreviousExecution(exec, executionReps);
        Assertions.assertNotNull(previousExecution);
        compareExecution(newExecInfo("Review Profile", "idp-review-profile", true, 0, 5, DISABLED, null, new String[]{REQUIRED, ALTERNATIVE,DISABLED}, previousExecution.getPriority() + 1), exec);

        // remove execution
        authMgmtResource.removeExecution(exec.getId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authExecutionPath(exec.getId()), ResourceType.AUTH_EXECUTION);

        // check execution was removed
        executionReps = authMgmtResource.getExecutions("Copy-of-browser");
        exec = findExecutionByProvider("idp-review-profile", executionReps);
        Assertions.assertNull(exec, "idp-review-profile removed");

        // now add the execution again using a different method and representation

        // delete auth-cookie
        authMgmtResource.removeExecution(authCookieExec.getId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authExecutionPath(authCookieExec.getId()), ResourceType.AUTH_EXECUTION);

        AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
        rep.setPriority(10);
        rep.setAuthenticator("auth-cookie");
        rep.setRequirement(CONDITIONAL);

        // Should fail - missing parent flow
        response = authMgmtResource.addExecution(rep);
        try {
            Assertions.assertEquals(400, response.getStatus(), "added execution missing parent flow");
        } finally {
            response.close();
        }

        // Should fail - not existent parent flow
        rep.setParentFlow("not-existent-id");
        response = authMgmtResource.addExecution(rep);
        try {
            Assertions.assertEquals(400, response.getStatus(), "added execution missing parent flow");
        } finally {
            response.close();
        }

        // Should fail - add execution to builtin flow
        AuthenticationFlowRepresentation browserFlow = findFlowByAlias("browser", authMgmtResource.getFlows());
        rep.setParentFlow(browserFlow.getId());
        response = authMgmtResource.addExecution(rep);
        try {
            Assertions.assertEquals(400, response.getStatus(), "added execution to builtin flow");
        } finally {
            response.close();
        }

        // get Copy-of-browser flow id, and set it on execution
        List<AuthenticationFlowRepresentation> flows = authMgmtResource.getFlows();
        AuthenticationFlowRepresentation flow = findFlowByAlias("Copy-of-browser", flows);
        rep.setParentFlow(flow.getId());

        // add execution - should succeed
        String createdId = ApiUtil.getCreatedId(authMgmtResource.addExecution(rep));
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourcePath(AdminEventPaths.authMgmtBasePath(), "executions", createdId)
                .representation(rep)
                .resourceType(ResourceType.AUTH_EXECUTION);

        // check execution was added
        List<AuthenticationExecutionInfoRepresentation> executions = authMgmtResource.getExecutions("Copy-of-browser");
        exec = findExecutionByProvider("auth-cookie", executions);
        Assertions.assertNotNull(exec, "auth-cookie added");

        // Note: there is no checking in addExecution if requirement is one of requirementChoices
        // Thus we can have OPTIONAL which is neither ALTERNATIVE, nor DISABLED
        compareExecution(newExecInfo("Cookie", "auth-cookie", false, 0, 0, CONDITIONAL, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 10), exec);
    }

    @Test
    public void testUpdateExecution() {

        // get current auth-cookie execution
        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions("browser");
        AuthenticationExecutionInfoRepresentation exec = findExecutionByProvider("auth-cookie", executionReps);

        Assertions.assertEquals(ALTERNATIVE, exec.getRequirement(), "auth-cookie set to ALTERNATIVE");
        Assertions.assertEquals(exec.getIndex(), 0, "auth-cookie is first in the flow");

        // switch from DISABLED to ALTERNATIVE
        exec.setRequirement(DISABLED);
        exec.setPriority(Integer.MAX_VALUE);
        authMgmtResource.updateExecutions("browser", exec);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), exec, ResourceType.AUTH_EXECUTION);

        // make sure the change is visible
        executionReps = authMgmtResource.getExecutions("browser");

        // get current auth-cookie execution
        AuthenticationExecutionInfoRepresentation exec2 = findExecutionByProvider("auth-cookie", executionReps);

        // The execution is expected to be last after priority change
        long expectedIndex = executionReps.stream()
            .filter(r -> r.getLevel() == exec2.getLevel())
            .count() - 1;
        exec.setIndex(Math.toIntExact(expectedIndex));

        compareExecution(exec, exec2);
    }

    @Test
    public void testClientFlowExecutions() {
        // Create client flow
        AuthenticationFlowRepresentation clientFlow = newFlow("new-client-flow", "desc", AuthenticationFlow.CLIENT_FLOW, true, false);
        createFlow(clientFlow);

        // Add execution to it
        Map<String, Object> executionData = new HashMap<>();
        executionData.put("provider", ClientIdAndSecretAuthenticator.PROVIDER_ID);
        authMgmtResource.addExecution("new-client-flow", executionData);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authAddExecutionPath("new-client-flow"), executionData, ResourceType.AUTH_EXECUTION);

        // Check executions of not-existent flow - SHOULD FAIL
        try {
            authMgmtResource.getExecutions("not-existent");
            Assertions.fail("Not expected to find executions");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Check existent executions
        List<AuthenticationExecutionInfoRepresentation> executions = authMgmtResource.getExecutions("new-client-flow");
        AuthenticationExecutionInfoRepresentation executionRep = findExecutionByProvider(ClientIdAndSecretAuthenticator.PROVIDER_ID, executions);
        Assertions.assertNotNull(executionRep);

        // Update execution with not-existent flow - SHOULD FAIL
        try {
            authMgmtResource.updateExecutions("not-existent", executionRep);
            Assertions.fail("Not expected to update execution with not-existent flow");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Update execution with not-existent ID - SHOULD FAIL
        AuthenticationExecutionInfoRepresentation executionRep2 = new AuthenticationExecutionInfoRepresentation();
        executionRep2.setId("not-existent");
        try {
            authMgmtResource.updateExecutions("new-client-flow", executionRep2);
            Assertions.fail("Not expected to update not-existent execution");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Update success
        executionRep.setRequirement(ALTERNATIVE);
        authMgmtResource.updateExecutions("new-client-flow", executionRep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("new-client-flow"), executionRep, ResourceType.AUTH_EXECUTION);

        // Check updated
        executionRep = findExecutionByProvider(ClientIdAndSecretAuthenticator.PROVIDER_ID, authMgmtResource.getExecutions("new-client-flow"));
        Assertions.assertEquals(ALTERNATIVE, executionRep.getRequirement());

        // Remove execution with not-existent ID
        try {
            authMgmtResource.removeExecution("not-existent");
            Assertions.fail("Didn't expect to find execution");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Successfuly remove execution and flow
        authMgmtResource.removeExecution(executionRep.getId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authExecutionPath(executionRep.getId()), ResourceType.AUTH_EXECUTION);
    }

    @Test
    public void testRequirementsInExecution() {
        HashMap<String, Object> params = new HashMap<>();
        String newBrowserFlow = "new-exec-flow";

        params.put("newName", newBrowserFlow);
        try (Response response = authMgmtResource.copy("browser", params)) {
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
            Assertions.assertEquals(201, response.getStatus(), "Copy flow");
        }

        addExecutionCheckReq(newBrowserFlow, UsernameFormFactory.PROVIDER_ID, params, REQUIRED);
        addExecutionCheckReq(newBrowserFlow, WebAuthnAuthenticatorFactory.PROVIDER_ID, params, DISABLED);

        AuthenticationFlowRepresentation rep = findFlowByAlias(newBrowserFlow, authMgmtResource.getFlows());
        Assertions.assertNotNull(rep);
        authMgmtResource.deleteFlow(rep.getId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authFlowPath(rep.getId()), ResourceType.AUTH_FLOW);
    }

    private void addExecutionCheckReq(String flow, String providerID, HashMap<String, Object> params, String expectedRequirement) {
        params.put("provider", providerID);
        authMgmtResource.addExecution(flow, params);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authAddExecutionPath(flow), params, ResourceType.AUTH_EXECUTION);

        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions(flow);
        AuthenticationExecutionInfoRepresentation exec = findExecutionByProvider(providerID, executionReps);

        Assertions.assertNotNull(exec);
        Assertions.assertEquals(expectedRequirement, exec.getRequirement());

        authMgmtResource.removeExecution(exec.getId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authExecutionPath(exec.getId()), ResourceType.AUTH_EXECUTION);
    }
}
