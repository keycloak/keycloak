/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.console.authentication;

import org.apache.commons.collections.CollectionUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.flows.CreateExecution;
import org.keycloak.testsuite.console.page.authentication.flows.CreateExecutionForm;
import org.keycloak.testsuite.console.page.authentication.flows.CreateFlow;
import org.keycloak.testsuite.console.page.authentication.flows.CreateFlowForm;
import org.keycloak.testsuite.console.page.authentication.flows.Flows;
import org.keycloak.testsuite.console.page.authentication.flows.FlowsTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 * @author <a href="mailto:pzaoral@redhat.com">Peter Zaoral</a>
 */

public class FlowsTest extends AbstractConsoleTest {
    
    @Page
    private Flows flowsPage;

    @Page
    private CreateFlow createFlowPage;
    
    @Page
    private CreateExecution createExecutionPage;
    
    @Before
    public void beforeFlowsTest() {
        flowsPage.navigateTo();
    }
    
    @Test
    public void createDeleteFlowTest() {
        // Adding new flow
        flowsPage.clickNew();
        createFlowPage.form().setValues("testFlow", "testDesc", CreateFlowForm.FlowType.GENERIC);
        assertAlertSuccess();

        // Checking if test flow is created via rest
        AuthenticationFlowRepresentation testFlow = getLastFlowFromREST();
        assertEquals("testFlow", testFlow.getAlias());
        
        // Checking if testFlow is selected in UI
        assertEquals("TestFlow", flowsPage.getFlowSelectValue());
        
        // Adding new execution flow within testFlow
        flowsPage.clickAddFlow();
        createFlowPage.form().setValues("testExecution", "executionDesc", CreateFlowForm.FlowType.GENERIC);
        assertAlertSuccess();
        
        // Checking if execution flow is created via rest
        testFlow = getLastFlowFromREST();
        assertEquals("testExecution", testFlow.getAuthenticationExecutions().get(0).getFlowAlias());
        
        // Checking if testFlow is selected in UI
        assertEquals("TestFlow", flowsPage.getFlowSelectValue());
        
        // Deleting test flow
        flowsPage.clickDelete();
        modalDialog.confirmDeletion();
        assertAlertSuccess();

        // Checking if both test flow and execution flow is removed via UI
        assertEquals("Browser", flowsPage.getFlowSelectValue());
        assertThat(flowsPage.getFlowAllValues(), not(hasItem("TestFlow")));

        // Checking if both test flow and execution flow is removed via rest
        assertThat(testRealmResource().flows().getFlows(), not(hasItem(testFlow)));
    }

    @Test
    public void selectFlowOptionTest() {
        flowsPage.selectFlowOption(Flows.FlowOption.DIRECT_GRANT);
        assertEquals("Direct Grant", flowsPage.getFlowSelectValue());
        flowsPage.selectFlowOption(Flows.FlowOption.BROWSER);
        assertEquals("Browser", flowsPage.getFlowSelectValue());
        flowsPage.selectFlowOption(Flows.FlowOption.CLIENTS);
        assertEquals("Clients", flowsPage.getFlowSelectValue());
    }

    @Test
    public void createFlowWithEmptyAliasTest() {
        flowsPage.clickNew();
        createFlowPage.form().setValues("", "testDesc", CreateFlowForm.FlowType.GENERIC);
        assertAlertDanger();
        
        //rest:flow isn't present
    }

    @Test
    public void createNestedFlowWithEmptyAliasTest() {
        //best-effort: check empty alias in nested flow
        flowsPage.clickNew();
        createFlowPage.form().setValues("testFlow", "testDesc", CreateFlowForm.FlowType.GENERIC);
        flowsPage.clickAddFlow();
        createFlowPage.form().setValues("", "executionDesc", CreateFlowForm.FlowType.GENERIC);
        assertAlertDanger();
    }

    @Test
    public void copyFlowTest() {
        flowsPage.selectFlowOption(Flows.FlowOption.BROWSER);
        flowsPage.clickCopy();
        
        modalDialog.setName("test copy of browser");
        modalDialog.ok();
        assertAlertSuccess();

        //UI
        assertEquals("Test Copy Of Browser", flowsPage.getFlowSelectValue());
        assertTrue(flowsPage.table().getFlowsAliasesWithRequirements().containsKey("Test Copy Of Browser Forms"));
        assertEquals(6,flowsPage.table().getFlowsAliasesWithRequirements().size());

        
        //rest: copied flow present
        assertThat(testRealmResource().flows().getFlows().stream()
                .map(AuthenticationFlowRepresentation::getAlias).
                        collect(Collectors.toList()), hasItem(getLastFlowFromREST().getAlias()));
    }
    
    @Test
    public void createDeleteExecutionTest() {
        // Adding new execution within testFlow

        flowsPage.clickNew();
        createFlowPage.form().setValues("testFlow", "testDesc", CreateFlowForm.FlowType.GENERIC);

        flowsPage.clickAddExecution();
        createExecutionPage.form().selectProviderOption(CreateExecutionForm.ProviderOption.RESET_PASSWORD);
        createExecutionPage.form().save();
        assertAlertSuccess();
        
        // REST
        assertEquals(1, getLastFlowFromREST().getAuthenticationExecutions().size());
        assertEquals("reset-password", getLastFlowFromREST().getAuthenticationExecutions().get(0).getAuthenticator());

        // UI
        assertEquals("TestFlow", flowsPage.getFlowSelectValue());
        assertEquals(1,flowsPage.table().getFlowsAliasesWithRequirements().size());
        assertTrue(flowsPage.table().getFlowsAliasesWithRequirements().keySet().contains("Reset Password"));

        // Deletion
        flowsPage.clickDelete();
        modalDialog.confirmDeletion();
        assertAlertSuccess();
        assertThat(flowsPage.getFlowAllValues(), not(hasItem("TestFlow")));
    }
    
    @Test
    public void navigationTest() {
        flowsPage.selectFlowOption(Flows.FlowOption.BROWSER);
        flowsPage.clickCopy();
        modalDialog.ok();
        
        //init order
        //first should be Cookie
        //second Kerberos
        //third Identity provider redirector
        //fourth Test Copy Of Browser Forms
            //a) Username Password Form
            //b) OTP Form
        
        
        flowsPage.table().clickLevelDownButton("Cookie");
        assertAlertSuccess();
        
        flowsPage.table().clickLevelUpButton("Cookie");
        assertAlertSuccess();

        flowsPage.table().clickLevelUpButton("Kerberos");
        assertAlertSuccess();

        flowsPage.table().clickLevelDownButton("Identity Provider Redirector");
        assertAlertSuccess();

        flowsPage.table().clickLevelUpButton("OTP Form");
        assertAlertSuccess();

        List<String> expectedOrder = new ArrayList<>();
        Collections.addAll(expectedOrder, "Kerberos", "Cookie", "Copy Of Browser Forms", "OTP Form",
                                          "Username Password Form", "Identity Provider Redirector");

        //UI
        assertEquals(6,flowsPage.table().getFlowsAliasesWithRequirements().size());
        assertTrue(expectedOrder.containsAll(flowsPage.table().getFlowsAliasesWithRequirements().keySet()));

        //REST
        assertEquals("auth-spnego", getLastFlowFromREST().getAuthenticationExecutions().get(0).getAuthenticator());
        assertEquals("auth-cookie", getLastFlowFromREST().getAuthenticationExecutions().get(1).getAuthenticator());
        assertEquals("Copy of browser forms", getLastFlowFromREST().getAuthenticationExecutions().get(2).getFlowAlias());
        assertEquals("identity-provider-redirector", getLastFlowFromREST().getAuthenticationExecutions().get(3).getAuthenticator());
        flowsPage.clickDelete();
        modalDialog.confirmDeletion();
    }
    
    @Test
    public void requirementTest() {
        //rest: add or copy flow to test navigation (browser), add reset, password
        flowsPage.selectFlowOption(Flows.FlowOption.BROWSER);
        flowsPage.table().changeRequirement("Cookie", FlowsTable.RequirementOption.DISABLED);
        assertAlertSuccess();
        flowsPage.table().changeRequirement("Kerberos", FlowsTable.RequirementOption.REQUIRED);
        assertAlertSuccess();
        flowsPage.table().changeRequirement("Kerberos", FlowsTable.RequirementOption.ALTERNATIVE);
        assertAlertSuccess();
        flowsPage.table().changeRequirement("OTP Form", FlowsTable.RequirementOption.DISABLED);
        assertAlertSuccess();
        flowsPage.table().changeRequirement("OTP Form", FlowsTable.RequirementOption.OPTIONAL);
        assertAlertSuccess();

        //UI
        List<String> expectedOrder = new ArrayList<>();
        Collections.addAll(expectedOrder,"DISABLED", "ALTERNATIVE", "ALTERNATIVE",
                                         "ALTERNATIVE", "REQUIRED", "OPTIONAL");
        assertTrue(expectedOrder.containsAll(flowsPage.table().getFlowsAliasesWithRequirements().values()));
        
        //REST:
        List<AuthenticationExecutionExportRepresentation> browserFlow = testRealmResource().flows()
                                                                    .getFlows().get(0).getAuthenticationExecutions();
        assertEquals("DISABLED", browserFlow.get(0).getRequirement());
        assertEquals("ALTERNATIVE", browserFlow.get(1).getRequirement());
        assertEquals("ALTERNATIVE", browserFlow.get(2).getRequirement());
    }
    
    @Test
    public void actionsTest() {
        //rest: add or copy flow to test navigation (browser)
        flowsPage.selectFlowOption(Flows.FlowOption.BROWSER);
        flowsPage.clickCopy();
        modalDialog.ok();

        flowsPage.table().performAction("Cookie", FlowsTable.Action.DELETE);
        modalDialog.confirmDeletion();
        assertAlertSuccess();
        flowsPage.table().performAction("Kerberos", FlowsTable.Action.DELETE);
        modalDialog.confirmDeletion();
        assertAlertSuccess();
        flowsPage.table().performAction("Copy Of Browser Forms", FlowsTable.Action.ADD_FLOW);
        createFlowPage.form().setValues("nestedFlow", "testDesc", CreateFlowForm.FlowType.FORM);
        assertAlertSuccess();
        flowsPage.table().performAction("Copy Of Browser Forms",FlowsTable.Action.ADD_EXECUTION);
        createExecutionPage.form().selectProviderOption(CreateExecutionForm.ProviderOption.RESET_PASSWORD);
        createExecutionPage.form().save();
        assertAlertSuccess();

        //UI
        List<String> expectedOrder = new ArrayList<>();
        Collections.addAll(expectedOrder, "Identity Provider Redirector", "Copy Of Browser Forms",
                                          "Username Password Form", "OTP Form", "NestedFlow", "Reset Password");

        assertEquals(6,flowsPage.table().getFlowsAliasesWithRequirements().size());
        assertTrue(expectedOrder.containsAll(flowsPage.table().getFlowsAliasesWithRequirements().keySet()));

        //REST
        assertEquals("identity-provider-redirector", getLastFlowFromREST().getAuthenticationExecutions().get(0).getAuthenticator());
        String tmpFlowAlias = getLastFlowFromREST().getAuthenticationExecutions().get(1).getFlowAlias();
        assertEquals("Copy of browser forms", tmpFlowAlias);
        assertEquals("Username Password Form", testRealmResource().flows().getExecutions(tmpFlowAlias).get(0).getDisplayName());
        assertEquals("nestedFlow", testRealmResource().flows().getExecutions(tmpFlowAlias).get(2).getDisplayName());
    }

    private AuthenticationFlowRepresentation getLastFlowFromREST() {
        List<AuthenticationFlowRepresentation> allFlows = testRealmResource().flows().getFlows();
        return (AuthenticationFlowRepresentation) CollectionUtils.
                get(allFlows, (allFlows.size() - 1));
    }
}
