/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.testsuite.util.UIUtils;

import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * <p>A page that contains the logout other sessions checkbox.</p>
 *
 * @author rmartinc
 */
public abstract class LogoutSessionsPage extends LanguageComboboxAwarePage {

    @FindBy(id = "logout-sessions")
    private WebElement logoutSessionsCheckbox;

    @Override
    public void assertCurrent() {
        super.assertCurrent();
        Assert.assertTrue("The page doesn't display the logout other sessions checkbox", this.isLogoutSessionDisplayed());
    }

    public boolean isLogoutSessionDisplayed() {
        return UIUtils.isElementVisible(logoutSessionsCheckbox);
    }

    public boolean isLogoutSessionsChecked() {
        return logoutSessionsCheckbox.isSelected();
    }

    public void checkLogoutSessions() {
        Assert.assertFalse("Logout sessions is checked", isLogoutSessionsChecked());
        UIUtils.switchCheckbox(logoutSessionsCheckbox, true);
    }

    public void uncheckLogoutSessions() {
        Assert.assertTrue("Logout sessions is not checked", isLogoutSessionsChecked());
        UIUtils.switchCheckbox(logoutSessionsCheckbox, false);
    }
}
