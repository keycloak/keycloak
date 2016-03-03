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

package org.keycloak.testsuite.console.page.events;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Events extends AdminConsoleRealm {

    @Override
    public String getUriFragment() {
        return super.getUriFragment();
    }
    
    @FindBy(linkText = "Login Events")
    private WebElement loginEventsTab;
    @FindBy(linkText = "Admin Events")
    private WebElement adminEventsTab;
    @FindBy(linkText = "Config")
    private WebElement configTab;
    
    public void loginEvents() {
        loginEventsTab.click();
    }
    public void adminEvents() {
        adminEventsTab.click();
    }
    public void config() {
        configTab.click();
    }
    
}
