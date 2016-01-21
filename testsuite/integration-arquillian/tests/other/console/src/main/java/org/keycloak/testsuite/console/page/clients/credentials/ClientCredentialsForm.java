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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ClientCredentialsForm extends Form {

    @FindBy(id = "clientAuthenticatorType")
    private Select clientAuthenticatorType;
    
    @FindBy(xpath = "//button[text()='Regenerate Secret']")
    private WebElement regenerateSecretButton;

    @FindBy(xpath = "//button[text()='Generate new keys and certificate']")
    private WebElement generateNewKeysAndCert;
    
    @FindBy(xpath = "//button[text()='Regenerate registration access token']")
    private WebElement regenerateRegistrationAccessTokenButton;
    
    public void selectClientIdAndSecret() {
        clientAuthenticatorType.selectByVisibleText("Client Id and Secret");
    }
    
    public void selectSignedJwt() {
        clientAuthenticatorType.selectByVisibleText("Signed Jwt");
    }

    public void regenerateSecret() {
        regenerateSecretButton.click();
    }

    public void regenerateRegistrationAccessToken() {
        regenerateRegistrationAccessTokenButton.click();
    }

    public void generateNewKeysAndCert() {
        generateNewKeysAndCert.click();
    }
}
