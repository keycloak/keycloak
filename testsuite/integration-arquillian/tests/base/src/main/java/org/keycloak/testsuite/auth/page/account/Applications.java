/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.auth.page.account;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;
import java.util.List;

/**
 * @author Petr Mensik
 * @author mhajas
 */
public class Applications extends AccountManagement {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("applications");
    }

    public static final String XPATH_APP_TABLE = "//table[./thead[//td[text()='Application']]]";

    @FindBy(xpath = XPATH_APP_TABLE)
    protected WebElement appTable;

    @FindBy(xpath = XPATH_APP_TABLE + "//tr")
    private List<WebElement> applicationRows;

    public boolean containsApplication(String application) {
        return getRowForLinkText(application) != null;
    }

    public void clickApplication(String application) {
        WebElement row = getRowForLinkText(application);
        if (row == null) {
            log.error("Application: " + application + " doesn't exist");
            throw new IllegalArgumentException("Application: " + application + " doesn't exist");
        }

        row.findElement(By.xpath(".//a")).click();
    }

    public void revokeGrantForApplication(String application) {
        WebElement row = getRowForLinkText(application);
        if (row == null) {
            log.error("Application: " + application + " doesn't exist");
            throw new IllegalArgumentException("Application: " + application + " doesn't exist");
        }

        row.findElement(By.xpath("//button[@id='revoke-" + application + "']")).click();
    }

    private WebElement getRowForLinkText(String appLink) {
        for (WebElement appRow : applicationRows) {
            if (appRow.findElement(By.xpath(".//td")).getText().equals(appLink)) {
                return appRow;
            }
        }

        return null;
    }


}
