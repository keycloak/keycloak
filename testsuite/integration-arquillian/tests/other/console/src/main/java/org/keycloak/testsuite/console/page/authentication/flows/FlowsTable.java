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
package org.keycloak.testsuite.console.page.authentication.flows;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import static org.openqa.selenium.By.xpath;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 * @author <a href="mailto:pzaoral@redhat.com">Peter Zaoral</a>
 */
public class FlowsTable {
    public enum RequirementOption {
        
        ALTERNATIVE("ALTERNATIVE"),
        DISABLED("DISABLED"),
        CONDITIONAL("CONDITIONAL"),
        REQUIRED("REQUIRED");
        
        private final String name;
        
        private RequirementOption(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public enum Action {
        
        DELETE("Delete"),
        ADD_EXECUTION("Add execution"),
        ADD_FLOW("Add flow");
        
        private final String name;
        
        private Action(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    @FindBy(tagName = "tbody")
    private WebElement tbody;

    private WebElement getRowByLabelText(String text) {
        WebElement row = tbody.findElement(By.xpath("//span[text() = '" + text + "']/../.."));
        //tbody.findElement(By.xpath("//span[contains(text(),\"" + text + "\")]/../.."));
        waitUntilElement(row).is().present();
        return row;
    }

    public void clickLevelUpButton(String rowLabel) {
        clickLink(getRowByLabelText(rowLabel).findElement(By.xpath(".//button[@data-ng-click='raisePriority(execution)']")));
    }

    public void clickLevelDownButton(String rowLabel) {
        clickLink(getRowByLabelText(rowLabel).findElement(xpath(".//button[@data-ng-click='lowerPriority(execution)']")));
    }

    public void changeRequirement(String rowLabel, RequirementOption option) {
        clickLink(getRowByLabelText(rowLabel).findElement(xpath(".//input[@value = '" + option + "']")));
    }

    public void performAction(String rowLabel, Action action) {

        clickLink(getRowByLabelText(rowLabel).findElement(
                xpath(".//div[@class = 'dropdown']/a[@class='dropdown-toggle ng-binding']")));
        WebElement currentAction = getRowByLabelText(rowLabel).findElement(
                xpath("//div[@class = 'dropdown open']/ul[@class = 'dropdown-menu']/li/" +
                        "a[@class='ng-binding' and text()='" + action.getName() + "']"));
        clickLink(currentAction);
    }

    // Returns all aliases of flows (first "Auth Type" column in table) including the names of execution flows
    // Each returned alias (key) has also the Requirement option (value) assigned in the Map
    public Map<String, String> getFlowsAliasesWithRequirements(){
        Map<String, String> flows = new LinkedHashMap<>();
        List<WebElement> aliases = tbody.findElements(By.xpath("//span[@class='ng-binding']"));

        for(WebElement alias : aliases)
        {
            List<WebElement> requirementsOptions = alias.findElements(By.xpath(".//../parent::*//input[@type='radio']"));
            for (WebElement requirement : requirementsOptions) {
                if (requirement.isSelected()) {
                    flows.put(getTextFromElement(alias), requirement.getAttribute("value"));
                }
            }
        }
        return flows;
    }
}
