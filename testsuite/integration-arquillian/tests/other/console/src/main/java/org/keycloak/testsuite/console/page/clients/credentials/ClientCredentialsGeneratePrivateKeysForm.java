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
package org.keycloak.testsuite.console.page.clients.credentials;

import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ClientCredentialsGeneratePrivateKeysForm extends Form {

    @FindBy(id = "downloadKeyFormat")
    private Select downloadKeyFormat;
    
    @FindBy(id = "keyAlias")
    private WebElement keyAliasInput;

    @FindBy(id = "keyPassword")
    private WebElement keyPasswordInput;
    
    @FindBy(id = "storePassword")
    private WebElement storePasswordInput;
    
    @FindBy(xpath = "//button[text()='Generate and Download']")
    private WebElement generateAndDownloadButton;
    
    public void selectFormatJKS() {
        downloadKeyFormat.selectByVisibleText("JKS");
    }

    public void selectFormatPKCS12() {
        downloadKeyFormat.selectByVisibleText("PKCS12");
    }
    
    public void setKeyAlias(String value) {
        UIUtils.setTextInputValue(keyAliasInput, value);
    }
    
    public void setKeyPassword(String value) {
        UIUtils.setTextInputValue(keyPasswordInput, value);
    }
    
    public void setStorePassword(String value) {
        UIUtils.setTextInputValue(storePasswordInput, value);
    }
    
    public void clickGenerateAndDownload() {
        waitUntilElement(generateAndDownloadButton).is().present();
        generateAndDownloadButton.click();
    }    
}
