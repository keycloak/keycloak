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
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccountFederatedIdentityPage extends AbstractAccountPage {

    @FindBy(className = "alert-error")
    private WebElement errorMessage;

    public AccountFederatedIdentityPage() {};

    private String realmName = "test";

    public void open() {
        driver.navigate().to(getPath());
    }

    public void realm(String realmName) {
        this.realmName = realmName;
    }

    public String getPath() {
        return Urls.accountFederatedIdentityPage(getAuthServerRoot(), realmName).toString();
    }

    @Override
    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getPageSource().contains("Federated Identities");
    }

    public List<FederatedIdentity> getIdentities() {
        List<FederatedIdentity> identities = new LinkedList<>();
        WebElement identitiesElement = driver.findElement(By.id("federated-identities"));
        for (WebElement i : identitiesElement.findElements(By.className("row"))) {

            String providerId = i.findElement(By.tagName("label")).getText();
            String subject = i.findElement(By.tagName("input")).getAttribute("value");
            WebElement button = i.findElement(By.tagName("button"));

            identities.add(new FederatedIdentity(providerId, subject, button));
        }
        return identities;
    }

    public WebElement findAddProvider(String providerId) {
        return driver.findElement(By.id("add-link-" + providerId));
    }

    public void clickAddProvider(String providerId) {
        findAddProvider(providerId).click();
    }

    public void clickRemoveProvider(String providerId) {
        driver.findElement(By.id("remove-link-" + providerId)).click();
    }

    public String getError() {
        return errorMessage.getText();
    }

    public boolean isLinked(String idpAlias) {
        return driver.getPageSource().contains("id=\"remove-link-" + idpAlias + "\"");
    }

    public static class FederatedIdentity {

        private String providerId;
        private String subject;
        private WebElement action;

        public FederatedIdentity(String providerId, String subject, WebElement action) {
            this.providerId = providerId;
            this.subject = subject;
            this.action = action;
        }

        public String getProvider() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public WebElement getAction() {
            return action;
        }

        public void setAction(WebElement action) {
            this.action = action;
        }
    }

}
