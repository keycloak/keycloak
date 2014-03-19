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


import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AppPage extends AbstractPage {

    public static final String baseUrl = "http://localhost:8081/app";

    @FindBy(id = "account")
    private WebElement accountLink;

    @Override
    public void open() {
        driver.navigate().to(baseUrl);
    }

    @Override
    public boolean isCurrent() {
        return driver.getCurrentUrl().startsWith(baseUrl);
    }

    public RequestType getRequestType() {
        return RequestType.valueOf(driver.getTitle());
    }

    public void openAccount() {
        accountLink.click();
    }

    public enum RequestType {
        AUTH_RESPONSE, LOGOUT_REQUEST, APP_REQUEST
    }

}
