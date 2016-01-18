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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ClientMapper extends ClientMappers {
    
    public static final String MAPPER_ID = "mapperId";
    
    @FindBy(xpath = "//i[contains(@class, 'delete')]")
    private WebElement deleteIcon;
    
    @FindBy(tagName = "form")
    private MapperSettingsForm form;
    
    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + MAPPER_ID + "}";
    }
    
    public void setMapperId(String id) {
        setUriParameter(MAPPER_ID, id);
    }

    public String getMapperId() {
        return getUriParameter(MAPPER_ID).toString();
    }
    
    @Override
    public void delete() {
        deleteIcon.click();
        modalDialog.confirmDeletion();
    }
    
    public MapperSettingsForm form() {
        return form;
    }
}
