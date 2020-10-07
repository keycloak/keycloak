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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.flows.CreateExecution;
import org.keycloak.testsuite.console.page.authentication.flows.CreateExecutionForm;
import org.keycloak.testsuite.console.page.authentication.flows.CreateFlow;
import org.keycloak.testsuite.console.page.authentication.flows.CreateFlowForm;
import org.keycloak.testsuite.console.page.authentication.flows.Flows;
import org.keycloak.testsuite.console.page.authentication.flows.FlowsTable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
        AuthenticationFlowRepresentation testFlow = getFlowFromREST("testFlow");
        assertEquals("testFlow", testFlow.getAlias());
        
        // Checking if testFlow is selected in UI
        assertEquals("TestFlow", flowsPage.getFlowSelectValue());
        
        // Adding new execution flow within testFlow
        flowsPage.clickAddFlow();
        createFlowPage.form().setValues("testExecution", "executionDesc", CreateFlowForm.FlowType.GENERIC);
        assertAlertSuccess();
        
        // Checking if execution flow is created via rest
        testFlow = getFlowFromREST("testFlow");
        assertEquals("testExecution", testFlow.getAuthenticationExecutions().get(0).getFlowAlias());
        
        // Checking if testFlow is selected in UI
        assertEquals("TestFlow", flowsPage.getFlowSelectValue());
        
        // Deleting test flow
        flowsPage.clickDelete();
        modalDialog.confirmDeletion();
        assertAlertSuccess();

        // Checking if both test flow and execution flow is removed via UI
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
        assertEquals(8, flowsPage.table().getFlowsAliasesWithRequirements().size());

        
        //rest: copied flow present
        assertThat(testRealmResource().flows().getFlows().stream()
                .map(AuthenticationFlowRepresentation::getAlias).
                        collect(Collectors.toList()), hasItem(getFlowFromREST("test copy of browser").getAlias()));
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
        AuthenticationFlowRepresentation flowRest = getFlowFromREST("testFlow");
        assertEquals(1, flowRest.getAuthenticationExecutions().size());
        assertEquals("reset-password", flowRest.getAuthenticationExecutions().get(0).getAuthenticator());

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

        List<String> expectedOrder = Arrays.asList(
                "Kerberos",
                "Cookie",
                "Copy Of Browser Forms",
                "Username Password Form",
                "Copy Of Browser Browser - Conditional OTP",
                "OTP Form",
                "Condition - User Configured",
                "Identity Provider Redirector"
        );
        
        //UI
        assertEquals(expectedOrder, flowsPage.table().getFlowsAliasesWithRequirements().keySet().stream().collect(Collectors.toList()));

        //REST
        assertEquals(expectedOrder.stream().map(displayName -> displayName.toLowerCase()).collect(Collectors.toList()), // case-insensitive comparison needed
                testRealmResource().flows().getExecutions("Copy of browser").stream().map(e -> e.getDisplayName().toLowerCase()).collect(Collectors.toList()));

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
        flowsPage.table().changeRequirement("Forms", FlowsTable.RequirementOption.CONDITIONAL);
        assertAlertSuccess();

        List<String> expectedOrder = Arrays.asList(
                "DISABLED", 
                "ALTERNATIVE", 
                "ALTERNATIVE",
                "CONDITIONAL", 
                "REQUIRED", 
                "CONDITIONAL",
                "REQUIRED", 
                "DISABLED"
        );

        //UI
        assertEquals(expectedOrder, flowsPage.table().getFlowsAliasesWithRequirements().values().stream().collect(Collectors.toList()));

        //REST:
        assertEquals(expectedOrder, testRealmResource().flows().getExecutions("browser").stream().map(e -> e.getRequirement()).collect(Collectors.toList()));
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

        List<String> expectedOrder = Arrays.asList(
                "Identity Provider Redirector",
                "Copy Of Browser Forms",
                "Username Password Form",
                "Copy Of Browser Browser - Conditional OTP",
                "Condition - User Configured",
                "OTP Form",
                "NestedFlow",
                "Reset Password"
        );

        //UI
        assertEquals(expectedOrder, flowsPage.table().getFlowsAliasesWithRequirements().keySet().stream().collect(Collectors.toList()));
        
        //REST
        assertEquals(expectedOrder.stream().map(displayName -> displayName.toLowerCase()).collect(Collectors.toList()), // case-insensitive comparison needed
                testRealmResource().flows().getExecutions("Copy of browser").stream().map(e -> e.getDisplayName().toLowerCase()).collect(Collectors.toList()));
        
    }

    private AuthenticationFlowRepresentation getFlowFromREST(String alias) {
        Optional<AuthenticationFlowRepresentation> flow = testRealmResource()
                .flows()
                .getFlows()
                .stream()
                .filter(f -> f.getAlias().equals(alias))
                .findFirst();
        return flow.isPresent() ? flow.get() : null;
    }
}
