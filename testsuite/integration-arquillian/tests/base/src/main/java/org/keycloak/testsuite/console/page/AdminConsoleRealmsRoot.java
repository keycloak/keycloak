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

package org.keycloak.testsuite.console.page;

import org.keycloak.testsuite.console.page.fragment.RealmSelector;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;
import java.util.List;

/**
 *
 * @author tkyjovsk
 */
public class AdminConsoleRealmsRoot extends AdminConsole {

    @FindBy(xpath = "//tr[@data-ng-repeat='r in realms']//a[contains(@class,'ng-binding')]")
    private List<WebElement> realmLinks;

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("/");
    }

    @Override
    public String getUriFragment() {
        return "/realms";
    }

    public void clickRealm(String realm) {
        boolean linkFound = false;
        for (WebElement realmLink : realmLinks) {
            if (realmLink.getText().equals(realm)) {
                linkFound = true;
                realmLink.click();
            }
        }
        if (!linkFound) {
            throw new IllegalStateException("A link for realm '" + realm + "' not found on the Realms page.");
        }
    }

    @FindBy(css = "realm-selector")
    protected RealmSelector realmSelector;

//    public RealmsResource realmsResource() {
//        return keycloak.realms();
//    }

}
