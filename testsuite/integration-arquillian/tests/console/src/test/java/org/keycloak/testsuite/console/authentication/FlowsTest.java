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
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.flows.CreateExecution;
import org.keycloak.testsuite.console.page.authentication.flows.CreateExecutionForm;
import org.keycloak.testsuite.console.page.authentication.flows.CreateFlow;
import org.keycloak.testsuite.console.page.authentication.flows.CreateFlowForm;
import org.keycloak.testsuite.console.page.authentication.flows.Flows;
import org.keycloak.testsuite.console.page.authentication.flows.FlowsTable;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
@Ignore //waiting for KEYCLOAK-1967(KEYCLOAK-1966)
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
        log.info("add new flow");
        flowsPage.clickNew();
        createFlowPage.form().setValues("testFlow", "testDesc", CreateFlowForm.FlowType.GENERIC);
        assertEquals("Success! Flow Created.", createFlowPage.getSuccessMessage());
        log.debug("new flow created via UI");
        
        log.info("check if test flow is created via rest");
        //rest: flow is present
        log.debug("checked");
        
        log.debug("check if testFlow is selected in UI");
        assertEquals("TestFlow", flowsPage.getFlowSelectValue());
        
        log.info("add new execution flow within testFlow");
        flowsPage.clickAddFlow();
        createFlowPage.form().setValues("testExecutionFlow", "executionDesc", CreateFlowForm.FlowType.GENERIC);
        assertEquals("Success! Flow Created.", createFlowPage.getSuccessMessage());
        log.debug("new execution flow created via UI");
        
        log.info("check if execution flow is created via rest");
        //rest: flow within nested flow is present
        log.debug("checked");
        
        log.debug("check if testFlow is selected in UI");
        assertEquals("TestFlow", flowsPage.getFlowSelectValue());
        
        log.info("delete test flow");
        flowsPage.clickDelete();
        assertEquals("Success! Flow removed", createFlowPage.getSuccessMessage());
        log.debug("test flow removed via UI");
        
        log.info("check if both test flow and execution flow is removed via rest");
        //rest
        log.debug("checked");
    }
    
    @Test
    public void createFlowWithEmptyAliasTest() {
        flowsPage.clickNew();
        createFlowPage.form().setValues("", "testDesc", CreateFlowForm.FlowType.GENERIC);
        assertEquals("Error! Missing or invalid field(s). Please verify the fields in red.", createFlowPage.getErrorMessage());
        
        //rest:flow isn't present
        
        //best-efford: check empty alias in nested flow
    }
    
    @Test
    public void copyFlowTest() {
        flowsPage.selectFlowOption(Flows.FlowOption.BROWSER);
        flowsPage.clickCopy();
        
        modalDialog.setName("test copy of browser");
        modalDialog.ok();
        assertEquals("Success! Flow copied.", createFlowPage.getSuccessMessage());
        
        //rest: copied flow present
    }
    
    @Test
    public void createDeleteExecutionTest() {
        //rest: add new flow
        
        log.info("add new execution within testFlow");
        flowsPage.clickAddExecution();
        createExecutionPage.form().selectProviderOption(CreateExecutionForm.ProviderOption.RESET_PASSWORD);
        createExecutionPage.form().save();
        
        assertEquals("Success! Execution Created.", createExecutionPage.getSuccessMessage());
        log.debug("new execution flow created via UI");
        
        //rest:check new execution
        
        log.debug("check if testFlow is selected in UI");
        assertEquals("TestFlow", flowsPage.getFlowSelectValue());
        
        log.info("delete test flow");
        flowsPage.clickDelete();
        assertEquals("Success! Flow removed", createFlowPage.getSuccessMessage());
        log.debug("test flow removed via UI");
        
        log.info("check if both test flow and execution flow is removed via rest");
        //rest
        log.debug("checked");
    }
    
    @Test
    public void navigationTest() {
        //rest: add or copy flow to test navigation (browser)
        
        //rest:
        log.debug("check if there is expected structure of the flow");
        //first should be Cookie
        //second Kerberos
        //third Test Copy Of Browser Forms
            //a) Username Password Form
            //b) OTP Form
        
        
        flowsPage.table().clickLevelDownButton("Cookie");
        assertEquals("Success! Priority lowered", flowsPage.getSuccessMessage());
        
        flowsPage.table().clickLevelUpButton("Test Copy Of Browser Forms");
        assertEquals("Success! Priority raised", flowsPage.getSuccessMessage());

        flowsPage.table().clickLevelUpButton("OTP Forms");
        assertEquals("Success! Priority raised", flowsPage.getSuccessMessage());
        
        //rest:check if navigation was changed properly
    }
    
    @Test
    public void requirementTest() {
        //rest: add or copy flow to test navigation (browser), add reset, password
        
        flowsPage.table().changeRequirement("Cookie", FlowsTable.RequirementOption.DISABLED);
        flowsPage.table().changeRequirement("Kerberos", FlowsTable.RequirementOption.ALTERNATIVE);
        flowsPage.table().changeRequirement("Copy Of Browser Forms", FlowsTable.RequirementOption.REQUIRED);
        flowsPage.table().changeRequirement("Reset Password", FlowsTable.RequirementOption.REQUIRED);
        
        //rest:check
    }
    
    @Test
    public void actionsTest() {
        //rest: add or copy flow to test navigation (browser)
        
        flowsPage.table().performAction("Kerberos", FlowsTable.Action.DELETE);
        flowsPage.table().performAction("Copy Of Browser Forms", FlowsTable.Action.ADD_FLOW);
        
        createFlowPage.form().setValues("nestedFlow", "", CreateFlowForm.FlowType.CLIENT);
        
        //todo: perform all remaining actions
        
        //rest: check
    }
}
