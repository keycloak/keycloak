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
package org.keycloak.testsuite.pages;

import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.Constants;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountUpdateProfilePage extends AbstractAccountPage {

    public static String PATH = RealmsResource.accountUrl(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT)).build("test").toString();

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;


    @FindBy(id = "referrer")
    private WebElement backToApplicationLink;

    @FindBy(css = "button[type=\"submit\"][value=\"Save\"]")
    private WebElement submitButton;

    @FindBy(css = "button[type=\"submit\"][value=\"Cancel\"]")
    private WebElement cancelButton;

    @FindBy(className = "alert-success")
    private WebElement successMessage;

    @FindBy(className = "alert-error")
    private WebElement errorMessage;

    public void updateProfile(String firstName, String lastName, String email) {
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);
        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);
        emailInput.clear();
        emailInput.sendKeys(email);

        submitButton.click();
    }

    public void clickCancel() {
        cancelButton.click();
    }


    public String getFirstName() {
        return firstNameInput.getAttribute("value");
    }

    public String getLastName() {
        return lastNameInput.getAttribute("value");
    }

    public String getEmail() {
        return emailInput.getAttribute("value");
    }

    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getPageSource().contains("Edit Account");
    }

    public void open() {
        driver.navigate().to(PATH);
    }

    public void backToApplication() {
        backToApplicationLink.click();
    }

    public String getSuccess(){
        return successMessage.getText();
    }

    public String getError() {
        return errorMessage.getText();
    }

    public boolean isPasswordUpdateSupported() {
        return driver.getPageSource().contains(PATH + "/password");
    }
}
