/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.auth.page.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.auth.page.account2.fragment.Card;
import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class WelcomeScreen extends AbstractAccountPage {
    @Page
    private PersonalInfoCard personalInfo;
    @Page
    private AccountSecurityCard accountSecurityCard;
    @Page
    private ApplicationsCard applicationsCard;
    @Page
    private MyResourcesCard myResourcesCard;
    @FindBy(id = "welcomeMsg")
    private WebElement welcomeMsg;
    @FindBy(id = "signInButton")
    private WebElement signInBtn;

    @Override
    protected List<String> createHashPath() {
        return null;
    }

    @Override
    public boolean isCurrent() {
        return URLUtils.currentUrlEquals(toString() + "#/") && isElementVisible(welcomeMsg); // the hash will be eventually added after the page is loaded
    }

    public PersonalInfoCard personalInfo() {
        return personalInfo;
    }

    public AccountSecurityCard accountSecurityCard() {
        return accountSecurityCard;
    }

    public ApplicationsCard applicationsCard() {
        return applicationsCard;
    }

    public MyResourcesCard myResourcesCard() {
        return myResourcesCard;
    }

    public void clickLoginBtn() {
        clickLink(signInBtn);
    }

    public boolean isLoginBtnVisible() {
        return isElementVisible(signInBtn);
    }

    public class PersonalInfoCard extends Card {
        @FindBy(id = "personalInfoCard")
        private WebElement personalInfoCard;
        @FindBy(id = "personalInfoLink")
        private WebElement personalInfoLink;

        @Override
        public boolean isVisible() {
            return isElementVisible(personalInfoCard);
        }

        public void clickPersonalInfo() {
            clickLink(personalInfoLink);
        }
    }

    public class AccountSecurityCard extends Card {
        @FindBy(id = "accountSecurityCard")
        private WebElement accountSecurityCard;
        @FindBy(id = "changePasswordLink")
        private WebElement changePasswordLink;
        @FindBy(id = "authenticatorLink")
        private WebElement authenticatorLink;
        @FindBy(id = "deviceActivityLink")
        private WebElement deviceActivityLink;
        @FindBy(id = "linkedAccountsLink")
        private WebElement linkedAccountsLink;

        @Override
        public boolean isVisible() {
            return isElementVisible(accountSecurityCard);
        }

        public void clickChangePassword() {
            clickLink(changePasswordLink);
        }

        public void clickAuthenticator() {
            clickLink(authenticatorLink);
        }

        public void clickDeviceActivity() {
            clickLink(deviceActivityLink);
        }

        public void clickLinkedAccounts() {
            clickLink(linkedAccountsLink);
        }

        public boolean isLinkedAccountsVisible() {
            return isElementVisible(linkedAccountsLink);
        }
    }

    public class ApplicationsCard extends Card {
        @FindBy(id = "applicationsCard")
        private WebElement applicationsCard;
        @FindBy(id = "applicationsLink")
        private WebElement applicationsLink;

        @Override
        public boolean isVisible() {
            return isElementVisible(applicationsCard);
        }

        public void clickApplicationsLink() {
            clickLink(applicationsLink);
        }
    }

    public class MyResourcesCard extends Card {
        @FindBy(id = "myResourcesCard")
        private WebElement myResourcesCard;
        @FindBy(id = "myResourcesLink")
        private WebElement myResourcesLink;

        @Override
        public boolean isVisible() {
            return isElementVisible(myResourcesCard);
        }

        public void clickMyResources() {
            clickLink(myResourcesLink);
        }
    }
}
