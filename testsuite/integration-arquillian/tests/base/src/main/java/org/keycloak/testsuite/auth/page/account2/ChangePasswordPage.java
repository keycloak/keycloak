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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickBtnAndWaitForAlert;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;
import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ChangePasswordPage extends AbstractLoggedInPage {
    @Page
    private UpdatePasswordCard updatePasswordCard;
    @Page
    private PasswordLastUpdateCard passwordLastUpdateCard;

    @Override
    protected List<String> createHashPath() {
        List<String> hashPath = super.createHashPath();
        hashPath.add("password");
        return hashPath;
    }

    @Override
    public void navigateToUsingNavBar() {
        // TODO
    }

    @Override
    public boolean isCurrent() {
        return super.isCurrent() && updatePassword().isVisible();
    }

    public PasswordLastUpdateCard passwordLastUpdate() {
        return passwordLastUpdateCard;
    }

    public UpdatePasswordCard updatePassword() {
        return updatePasswordCard;
    }

    public class PasswordLastUpdateCard extends Card {
        @FindBy(id = "passwordLastUpdate")
        private WebElement cardRoot;

        @Override
        public boolean isVisible() {
            return isElementVisible(cardRoot);
        }

        public String getTextDateTime() {
            return getTextFromElement(cardRoot.findElement(By.tagName("strong")));
        }

        public LocalDateTime getDateTime() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:m:s a"); // e.g. Aug 31, 2018, 5:41:24 PM
            return LocalDateTime.from(formatter.parse(getTextDateTime()));
        }
    }

    public class UpdatePasswordCard extends Card {
        @FindBy(id = "updatePasswordSubTitle")
        private WebElement updatePasswordSubTitle;
        @FindBy(id = "password")
        private WebElement currentPassword;
        @FindBy(id = "newPassword")
        private WebElement newPassword;
        @FindBy(id = "confirmation")
        private WebElement confirmPassword;
        @FindBy(name = "submitAction")
        private WebElement submitBtn;

        @Override
        public boolean isVisible() {
            return isElementVisible(updatePasswordSubTitle);
        }

        public void setCurrentPassword(String value) {
            setTextInputValue(currentPassword, value);
        }

        public void setNewPassword(String value) {
            setTextInputValue(newPassword, value);
        }

        public void setConfirmPassword(String value) {
            setTextInputValue(confirmPassword, value);
        }

        public boolean isSaveDisabled() {
            return submitBtn.getAttribute("disabled") != null;
        }

        public void clickSave() {
            clickBtnAndWaitForAlert(submitBtn);
        }

        public void setPasswords(String currentPassword, String newPassword) {
            setCurrentPassword(currentPassword);
            setNewPassword(newPassword);
            setConfirmPassword(newPassword);
        }
    }
}
