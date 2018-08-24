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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DeviceActivityPage extends AbstractLoggedInPage {
    @Page
    private SignedInDevicesCard signedInDevicesCard;
    @Page
    private RecentlyUsedDevicesCard recentlyUsedDevicesCard;

    @Override
    protected List<String> createHashPath() {
        List<String> hashPath = super.createHashPath();
        hashPath.add("device-activity");
        return hashPath;
    }

    @Override
    public void navigateToUsingNavBar() {
        // TODO
    }

    @Override
    public boolean isCurrent() {
        return super.isCurrent() && signedInDevices().isVisible() && recentlyUsedDevices().isVisible();
    }

    public SignedInDevicesCard signedInDevices() {
        return signedInDevicesCard;
    }

    public RecentlyUsedDevicesCard recentlyUsedDevices() {
        return recentlyUsedDevicesCard;
    }

    public class SignedInDevicesCard extends Card {
        @FindBy(id = "signedInDevicesTitle")
        private WebElement signedInDevicesTitle;

        @Override
        public boolean isVisible() {
            return isElementVisible(signedInDevicesTitle);
        }
    }

    public class RecentlyUsedDevicesCard extends Card {
        @FindBy(id = "recentlyUsedDevicesTitle")
        private WebElement recentlyUsedDevicesTitle;

        @Override
        public boolean isVisible() {
            return isElementVisible(recentlyUsedDevicesTitle);
        }
    }
}
