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
package org.keycloak.testsuite.console.page.clients.clustering;

import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ClientClusteringForm extends Form {

    @FindBy(id = "nodeReRegistrationTimeout")
    private WebElement nodeReRegistrationTimeoutInput;
    
    @FindBy(xpath = ".//select")
    private Select nodeReRegistrationTimeoutUnitSelect;
    
    @FindBy(linkText = "Register node manually")
    private WebElement registerNodeManuallyLink;
    
    @FindBy(id = "host")
    private WebElement hostNameInput;

    private void setNodeReRegistrationTimeout(String value) {
        UIUtils.setTextInputValue(nodeReRegistrationTimeoutInput, value);
    }
    
    private void setNodeReRegistrationTimeoutUnit(String value) {
        nodeReRegistrationTimeoutUnitSelect.selectByVisibleText(value);
    }
    
    public void setNodeReRegistrationTimeout(String timeout, String unit) {
        setNodeReRegistrationTimeoutUnit(unit);
        setNodeReRegistrationTimeout(timeout);
    }
    
    public void addNode(String hostName) {
        registerNodeManuallyLink.click();
//        waitforElement(hostNameInput);
        UIUtils.setTextInputValue(hostNameInput, hostName);
        save();
    }
}
