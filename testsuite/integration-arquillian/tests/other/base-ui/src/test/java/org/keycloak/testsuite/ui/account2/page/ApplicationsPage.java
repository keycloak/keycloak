/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2.page;

import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ApplicationsPage extends AbstractLoggedInPage {
    @FindBy(xpath = "//li[starts-with(@id,'application-client-id')]")
    private List<WebElement> applications;

    @Override
    public String getPageId() {
        return "applications";
    }

    public void toggleApplicationDetails(String clientId) {
        By selector = By.xpath("//button[@id='application-toggle-" + clientId + "']");
        waitUntilElement(selector).is().clickable();
        driver.findElement(selector).click();
    }

    public List<ClientRepresentation> getApplications() {
        ArrayList<ClientRepresentation> apps = new ArrayList<>();
        for(WebElement app : applications) {
            String clientId = app.getAttribute("id").replace("application-client-id-", "");
            apps.add(toRepresentation(app, clientId));
        }
        return apps;
    }

    private ClientRepresentation toRepresentation(WebElement app, String clientId) {
        String clientName = UIUtils.getTextFromElement(app.findElement(By.xpath("//div[@id='application-name-" + clientId + "']")));
        boolean userConsentRequired = !UIUtils.getTextFromElement(app.findElement(By.xpath("//div[@id='application-internal-" + clientId + "']"))).equals("Internal");
        boolean inUse = UIUtils.getTextFromElement(app.findElement(By.xpath("//div[@id='application-status-" + clientId + "']"))).equals("In use");
        String baseURL = UIUtils.getTextFromElement(app.findElement(By.xpath("//div[@id='application-baseurl-" + clientId + "']")));
        boolean applicationDetailsVisible = app.findElement(By.xpath("//section[@id='application-expandable-" + clientId + "']")).isDisplayed();
        return new ClientRepresentation(clientId, clientName, userConsentRequired, inUse, baseURL, applicationDetailsVisible);
    }

    public class ClientRepresentation {
        private final String clientId;
        private final String clientName;
        private final boolean userConsentRequired;
        private final boolean inUse;
        private final String baseUrl;
        private final boolean applicationDetailsVisible;

        public ClientRepresentation(String clientId, String clientName, boolean userConsentRequired, boolean inUse, String baseUrl, boolean applicationDetailsVisible) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.userConsentRequired = userConsentRequired;
            this.inUse = inUse;
            this.baseUrl = baseUrl;
            this.applicationDetailsVisible = applicationDetailsVisible;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public boolean isUserConsentRequired() {
            return userConsentRequired;
        }

        public boolean isInUse() {
            return inUse;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public boolean isApplicationDetailsVisible() {
            return applicationDetailsVisible;
        }
    }
}
