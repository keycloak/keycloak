/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.page.realm;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.openqa.selenium.WebElement;

/**
 * @author Filip Kiss
 * @author mhajas
 */
public class SecurityDefenses extends RealmSettings {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/defense"; // NOTE: page doesn't exist, only subpages
    }

    @FindByJQuery("a:contains('Brute Force Detection')")
    private WebElement bruteForceDetectionTab;

    @FindByJQuery("a:contains('Headers')")
    private WebElement headersTab;

    public void goToBruteForceDetection() {
        bruteForceDetectionTab.click();
    }

    public void goToHeaders() {
        headersTab.click();
    }
}
