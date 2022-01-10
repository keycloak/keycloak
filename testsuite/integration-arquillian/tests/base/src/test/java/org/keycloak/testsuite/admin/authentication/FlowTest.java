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
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.testsuite.util.AdminEventPaths;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.keycloak.testsuite.util.Matchers.body;
import static org.keycloak.testsuite.util.Matchers.statusCodeIs;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class FlowTest extends AbstractAuthenticationTest {

    // KEYCLOAK-3681: Delete top flow doesn't delete all subflows
    @Test
    public void testRemoveSubflows() {
        createFlow(newFlow("Foo", "Foo flow", "generic", true, false));
        addFlowToParent("Foo", "child");
        addFlowToParent("child", "grandchild");
        
        List<AuthenticationFlowRepresentation> flows = authMgmtResource.getFlows();
        AuthenticationFlowRepresentation found = findFlowByAlias("Foo", flows);
        authMgmtResource.deleteFlow(found.getId());
        assertAdminEvents.clear();

        createFlow(newFlow("Foo", "Foo flow", "generic", true, false));
        addFlowToParent("Foo", "child");
        
        // Under the old code, this would throw an error because "grandchild"
        // was left in the database
        addFlowToParent("child", "grandchild");
    }
    
    private void addFlowToParent(String parentAlias, String childAlias) {
        Map<String, String> data = new HashMap<>();
        data.put("alias", childAlias);
        data.put("type", "generic");
        data.put("description", childAlias + " flow");
        authMgmtResource.addExecutionFlow(parentAlias, data);
    }
    
    @Test
    public void testAddFlowWithRestrictedCharInAlias() {
        Response resp = authMgmtResource.createFlow(newFlow("fo]o", "Browser flow", "basic-flow", true, false));
        Assert.assertEquals(400, resp.getStatus());
    }
    
    @Test
    public void testAddRemoveFlow() {

        // test that built-in flow cannot be deleted
        List<AuthenticationFlowRepresentation> flows = authMgmtResource.getFlows();
        for (AuthenticationFlowRepresentation flow : flows) {
            try {
                authMgmtResource.deleteFlow(flow.getId());
                Assert.fail("deleteFlow should fail for built in flow");
            } catch (BadRequestException e) {
                break;
            }
        }

        // try create new flow using alias of already existing flow
        Response response = authMgmtResource.createFlow(newFlow("browser", "Browser flow", "basic-flow", true, false));
        try {
            Assert.assertEquals("createFlow using the alias of existing flow should fail", 409, response.getStatus());
        } finally {
            response.close();
        }

        // try create flow without alias
        response = authMgmtResource.createFlow(newFlow(null, "Browser flow", "basic-flow", true, false));
        try {
            Assert.assertEquals("createFlow using the alias of existing flow should fail", 409, response.getStatus());
        } finally {
            response.close();
        }


        // create new flow that should succeed
        AuthenticationFlowRepresentation newFlow = newFlow("browser-2", "Browser flow", "basic-flow", true, false);
        createFlow(newFlow);

        // check that new flow is returned in a children list
        flows = authMgmtResource.getFlows();
        AuthenticationFlowRepresentation found = findFlowByAlias("browser-2", flows);

        Assert.assertNotNull("created flow visible in parent", found);
        compareFlows(newFlow, found);

        // check lookup flow with unexistent ID
        try {
            authMgmtResource.getFlow("id-123-notExistent");
            Assert.fail("Not expected to find unexistent flow");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // check that new flow is returned individually
        AuthenticationFlowRepresentation found2 = authMgmtResource.getFlow(found.getId());
        Assert.assertNotNull("created flow visible directly", found2);
        compareFlows(newFlow, found2);


        // add execution flow to some parent flow
        Map<String, String> data = new HashMap<>();
        data.put("alias", "SomeFlow");
        data.put("type", "basic-flow");
        data.put("description", "Test flow");
        // This tests against a regression in KEYCLOAK-16656
        data.put("provider", "registration-page-form");

        Map<String, String> data2 = new HashMap<>();
        data2.put("alias", "SomeFlow2");
        data2.put("type", "form-flow");
        data2.put("description", "Test flow 2");
        data2.put("provider", "registration-page-form");


        // inexistent parent flow - should fail
        try {
            authMgmtResource.addExecutionFlow("inexistent-parent-flow-alias", data);
            Assert.fail("addExecutionFlow for inexistent parent should have failed");
        } catch (Exception expected) {
            // Expected
        }

        // already existent flow - should fail
        try {
            data.put("alias", "browser");
            authMgmtResource.addExecutionFlow("browser-2", data);
            Assert.fail("addExecutionFlow should have failed as browser flow already exists");
        } catch (Exception expected) {
            // Expected
        }

        // Successfully add flow
        data.put("alias", "SomeFlow");
        authMgmtResource.addExecutionFlow("browser-2", data);
        authMgmtResource.addExecutionFlow("browser-2", data2);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionFlowPath("browser-2"), data, ResourceType.AUTH_EXECUTION_FLOW);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionFlowPath("browser-2"), data2, ResourceType.AUTH_EXECUTION_FLOW);

        // check that new flow is returned in a children list
        flows = authMgmtResource.getFlows();
        found2 = findFlowByAlias("browser-2", flows);
        Assert.assertNotNull("created flow visible in parent", found2);

        List<AuthenticationExecutionExportRepresentation> execs = found2.getAuthenticationExecutions();
        Assert.assertNotNull(execs);
        Assert.assertEquals("Size two", 2, execs.size());

        AuthenticationExecutionExportRepresentation expected = new AuthenticationExecutionExportRepresentation();
        expected.setFlowAlias("SomeFlow");
        expected.setUserSetupAllowed(false);
        expected.setAuthenticatorFlow(true);
        expected.setRequirement("DISABLED");
        expected.setPriority(0);
        compareExecution(expected, execs.get(0));

        expected = new AuthenticationExecutionExportRepresentation();
        expected.setFlowAlias("SomeFlow2");
        expected.setUserSetupAllowed(false);
        expected.setAuthenticator("registration-page-form");
        expected.setAuthenticatorFlow(true);
        expected.setRequirement("DISABLED");
        expected.setPriority(1);
        compareExecution(expected, execs.get(1));

        // delete non-built-in flow
        authMgmtResource.deleteFlow(found.getId());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.authFlowPath(found.getId()), ResourceType.AUTH_FLOW);

        // check the deleted flow is no longer returned
        flows = authMgmtResource.getFlows();
        found = findFlowByAlias("browser-2", flows);
        Assert.assertNull("flow deleted", found);

        // Check deleting flow second time will fail
        try {
            authMgmtResource.deleteFlow("id-123-notExistent");
            Assert.fail("Not expected to delete flow, which doesn't exist");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }


    @Test
    public void testCopyFlow() {

        HashMap<String, String> params = new HashMap<>();
        params.put("newName", "clients");

        // copy using existing alias as new name
        Response response = authMgmtResource.copy("browser", params);
        try {
            Assert.assertThat("Copy flow using the new alias of existing flow should fail", response, statusCodeIs(Status.CONFLICT));
            Assert.assertThat("Copy flow using the new alias of existing flow should fail", response, body(containsString("already exists")));
            Assert.assertThat("Copy flow using the new alias of existing flow should fail", response, body(containsString("flow alias")));
        } finally {
            response.close();
        }

        // copy non-existing flow
        params.clear();
        response = authMgmtResource.copy("non-existent", params);
        try {
            Assert.assertThat("Copy non-existing flow", response, statusCodeIs(Status.NOT_FOUND));
        } finally {
            response.close();
        }

        // copy that should succeed
        params.put("newName", "Copy of browser");
        response = authMgmtResource.copy("browser", params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
        try {
            Assert.assertThat("Copy flow", response, statusCodeIs(Status.CREATED));
        } finally {
            response.close();
        }

        // compare original flow with a copy - fields should be the same except id, alias, and builtIn
        List<AuthenticationFlowRepresentation> flows = authMgmtResource.getFlows();
        AuthenticationFlowRepresentation browser = findFlowByAlias("browser", flows);
        AuthenticationFlowRepresentation copyOfBrowser = findFlowByAlias("Copy of browser", flows);

        Assert.assertNotNull(browser);
        Assert.assertNotNull(copyOfBrowser);

        // adjust expected values before comparing
        browser.setAlias("Copy of browser");
        browser.setBuiltIn(false);
        browser.getAuthenticationExecutions().get(3).setFlowAlias("Copy of browser forms");
        compareFlows(browser, copyOfBrowser);

        // get new flow directly and compare
        copyOfBrowser = authMgmtResource.getFlow(copyOfBrowser.getId());
        Assert.assertNotNull(copyOfBrowser);
        compareFlows(browser, copyOfBrowser);
        authMgmtResource.deleteFlow(copyOfBrowser.getId());
    }

    @Test
    // KEYCLOAK-2580
    public void addExecutionFlow() {
        HashMap<String, String> params = new HashMap<>();
        params.put("newName", "parent");
        Response response = authMgmtResource.copy("browser", params);
        Assert.assertEquals(201, response.getStatus());
        response.close();
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);

        params = new HashMap<>();
        params.put("alias", "child");
        params.put("description", "Description");
        params.put("provider", "registration-page-form");
        params.put("type", "basic-flow");

        authMgmtResource.addExecutionFlow("parent", params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionFlowPath("parent"), params, ResourceType.AUTH_EXECUTION_FLOW);
    }

    @Test
    //KEYCLOAK-12741
    //test editing of authentication flows
    public void editFlowTest() {
        List<AuthenticationFlowRepresentation> flows;

        //copy an existing one first
        HashMap<String, String> params = new HashMap<>();
        params.put("newName", "Copy of browser");
        Response response = authMgmtResource.copy("browser", params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
        try {
            Assert.assertEquals("Copy flow", 201, response.getStatus());
        } finally {
            response.close();
        }

        //load the newly copied flow
        flows = authMgmtResource.getFlows();
        AuthenticationFlowRepresentation testFlow = findFlowByAlias("Copy of browser", flows);
        //Set a new unique name. Should succeed
        testFlow.setAlias("Copy of browser2");
        authMgmtResource.updateFlow(testFlow.getId(), testFlow);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authEditFlowPath(testFlow.getId()), ResourceType.AUTH_FLOW);
        flows = authMgmtResource.getFlows();
        Assert.assertEquals("Copy of browser2", findFlowByAlias("Copy of browser2", flows).getAlias());

        //Create new flow and edit the old one to have the new ones name
        AuthenticationFlowRepresentation newFlow = newFlow("New Flow", "Test description", "basic-flow", true, false);
        createFlow(newFlow);
        // check that new flow is returned in a children list
        flows = authMgmtResource.getFlows();
        AuthenticationFlowRepresentation found = findFlowByAlias("New Flow", flows);

        Assert.assertNotNull("created flow visible in parent", found);
        compareFlows(newFlow, found);

        //try to update old flow with alias that already exists
        testFlow.setAlias("New Flow");
        try {
            authMgmtResource.updateFlow(found.getId(), testFlow);
        } catch (ClientErrorException exception){
            //expoected
        }
        flows = authMgmtResource.getFlows();

        //name should be the same for the old Flow
        Assert.assertEquals("Copy of browser2", findFlowByAlias("Copy of browser2", flows).getAlias());

        //Only update the description
        found.setDescription("New description");
        authMgmtResource.updateFlow(found.getId(), found);
        flows = authMgmtResource.getFlows();

        Assert.assertEquals("New description", findFlowByAlias("New Flow", flows).getDescription());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authEditFlowPath(found.getId()), ResourceType.AUTH_FLOW);

        //Update name and description
        found.setAlias("New Flow2");
        found.setDescription("New description2");
        authMgmtResource.updateFlow(found.getId(), found);
        flows = authMgmtResource.getFlows();

        Assert.assertEquals("New Flow2", findFlowByAlias("New Flow2", flows).getAlias());
        Assert.assertEquals("New description2", findFlowByAlias("New Flow2", flows).getDescription());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authEditFlowPath(found.getId()), ResourceType.AUTH_FLOW);
        Assert.assertNull(findFlowByAlias("New Flow", flows));

        authMgmtResource.deleteFlow(testFlow.getId());
        authMgmtResource.deleteFlow(found.getId());
    }

    @Test
    public void editExecutionFlowTest() {
        HashMap<String, String> params = new HashMap<>();
        List<AuthenticationExecutionInfoRepresentation> executionReps;
        //create new parent flow
        AuthenticationFlowRepresentation newFlow = newFlow("Parent-Flow", "This is a parent flow", "basic-flow", true, false);
        createFlow(newFlow);

        //create a child sub flow
        params.put("alias", "Child-Flow");
        params.put("description", "This is a child flow");
        params.put("provider", "registration-page-form");
        params.put("type", "basic-flow");

        authMgmtResource.addExecutionFlow("Parent-Flow", params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionFlowPath("Parent-Flow"), params, ResourceType.AUTH_EXECUTION_FLOW);

        executionReps = authMgmtResource.getExecutions("Parent-Flow");

        //create another with the same name of the previous one. Should fail to create
        params = new HashMap<>();
        params.put("alias", "Child-Flow");
        params.put("description", "This is another child flow");
        params.put("provider", "registration-page-form");
        params.put("type", "basic-flow");

        try {
            authMgmtResource.addExecutionFlow("Parent-Flow", params);
            Assert.fail("addExecutionFlow the alias already exist");
        } catch (Exception expected) {
            // Expected
        }

        AuthenticationExecutionInfoRepresentation found = executionReps.get(0);
        found.setDisplayName("Parent-Flow");

        try {
            authMgmtResource.updateExecutions("Parent-Flow", found);
        } catch (ClientErrorException exception){
            //expected
        }

        //edit both name and description
        found.setDisplayName("Child-Flow2");
        found.setDescription("This is another child flow2");

        authMgmtResource.updateExecutions("Parent-Flow", found);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("Parent-Flow"), ResourceType.AUTH_EXECUTION);
        executionReps = authMgmtResource.getExecutions("Parent-Flow");
        Assert.assertEquals("Child-Flow2", executionReps.get(0).getDisplayName());
        Assert.assertEquals("This is another child flow2", executionReps.get(0).getDescription());

        //edit only description
        found.setDescription("This is another child flow3");
        authMgmtResource.updateExecutions("Parent-Flow", found);

        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("Parent-Flow"), ResourceType.AUTH_EXECUTION);
        executionReps = authMgmtResource.getExecutions("Parent-Flow");
        Assert.assertEquals("Child-Flow2", executionReps.get(0).getDisplayName());
        Assert.assertEquals("This is another child flow3", executionReps.get(0).getDescription());
    }

}
