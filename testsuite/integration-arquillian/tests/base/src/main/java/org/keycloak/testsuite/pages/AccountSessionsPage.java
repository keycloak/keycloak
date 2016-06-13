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

package org.keycloak.testsuite.pages;

import org.keycloak.services.Urls;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountSessionsPage extends AbstractAccountPage {

    private String realmName = "test";

    @FindBy(id = "logout-all-sessions")
    private WebElement logoutAllLink;


    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getCurrentUrl().endsWith("/account/sessions");
    }

    public void realm(String realmName) {
        this.realmName = realmName;
    }

    public String getPath() {
        return Urls.accountSessionsPage(getAuthServerRoot(), realmName).toString();
    }

    public void open() {
        driver.navigate().to(getPath());
    }

    public void logoutAll() {
        logoutAllLink.click();
    }

    public List<List<String>> getSessions() {
        List<List<String>> table = new LinkedList<List<String>>();
        for (WebElement r : driver.findElements(By.tagName("tr"))) {
            List<String> row = new LinkedList<String>();
            for (WebElement col : r.findElements(By.tagName("td"))) {
                row.add(col.getText());
            }
            table.add(row);
        }
        table.remove(0);
        return table;
    }

}
