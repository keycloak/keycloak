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
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.account2.fragment.Card;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickBtnAndWaitForAlert;
import static org.keycloak.testsuite.util.UIUtils.getTextInputValue;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;
import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class PersonalInfoPage extends AbstractLoggedInPage {
    @Page
    private PersonalInfoCard personalInfoCard;

    @Override
    protected List<String> createHashPath() {
        List<String> hashPath = super.createHashPath();
        hashPath.add("account");
        return hashPath;
    }

    @Override
    public void navigateToUsingNavBar() {
        // TODO
    }

    @Override
    public boolean isCurrent() {
        return super.isCurrent() && personalInfo().isVisible();
    }

    public PersonalInfoCard personalInfo() {
        return personalInfoCard;
    }

    public class PersonalInfoCard extends Card {
        @FindBy(id = "personalSubTitle")
        private WebElement personalSubTitle;
        @FindBy(id = "username")
        private WebElement username;
        @FindBy(id = "email")
        private WebElement email;
        @FindBy(id = "firstName")
        private WebElement firstName;
        @FindBy(id = "lastName")
        private WebElement lastName;
        @FindBy(name = "submitAction")
        private WebElement submitBtn;

        @Override
        public boolean isVisible() {
            return isElementVisible(personalSubTitle);
        }
        
        public boolean isUsernameDisabled() {
            return !username.getTagName().equals("input"); // <div> for disabled
        }
        
        public String getUsername() {
            return getTextInputValue(username);
        }
        
        public void setUsername(String value) {
            setTextInputValue(username, value);
        }
        
        public String getEmail() {
            return getTextInputValue(email);
        }
        
        public void setEmail(String value) {
            setTextInputValue(email, value);
        }

        public String getFirstName() {
            return getTextInputValue(firstName);
        }

        public void setFirstName(String value) {
            setTextInputValue(firstName, value);
        }

        public String getLastName() {
            return getTextInputValue(lastName);
        }

        public void setLastName(String value) {
            setTextInputValue(lastName, value);
        }

        public boolean isSaveDisabled() {
            return submitBtn.getAttribute("disabled") != null;
        }

        public void clickSave() {
            clickBtnAndWaitForAlert(submitBtn);
        }

        public void setValues(UserRepresentation user) {
            if (!isUsernameDisabled()) {setUsername(user.getUsername());}
            setEmail(user.getEmail());
            setFirstName(user.getFirstName());
            setLastName(user.getLastName());
        }

        public boolean valuesEqual(UserRepresentation user) {
            return user.getUsername().equals(getUsername())
                    && user.getEmail().equals(getEmail())
                    && user.getFirstName().equals(getFirstName())
                    && user.getLastName().equals(getLastName());
        }
    }
}
