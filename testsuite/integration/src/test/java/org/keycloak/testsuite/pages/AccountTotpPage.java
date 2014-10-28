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

import org.keycloak.services.resources.AccountService;
import org.keycloak.testsuite.Constants;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountTotpPage extends AbstractAccountPage {

    private static String PATH = AccountService.totpUrl(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT)).build("test").toString();

    @FindBy(id = "totpSecret")
    private WebElement totpSecret;

    @FindBy(id = "totp")
    private WebElement totpInput;

    @FindBy(css = "button[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(id = "remove-mobile")
    private WebElement removeLink;

    public void configure(String totp) {
        totpInput.sendKeys(totp);
        submitButton.click();
    }

    public String getTotpSecret() {
        return totpSecret.getAttribute("value");
    }

    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getCurrentUrl().split("\\?")[0].endsWith("/account/totp");
    }

    public void open() {
        driver.navigate().to(PATH);
    }

    public void removeTotp() {
        removeLink.click();
    }

}
