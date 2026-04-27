/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn.pages;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SigningInPage extends AbstractLoggedInPage {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    private static final String CATEG_TITLE = "-categ-title";

    @Override
    public String getPageId() {
        return "signing-in";
    }

    @Override
    public String getParentPageId() {
        return ACCOUNT_SECURITY_ID;
    }

    @Override
    public String getTranslatedPageTitle() {
        return "Signing in";
    }

    public CredentialType getCredentialType(String type) {
        return new CredentialType(type);
    }

    public String getCategoryTitle(String categoryId) {
        return getTextFromElement(driver.findElement(By.id(categoryId + CATEG_TITLE)));
    }

    public int getCategoriesCount() {
        String xpath = String.format("//*[contains(@id,'%s')]", CATEG_TITLE);
        return driver.findElements(By.xpath(xpath)).size();
    }

    public class CredentialType {
        private static final String NOT_SET_UP = "not-set-up";
        private static final String SET_UP_TEST_ID = "create";
        private static final String TITLE = "title";
        private static final String HELP = "help-text";

        private final String type;

        private CredentialType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        private WebElement getItemElementByTestId(String item) {
            String xpath = String.format("//*[@data-testid = '%s/%s']", type, item);
            return driver.findElement(By.xpath(xpath));
        }

        public int getUserCredentialsCount() {
            String xpath = String.format("//*[@data-testid='%s/credential-list']//div[starts-with(@id,'cred-')]", type);
            return driver.findElements(By.xpath(xpath)).size();
        }

        public UserCredential getUserCredential(String id) {
            return new UserCredential(id, this);
        }

        public boolean isSetUp() {
            boolean notSetUpLabelPresent;

            try {
                notSetUpLabelPresent = getItemElementByTestId(NOT_SET_UP).isDisplayed();
            }
            catch (NoSuchElementException e) {
                notSetUpLabelPresent = false;
            }

            int userCredentialsCount = getUserCredentialsCount();

            if (notSetUpLabelPresent && userCredentialsCount == 0) {
                return false;
            }
            else if (!notSetUpLabelPresent && userCredentialsCount > 0) {
                return true;
            }
            else {
                throw new IllegalStateException("Unexpected \"not set up label\" state");
            }
        }

        public void clickSetUpLink() {
            clickLink(getItemElementByTestId(SET_UP_TEST_ID));
        }

        public boolean isSetUpLinkVisible() {
            try {
                return getItemElementByTestId(SET_UP_TEST_ID).isDisplayed();
            }
            catch (NoSuchElementException e) {
                return false;
            }
        }

        public boolean isNotSetUpLabelVisible() {
            try {
                return getItemElementByTestId(NOT_SET_UP).isDisplayed();
            }
            catch (NoSuchElementException e) {
                return false;
            }
        }

        public boolean isTitleVisible() {
            try {
                return getItemElementByTestId(TITLE).isDisplayed();
            }
            catch (NoSuchElementException e) {
                return false;
            }
        }

        public String getTitle() {
            return getTextFromElement(getItemElementByTestId(TITLE));
        }

        public String getHelpText() {
            return getTextFromElement(getItemElementByTestId(HELP));
        }

        public void navigateToUsingSidebar() {
            SigningInPage.this.navigateToUsingSidebar();
        }
    }

    public class UserCredential {
        private static final String LABEL = "label";
        private static final String CREATED_AT = "created-at";
        private static final String CREDENTIAL_CREATED_AT = "Created ";
        private static final String UPDATE = "update";
        private static final String REMOVE = "remove";

        private final String fullId;
        private final CredentialType credentialType;

        private UserCredential(String id, CredentialType credentialType) {
            this.fullId = id;
            this.credentialType = credentialType;
        }

        public String getId() {
            return fullId;
        }

        public CredentialType getCredentialType() {
            return credentialType;
        }

        private WebElement getItemElement(String item) {
            String elementId = String.format("//*[@data-testid='%s/credential-list']//div[@id='cred-%s']//*[@data-testrole='%s']", credentialType.getType(), fullId, item);
            return driver.findElement(By.xpath(elementId));
        }

        private boolean isItemDisplayed(String item) {
            try {
                return getItemElement(item).isDisplayed();
            }
            catch (NoSuchElementException e) {
                return false;
            }
        }

        private String getTextFromItem(String item) {
            return getTextFromElement(getItemElement(item));
        }

        public String getUserLabel() {
            return getTextFromElement(getItemElement(LABEL));
        }

        public boolean hasCreatedAt() {
            boolean result = false;
            try {
                result = getItemElement(CREATED_AT).isDisplayed();
            } catch (NoSuchElementException e) {}

            return result;
        }

        public String getCreatedAtStr() {
            String lastCreatedAtLabelXpath = String.format("//*[@data-testid='%s/credential-list']//div[@id='cred-%s']//*[@data-testrole='%s']/strong", credentialType.getType(), fullId, CREATED_AT);
            String lastCreatedAtLabel = getTextFromElement(driver.findElement(By.xpath(lastCreatedAtLabelXpath)));
            String lastCreateAtText = getTextFromItem(CREATED_AT);

            return lastCreateAtText
                    .substring(lastCreatedAtLabel.length(), lastCreateAtText.length() - 1)  // remove label, drop last dot
                    .trim();
        }

        public LocalDateTime getCreatedAt() {
            return LocalDateTime.parse(getCreatedAtStr(), DATE_TIME_FORMATTER);
        }

        public void clickUpdateBtn() {
            clickLink(getItemElement(UPDATE));
        }

        public void clickRemoveBtn() {
            clickLink(getItemElement(REMOVE));
        }

        public boolean isUpdateBtnDisplayed() {
            return isItemDisplayed(UPDATE);
        }

        public boolean isRemoveBtnDisplayed() {
            return isItemDisplayed(REMOVE);
        }

        public boolean isPresent() {
            try {
                return getItemElement(LABEL).isDisplayed();
            }
            catch (NoSuchElementException e) {
                return false;
            }
        }
    }
}
