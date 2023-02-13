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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccountApplicationsPage extends AbstractAccountPage {

    @Override
    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getCurrentUrl().endsWith("/account/applications");
    }

    @Override
    public void open() {
        driver.navigate().to(getPath());
        waitForPageToLoad();
    }

    private String getPath() {
        return Urls.accountApplicationsPage(getAuthServerRoot(), "test").toString();
    }

    public void revokeGrant(String clientId) {
        clickLink(driver.findElement(By.id("revoke-" + clientId)));
    }

    public Map<String, AppEntry> getApplications() {
        Map<String, AppEntry> table = new HashMap<String, AppEntry>();
        for (WebElement r : driver.findElements(By.tagName("tr"))) {
            int count = 0;
            AppEntry currentEntry = null;

            for (WebElement col : r.findElements(By.tagName("td"))) {
                count++;
                switch (count) {
                    case 1:
                        currentEntry = new AppEntry();
                        String client = col.getText();
                        WebElement link = null;
                        try {
                            link = col.findElement(By.tagName("a"));
                            String href = link.getAttribute("href");
                            currentEntry.setHref(href);
                        } catch (Exception e) {
                            //ignore
                        }
                        table.put(client, currentEntry);
                        break;
                    case 2:
                        String rolesStr = col.getText();
                        String[] roles = rolesStr.split(",");
                        for (String role : roles) {
                            role = role.trim();
                            currentEntry.addAvailableRole(role);
                        }
                        break;
                    case 3:
                        String clientScopesStr = col.getText();
                        if (clientScopesStr.isEmpty()) break;
                        String[] clientScopes = clientScopesStr.split(",");
                        for (String clientScope : clientScopes) {
                            clientScope = clientScope.trim();
                            currentEntry.addGrantedClientScope(clientScope);
                        }
                        break;
                    case 4:
                        String additionalGrant = col.getText();
                        if (additionalGrant.isEmpty()) break;
                        String[] grants = additionalGrant.split(",");
                        for (String grant : grants) {
                            grant = grant.trim();
                            currentEntry.addAdditionalGrant(grant);
                        }
                        break;
                }
            }
        }
        table.remove("Application");
        return table;
    }

    public static class AppEntry {

        private final List<String> rolesAvailable = new ArrayList<String>();
        private final List<String> clientScopesGranted = new ArrayList<String>();
        private final List<String> additionalGrants = new ArrayList<>();
        private String href = null;

        private void addAvailableRole(String role) {
            rolesAvailable.add(role);
        }

        private void addGrantedClientScope(String clientScope) {
            clientScopesGranted.add(clientScope);
        }

        private void addAdditionalGrant(String grant) {
            additionalGrants.add(grant);
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public String getHref() {
            return this.href;
        }

        public List<String> getRolesAvailable() {
            return rolesAvailable;
        }

        public List<String> getClientScopesGranted() {
            return clientScopesGranted;
        }

        public List<String> getAdditionalGrants() {
            return additionalGrants;
        }
    }
}
