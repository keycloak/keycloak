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
package org.keycloak.testsuite.console.page.clients.mappers;

import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class MapperSettingsForm extends CreateClientMappersForm {

    @FindBy(id = "protocol")
    private WebElement protocolInput;
    @FindBy(id = "mapperId")
    private WebElement mapperIdInput;
    @FindBy(id = "name")
    private WebElement nameInput;
    @FindBy(id = "mapperType")
    private WebElement mapperTypeInput;
    
    public String getProtocol() {
        return UIUtils.getTextInputValue(protocolInput);
    }
    
    public String getMapperId() {
        return UIUtils.getTextInputValue(mapperIdInput);
    }
    
    public String getName() {
        return UIUtils.getTextInputValue(nameInput);
    }
    
    public String getMapperType() {
        return UIUtils.getTextInputValue(mapperTypeInput);
    }
}
