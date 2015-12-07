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

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class FlowsTable {
    public enum RequirementOption {
        
        ALTERNATIVE("ALTERNATIVE"),
        DISABLED("DISABLED"),
        OPTIONAL("OPTIONAL"),
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
        ADD_EXECUTION("Add Execution"),
        ADD_FLOW("Add Flow");
        
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
        waitUntilElement(row).is().present();
        return row;
    }
    
    public void clickLevelUpButton(String rowLabel) {
        getRowByLabelText(rowLabel).findElement(By.xpath("//i[contains(@class, 'up')]/..")).click();
    }
    
    public void clickLevelDownButton(String rowLabel) {
        getRowByLabelText(rowLabel).findElement(By.xpath("//i[contains(@class, 'down')]/..")).click();
    }
    
    public void changeRequirement(String rowLabel, RequirementOption option) {
        getRowByLabelText(rowLabel).findElement(By.xpath("//input[@value = '" + option + "']")).click();
    }
    
    public void performAction(String rowLabel, Action action) {
        
    }
}
