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

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DeviceActivityPage extends AbstractLoggedInPage {
    @FindBy(id = "sign-out-all")
    private WebElement signOutAllBtn;

    @FindBy(xpath = "//div[@rowid='sessions']/div[contains(@class,'-m-3-')]")
    private List<WebElement> sessionsFirstCol; // this represents first column of each session (which contains the browser icon)

    @Override
    public String getPageId() {
        return "device-activity";
    }

    @Override
    public String getParentPageId() {
        return ACCOUNT_SECURITY_ID;
    }

    public boolean isSignOutAllDisplayed() {
        return isElementVisible(signOutAllBtn);
    }

    public void clickSignOutAll() {
        clickLink(signOutAllBtn);
    }

    public int getSessionsCount() {
        return sessionsFirstCol.size();
    }

    public Session getSessionByIndex(int index) {
        // get the session ID from browser icon (which we know is always present)
        String sessionId = sessionsFirstCol.get(index)
                .findElement(By.xpath("//*[contains(@id,'-icon-')]"))
                .getAttribute("id")
                .split("-")[1];

        return getSession(sessionId);
    }

    public Session getSession(String sessionId) {
        return new Session(sessionId);
    }

    // We cannot use standard Page Fragment as there's no root element. Even though the sessions are placed in rows,
    // there's no element that would encapsulate it. Hence we cannot simply use e.g. @FindBy annotations.
    public class Session {
        private static final String SESSION = "session";
        private static final String BROWSER = "browser";
        private static final String IP = "ip";
        private static final String SIGN_OUT = "sign-out";

        private final String sessionId;
        private final String fullSessionId;

        // we don't want Session to be instantiated outside DeviceActivityPage
        private Session(String sessionId) {
            this.fullSessionId = sessionId;
            this.sessionId = sessionId.substring(0,7);
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getFullSessionId() {
            return fullSessionId;
        }

        public boolean isPresent() {
            return isItemDisplayed(IP); // no root element hence this workaround
        }

        public String getBrowserIconName() {
            String id = driver
                    .findElement(By.xpath(String.format("//*[contains(@id,'%s')]", getFullItemId("icon"))))
                    .getAttribute("id");

            return id.split("-")[3]; // the id looks like session-71891504-icon-chrome
        }

        public String getIp() {
            return getTextFromItem(IP);
        }

        public boolean hasCurrentBadge() {
            return isItemDisplayed("current-badge");
        }

        public boolean isBrowserDisplayed() {
            return isItemDisplayed(BROWSER);
        }

        public String getBrowser() {
            return getTextFromItem(BROWSER);
        }

        public String getLastAccess() {
            return getTextFromItem("last-access").split("Last accessed on ")[1];
        }

        public String getClients() {
            return getTextFromItem("clients").split("Clients ")[1];
        }

        public String getStarted() {
            return getTextFromItem("started").split("Started at ")[1];
        }

        public String getExpires() {
            return getTextFromItem("expires").split("Expires at ")[1];
        }

        public boolean isSignOutDisplayed() {
            return isItemDisplayed(SIGN_OUT);
        }

        public void clickSignOut() {
            clickLink(getItemElement(SIGN_OUT));
        }

        private String getFullItemId(String itemId) {
            return String.format("%s-%s-%s", SESSION, sessionId, itemId);
        }

        private WebElement getItemElement(String itemId) {
            return driver.findElement(By.id(getFullItemId(itemId)));
        }

        private String getTextFromItem(String itemId) {
            return getTextFromElement(getItemElement(itemId));
        }

        private boolean isItemDisplayed(String itemId) {
            try {
                return getItemElement(itemId).isDisplayed();
            }
            catch (NoSuchElementException e) {
                return false;
            }
        }
    }
}
