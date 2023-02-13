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
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class Sessions extends AccountManagement {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("sessions");
    }

    @FindBy(id = "logout-all-sessions")
    private WebElement logoutAllLink;

    public void logoutAll() {
        clickLink(logoutAllLink);
    }

    public List<List<String>> getSessions() {
        List<List<String>> table = new LinkedList<>();
        for (WebElement r : driver.findElements(By.tagName("tr"))) {
            List<String> row = new LinkedList<>();
            for (WebElement col : r.findElements(By.tagName("td"))) {
                row.add(col.getText());
            }
            table.add(row);
        }
        table.remove(0);
        return table;
    }
}
