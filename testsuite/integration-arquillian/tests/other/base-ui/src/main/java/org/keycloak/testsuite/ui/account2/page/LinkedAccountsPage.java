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

import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.fragment.Root;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.xpath;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class LinkedAccountsPage extends AbstractLoggedInPage {
    public static final String LINKED_ACCOUNTS_ID = "linked-accounts";
    public static final String LINKED_IDPS_LIST_ID = "linked-idps";
    public static final String UNLINKED_IDPS_LIST_ID = "unlinked-idps";

    @FindBy(id = LINKED_IDPS_LIST_ID)
    private List<WebElement> linkedIdPsList;

    @FindBy(id = UNLINKED_IDPS_LIST_ID)
    private List<WebElement> unlinkedIdPsList;

    @Override
    public String getPageId() {
        return LINKED_ACCOUNTS_ID;
    }

    @Override
    public String getParentPageId() {
        return ACCOUNT_SECURITY_ID;
    }

    public IdentityProvider getProvider(String providerAlias) {
        WebElement root = driver.findElement(id(providerAlias + "-idp"));
        return Graphene.createPageFragment(IdentityProvider.class, root);
    }

    public int getLinkedProvidersCount() {
        return linkedIdPsList.size();
    }

    public int getUnlinkedProvidersCount() {
        return unlinkedIdPsList.size();
    }

    public class IdentityProvider {
        @Root
        private WebElement root;

        @FindBy(xpath = ".//*[contains(@id,'idp-name')]")
        private WebElement nameElement;

        @FindBy(xpath = ".//*[contains(@id,'idp-icon')]")
        private WebElement iconElement;

        @FindBy(xpath = ".//*[contains(@id,'idp-label')]")
        private WebElement badgeElement;

        @FindBy(xpath = ".//*[contains(@id,'idp-username')]")
        private WebElement usernameElement;

        @FindBy(xpath = ".//*[contains(@id,'idp-link')]")
        private WebElement linkBtn;

        @FindBy(xpath = ".//*[contains(@id,'idp-unlink')]")
        private WebElement unlinkBtn;

        public boolean isLinked() {
            String parentListId = root.findElement(xpath("ancestor::ul")).getAttribute("id");

            if (parentListId.equals(LINKED_IDPS_LIST_ID)) {
                return true;
            }
            else if (parentListId.equals(UNLINKED_IDPS_LIST_ID)) {
                return false;
            }
            else {
                throw new IllegalStateException("Unexpected parent list ID: " + parentListId);
            }
        }

        public boolean hasSocialLoginBadge() {
            return getTextFromElement(badgeElement).equals("Social login");
        }

        public boolean hasSystemDefinedBadge() {
            return getTextFromElement(badgeElement).equals("System defined");
        }

        public boolean hasSocialIcon() {
            return iconElement.getAttribute("id").contains("social");
        }

        public boolean hasDefaultIcon() {
            return iconElement.getAttribute("id").contains("default");
        }

        public String getUsername() {
            return getTextFromElement(usernameElement);
        }

        public boolean isLinkBtnVisible() {
            return isElementVisible(linkBtn);
        }

        public boolean isUnlinkBtnVisible() {
            return isElementVisible(unlinkBtn);
        }

        public void clickLinkBtn() {
            clickLink(linkBtn);
        }

        public void clickUnlinkBtn() {
            clickLink(unlinkBtn);
        }
    }
}
