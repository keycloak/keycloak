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

package org.keycloak.testsuite.authentication;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class FlowTest extends AbstractAuthenticationTest {

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

        // create new flow that should succeed
        AuthenticationFlowRepresentation newFlow = newFlow("browser-2", "Browser flow", "basic-flow", true, false);
        response = authMgmtResource.createFlow(newFlow);
        try {
            Assert.assertEquals("createFlow success", 201, response.getStatus());
        } finally {
            response.close();
        }

        // check that new flow is returned
        flows = authMgmtResource.getFlows();
        AuthenticationFlowRepresentation found = findFlowByAlias("browser-2", flows);

        Assert.assertNotNull("created flow visible", found);
        compareFlows(newFlow, found);

        // delete non-built-in flow
        authMgmtResource.deleteFlow(found.getId());

        // check the deleted flow is no longer returned
        flows = authMgmtResource.getFlows();
        found = findFlowByAlias("browser-2", flows);
        Assert.assertNull("flow deleted", found);
    }


    @Test
    public void testCopyFlow() {

        HashMap<String, String> params = new HashMap<>();
        params.put("newName", "clients");

        // copy using existing alias as new name
        Response response = authMgmtResource.copy("browser", params);
        try {
            Assert.assertEquals("Copy flow using the new alias of existing flow should fail", 409, response.getStatus());
        } finally {
            response.close();
        }

        // copy non-existing flow
        params.clear();
        response = authMgmtResource.copy("non-existent", params);
        try {
            Assert.assertEquals("Copy non-existing flow", 404, response.getStatus());
        } finally {
            response.close();
        }

        // copy that should succeed
        params.put("newName", "Copy of browser");
        response = authMgmtResource.copy("browser", params);
        try {
            Assert.assertEquals("Copy flow", 201, response.getStatus());
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
        compareFlows(browser, copyOfBrowser);
    }

}
