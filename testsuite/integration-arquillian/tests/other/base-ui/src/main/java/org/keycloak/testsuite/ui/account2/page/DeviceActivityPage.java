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
import java.util.Optional;
import java.util.function.Predicate;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DeviceActivityPage extends AbstractLoggedInPage {
    @FindBy(id = "sign-out-all")
    private WebElement signOutAllBtn;

    @FindBy(className = "signed-in-device-grid")
    private List<WebElement> sessions;

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
        return sessions.size();
    }

    public Optional<Session> getSessionByIndex(int index) {
        try {
            return Optional.of(new Session(sessions.get(index)));
        } catch (Exception e) {
            log.warn(e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Session> getSession(String sessionId) {
        try {
            return Optional.of(new Session(getSessionElement(sessionId)));
        } catch (Exception e) {
            log.warn(e.getMessage());
            return Optional.empty();
        }
    }

    private WebElement getSessionElement(String sessionId) {
        return sessions.stream()
                .filter(f -> getTrimmedSessionId(sessionId).equals(getSessionId(f)))
                .findFirst()
                .orElse(null);
    }

    private static String getSessionId(WebElement sessionElement) {
        if (sessionElement == null) return null;
        return sessionElement.getAttribute("id").split("-")[1]; // the id looks like session-71891504-item
    }

    public static String getTrimmedSessionId(String fullSessionId) {
        return fullSessionId.substring(0, 7);
    }

    // We cannot use standard Page Fragment as there's no root element. Even though the sessions are placed in rows,
    // there's no element that would encapsulate it. Hence we cannot simply use e.g. @FindBy annotations.
    public class Session {
        private static final String SESSION = "session";
        private static final String DEVICE_ICON = "device-icon";
        private static final String IP = "ip";
        private static final String SIGN_OUT = "sign-out";

        private final WebElement element;
        private final String sessionId;

        // we don't want Session to be instantiated outside DeviceActivityPage
        private Session(WebElement element) {
            this.element = element;
            this.sessionId = DeviceActivityPage.getSessionId(element);
        }

        public String getSessionId() {
            return sessionId;
        }

        public boolean isPresent() {
            return isItemDisplayed(IP); // no root element hence this workaround
        }

        public String getIcon() {
            final WebElement icon = (WebElement) Optional.ofNullable(element.findElement(By.className(DEVICE_ICON)))
                    .map(f -> (WebElement) f)
                    .map(f -> f.findElement(By.tagName("svg")))
                    .orElse(null);

            if (icon == null) return "";
            return icon.getAttribute("id").split("-")[3]; // the id looks like session-71891504-icon-desktop
        }

        public String getIp() {
            return getTextFromItem(IP);
        }

        public boolean hasCurrentBadge() {
            return isItemDisplayed("current-badge");
        }

        public boolean isBrowserDisplayed() {
            return !"".equals(getBrowser());
        }

        public String getTitle() {
            return getTextFromElement(element.findElement(By.className("session-title")));
        }

        public String getBrowser() {
            try {
                return getTitle().split("/", 2)[1].trim();
            } catch (Exception e) {
                return "";
            }
        }

        public String getLastAccess() {
            return getTextFromItem("last-access");
        }

        public String getClients() {
            return getTextFromItem("clients");
        }

        public String getStarted() {
            return getTextFromItem("started");
        }

        public String getExpires() {
            return getTextFromItem("expires");
        }

        public boolean isSignOutDisplayed() {
            return getSignOutButton() != null;
        }

        public void clickSignOut() {
            WebElement signOutButton = getSignOutButton();
            if (signOutButton != null) {
                clickLink(signOutButton);
            } else {
                log.warn("Cannot click sign out button; not present");
            }
        }

        private WebElement getSignOutButton() {
            try {
                return driver.findElement(By.xpath(String.format("//button[@id='%s']", getFullItemId(SIGN_OUT))));
            } catch (NoSuchElementException e) {
                return null;
            }
        }

        private String getFullItemId(String itemId) {
            return String.format("%s-%s-%s", SESSION, sessionId, itemId);
        }

        private WebElement getItemElement(String itemId) {
            return element.findElement(By.id(getFullItemId(itemId)));
        }

        private String getTextFromItem(String itemId) {
            return getTextFromElement(getItemElement(itemId).findElement(By.tagName("div")));
        }

        private boolean isItemDisplayed(String itemId) {
            try {
                return getItemElement(itemId).isDisplayed();
            } catch (NoSuchElementException e) {
                return false;
            }
        }
    }
}
